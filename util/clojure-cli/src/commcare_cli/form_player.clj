(ns commcare-cli.form_player
  (:require [commcare-cli.helpers :as helpers]
            [commcare-cli.repl :as repl]
            [commcare-cli.answer_input :as answer]
            [clojure.stacktrace :as st]
            [clojure.string :as string])
  (:import [java.io IOException]
           [org.javarosa.core.model Constants]
           [org.javarosa.core.model.data SelectMultiData]
           [org.javarosa.core.model.condition EvaluationContext]
           [org.javarosa.core.model.trace StringEvaluationTraceSerializer]
           [org.javarosa.engine XFormEnvironment]
           [org.javarosa.form.api FormEntrySession FormEntryController
            FormEntrySessionReplayer FormEntrySessionReplayer$ReplayError]
           [org.javarosa.model.xform XFormSerializingVisitor]
           [org.javarosa.xpath XPathNodeset XPathParseTool]
           [org.javarosa.xpath.expr XPathFuncExpr]
           [org.javarosa.xpath.parser XPathSyntaxException]))

(def debug-mode? (atom false))

(defn print-choice [is-multi-select? is-selected? index choice-text]
  (println
    (when is-multi-select? (string/join "[" (if is-selected? "X" " ") "]"))
    (+ 1 index)
    ") "
    choice-text))

(defn show-choices [entry-prompt choices]
  (let [is-multi-select? (= Constants/CONTROL_SELECT_MULTI (.getControlType entry-prompt))
        indices (range (count choices))
        choices-text (map (fn [c] (.getSelectChoiceText entry-prompt c)) choices)
        selected-choices (answer/get-selected-choices entry-prompt choices)
        is-selected? (map (fn [c] (some #{c} selected-choices)) choices)]
    (doall
      (map
        (partial print-choice is-multi-select?)
        is-selected?
        indices
        choices-text))))

(defn show-question [entry-prompt]
  (println (.getQuestionText entry-prompt))
  (let [choices (.getSelectChoices entry-prompt)]
    (when (not (nil? choices))
      (show-choices entry-prompt choices))
    (when (= Constants/CONTROL_TRIGGER (.getControlType entry-prompt))
      (println "Press Return to proceed"))))

(defn new-repeat-question []
  (println "Add new repeat?")
  (println "1) Yes, add a new repeat group")
  (println "2) No, continue to the next question"))

;; FormEntryController [FormEntryController -> None] -> None
(defn show-event [entry-controller step-func]
  (helpers/clear-view)
  (let [event (.getEvent (.getModel entry-controller))]
    (cond
      (= event FormEntryController/EVENT_BEGINNING_OF_FORM) (println "Form Start: Press Return to proceed")
      (= event FormEntryController/EVENT_END_OF_FORM) (println "Form End: Press Return to Complete Entry")
      ;;mProcessOnExit = true;
      (= event FormEntryController/EVENT_GROUP) (do (step-func entry-controller)
                                                    (show-event entry-controller step-func))
      (= event FormEntryController/EVENT_QUESTION) (show-question (.getQuestionPrompt (.getModel entry-controller)))
      (= event FormEntryController/EVENT_REPEAT) (do (step-func entry-controller)
                                                     (show-event entry-controller step-func))
      (= event FormEntryController/EVENT_REPEAT_JUNCTURE) (println "Repeats Not Implemented, press return to exit")
      (= event FormEntryController/EVENT_PROMPT_NEW_REPEAT) (new-repeat-question))))

(defn display-relevant [entry-model]
  (let [debug-info
        (.getDebugInfo entry-model
                       (.getFormIndex entry-model)
                       "relevant"
                       (StringEvaluationTraceSerializer.))]
    (if (nil? debug-info)
      (println "No display logic defined")
      (println debug-info))))

;; FormEntryController String -> NavAction
(defn replay-entry-session [entry-controller command]
  (if (string/blank? command)
    (println "Invalid command, please provide session string to replay")
    (try
      (do (FormEntrySessionReplayer/tryReplayingFormEntry
            entry-controller
            (FormEntrySession/fromString command))
          :forward)
      (catch FormEntrySessionReplayer$ReplayError e
        (println "Error replaying form: " (.getMessage e))
        (println "Aborting form entry")
        :exit))))

(defn get-eval-ctx [entry-model in-debug-mode?]
  (let [pre-eval-ctx (.getEvaluationContext (.getForm entry-model))
        current-index (.getFormIndex entry-model)
        eval-ctx (if (.isInForm current-index)
                   (EvaluationContext. pre-eval-ctx (.getReference current-index))
                   pre-eval-ctx)]
      (when in-debug-mode? (.setDebugModeOn eval-ctx))
      eval-ctx))

(defn print-result [value in-debug-mode? eval-ctx]
  (println (if (instance? XPathNodeset value)
             (XPathFuncExpr/getSerializedNodeset (cast XPathNodeset value))
             (XPathFuncExpr/toString value)))
  (when (and in-debug-mode? (not (nil? (.getEvaluationTrace eval-ctx))))
    (println (.serializeEvaluationLevels
               (StringEvaluationTraceSerializer.)
               (.getEvaluationTrace eval-ctx)))))

(defn eval-expr [entry-controller in-debug-mode? raw-expr]
  (try
    (let [expr (XPathParseTool/parseXPath raw-expr)
          eval-ctx (get-eval-ctx (.getModel entry-controller) in-debug-mode?)]
      (print-result (.eval expr eval-ctx) in-debug-mode? eval-ctx))
    (catch XPathSyntaxException e
      (println "Parse error: " (.getMessage e)))
    (catch Exception e
      (st/print-stack-trace e)
      (println "Eval error: " (.getMessage e)))))

(defn eval-mode [entry-controller command]
  (let [input (string/trim command)]
    (if (string/blank? input)
      (repl/start-repl (partial eval-expr entry-controller @debug-mode?))
      (eval-expr entry-controller input @debug-mode?))))

;; FormEntryController String -> NavAction
;; where NavAction is one of [:forward :back :exit :finish]
(defn process-command [entry-controller command]
  (cond
    (= command "next") (do (.stepToNextEvent entry-controller) :forward)
    (= command "back") (do (.stepToPreviousEvent entry-controller) :back)
    (= command "quit") :exit
    (= command "cancel") :exit
    (= command "finish") :finish
    (= command "print") :forward
    (string/starts-with? command "eval") (do (eval-mode
                                               entry-controller
                                               (subs command 4))
                                             :forward)
    (string/starts-with? command "replay") (replay-entry-session
                                             (string/trim (subs command 6)))
    (= command "entry-session") (doall
                                  (println (.getFormEntrySessionString entry-controller))
                                  :forward)
    (= command "relevant") (doall
                             (display-relevant (.getModel entry-controller))
                             :forward)
    (= command "debug") (doall
                          (swap! debug-mode? not @debug-mode?)
                          (println "Expression debuggion: "
                                   (if @debug-mode? "ENABLED" "DISABLED"))
                          :forward)
    :else (doall (println "Invalid command: " command) :forward)))

;; FormEntryController -> (or Byte[] Nil)
(defn process-form [entry-controller]
  (let [form (.getForm (.getModel entry-controller))]
    (.postProcessInstance form)
    (helpers/clear-view)
    (try (let [instance (.serializeInstance (XFormSerializingVisitor.) (.getInstance form))]
           (helpers/ppxml (String. instance))
           instance)
         (catch IOException e 
           (doall (println "Error serializing XForm")
                  nil)))))

;; FormEntryController Boolean -> (or Byte[] Nil)
(defn process-loop [entry-controller forward?]
  (show-event entry-controller
              (fn [entry-controller] (if forward? 
                                       (.stepToNextEvent entry-controller)
                                       (.stepToPreviousEvent entry-controller))))
  (let [user-input (read-line)
        next-action (if (string/starts-with? user-input ":")
                      (process-command entry-controller (subs user-input 1))
                      (answer/answer-question entry-controller user-input))]
    (cond (or (= next-action :forward) (= next-action :back)) (recur entry-controller (= next-action :forward))
          (= next-action :finish) (process-form entry-controller)
          (= next-action :exit) (doall (println "Exit without saving")
                                       nil))))

;; FormDef CommCareSession String String -> (or Byte[] Nil)
(defn play [form-def session locale today-date]
  (let [env (XFormEnvironment. form-def)]
    (when (not (nil? locale))
      (.setLocale env locale))
    (when (not (nil? today-date))
      (println today-date)
      (.setToday env today-date))
    (let [entry-controller (.setup env (.getIIF session))]
      (process-loop entry-controller true))))

