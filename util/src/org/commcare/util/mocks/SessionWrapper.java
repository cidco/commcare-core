package org.commcare.util.mocks;

import org.commcare.core.interfaces.UserDataInterface;
import org.commcare.core.process.CommCareInstanceInitializer;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.CommCareSession;
import org.javarosa.core.model.condition.EvaluationContext;

/**
 * Extends a generic CommCare session to include context about the
 * current runtime environment
 *
 * @author ctsims
 */
public class SessionWrapper extends CommCareSession {
    
    UserDataInterface mSandbox;
    CommCarePlatform mPlatform;
    CommCareInstanceInitializer initializer;
    
    public SessionWrapper(CommCarePlatform platform, UserDataInterface sandbox) {
        super(platform);
        this.mSandbox = sandbox;
        this.mPlatform = platform;
    }

    /**
     * @return The evaluation context for the current state.
     */
    public EvaluationContext getEvaluationContext() {
        return getEvaluationContext(getIIF());
    }

    /**
     * @param commandId The id of the command to evaluate against
     * @return The evaluation context relevant for the provided command id
     */
    public EvaluationContext getEvaluationContext(String commandId) {
        return getEvaluationContext(getIIF(), commandId);
    }

    public CommCareInstanceInitializer getIIF() {
        if (initializer == null) {
            initializer = new CommCareInstanceInitializer(this, mSandbox, mPlatform);
        }

        return initializer;
    }
    public CommCarePlatform getPlatform(){
        return this.mPlatform;
    }
    public UserDataInterface getSandbox() {
        return this.mSandbox;
    }

    public void clearVolitiles() {
        initializer = null;
    }

    public void setComputedDatum() {
        setComputedDatum(getEvaluationContext());
    }
}
