package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathBooleanFromStringFunc extends XPathFuncExpr {
    public static final String NAME = "boolean-from-string";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathBooleanFromStringFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathBooleanFromStringFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        String s = FunctionUtils.toString(evaluatedArgs[0]);
        if (s.equalsIgnoreCase("true") || s.equals("1")) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior: Will convert a string value of \"1\" or \"true\" to true.  Otherwise will return false.\n"
                + "Return: Returns true or false based on the argument.\n"
                + "Arguments:  The value to be converted\n"
                + "Syntax: boolean-from-string(value_to_convert)\n"
                + "Example:  boolean(/data/my_question) or boolean(\"1\")";
    }
}
