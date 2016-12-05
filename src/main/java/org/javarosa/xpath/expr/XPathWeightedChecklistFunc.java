package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathArityException;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathWeightedChecklistFunc extends XPathFuncExpr {
    public static final String NAME = "weighted-checklist";
    private static final int EXPECTED_ARG_COUNT = -1;

    public XPathWeightedChecklistFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathWeightedChecklistFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    protected void validateArgCount() throws XPathSyntaxException {
        if (!(args.length >= 2 && args.length % 2 == 0)) {
            throw new XPathArityException(name, "an even number of arguments", args.length);
        }
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        if (args.length == 4 && evaluatedArgs[2] instanceof XPathNodeset && evaluatedArgs[3] instanceof XPathNodeset) {
            Object[] factors = ((XPathNodeset)evaluatedArgs[2]).toArgList();
            Object[] weights = ((XPathNodeset)evaluatedArgs[3]).toArgList();
            if (factors.length != weights.length) {
                throw new XPathTypeMismatchException("weighted-checklist: nodesets not same length");
            }
            return checklistWeighted(evaluatedArgs[0], evaluatedArgs[1], factors, weights);
        } else {
            return checklistWeighted(evaluatedArgs[0], evaluatedArgs[1], FunctionUtils.subsetArgList(evaluatedArgs, 2, 2), FunctionUtils.subsetArgList(evaluatedArgs, 3, 2));
        }
    }

    /**
     * very similar to checklist, only each factor is assigned a real-number 'weight'.
     *
     * the first and second args are again the minimum and maximum, but -1 no longer means
     * 'not applicable'.
     *
     * subsequent arguments come in pairs: first the boolean value, then the floating-point
     * weight for that value
     *
     * the weights of all the 'true' factors are summed, and the function returns whether
     * this sum is between the min and max
     */
    private static Boolean checklistWeighted(Object oMin, Object oMax, Object[] flags, Object[] weights) {
        double min = FunctionUtils.toNumeric(oMin);
        double max = FunctionUtils.toNumeric(oMax);

        double sum = 0.;
        for (int i = 0; i < flags.length; i++) {
            boolean flag = FunctionUtils.toBoolean(flags[i]);
            double weight = FunctionUtils.toNumeric(weights[i]);

            if (flag)
                sum += weight;
        }

        return sum >= min && sum <= max;
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior:  Similar to a checklist but each item is assigned a weight.  Will return true if the total weight of the true items is between the range specified.\n"
                + "Return: True or false depending on the weighted-checklist (if value of the weighting is within the specified range).\n"
                + "Arguments:\n"
                + "\tThe first argument is a numeric value expressing the minimum value.  If -1, no minimum is applicable\n"
                + "\tThe second argument is a numeric value expressing the maximum value.  If -1, no maximum is applicable\n"
                + "\targuments 3 through the end come in pairs.  The first is the value to be checked and the second is the weight of that value.\n"
                + "Syntax: weighted-checklist(min_num, max_num, checklist_item_1, checklist_item_weight_1, checklist_item_2, checklist_item_weight_2, ...)\n"
                + "Example:  weighted-checklist(-1, 2, /data/high_risk_condition_1 = \"yes\", 0.5, /data/high_risk_condition_2 = \"yes\", 2.5, /data/high_risk_condition_3 = \"yes\", 0.75)";
    }
}