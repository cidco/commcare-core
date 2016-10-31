package org.javarosa.xpath.parser.ast;

import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFilterExpr;
import org.javarosa.xpath.expr.XPathPathExpr;
import org.javarosa.xpath.expr.XPathStep;
import org.javarosa.xpath.parser.Token;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.ArrayList;
import java.util.List;

public class ASTNodeLocPath extends ASTNode {
    public final Vector<ASTNode> clauses;
    public List<Integer> separators;

    public ASTNodeLocPath() {
        clauses = new Vector<>();
        separators = new ArrayList<>();
    }

    @Override
    public Vector getChildren() {
        return clauses;
    }

    public boolean isAbsolute() {
        return clauses.size() == separators.size()
                || (clauses.size() == 0 && separators.size() == 1)
                || isHashRef();
    }

    private boolean isHashRef() {
        return !clauses.isEmpty()
                && clauses.firstElement() instanceof ASTNodePathStep
                && ((ASTNodePathStep)clauses.firstElement()).nodeTestType == ASTNodePathStep.NODE_TEST_TYPE_HASH_REF;
    }

    @Override
    public XPathExpression build() throws XPathSyntaxException {
        ArrayList<XPathStep> steps = new ArrayList<>();
        XPathExpression filtExpr = null;
        int offset = isAbsolute() ? 1 : 0;
        for (int i = 0; i < clauses.size() + offset; i++) {
            if (offset == 0 || i > 0) {
                ASTNode currentClause = clauses.elementAt(i - offset);
                if (currentClause instanceof ASTNodePathStep) {
                    steps.add(((ASTNodePathStep)currentClause).getStep());
                } else {
                    filtExpr = currentClause.build();
                }
            }

            if (i < separators.size()) {
                if (separators.get(i) == Token.DBL_SLASH) {
                    steps.add(XPathStep.ABBR_DESCENDANTS());
                }
            }
        }

        XPathStep[] stepArr = steps.toArray(new XPathStep[]{});
        if (filtExpr == null) {
            if (isAbsolute()) {
                if (isHashRef()) {
                    return XPathPathExpr.buildHashRefPath(stepArr);
                } else {
                    return XPathPathExpr.buildAbsolutePath(stepArr);
                }
            } else {
                return XPathPathExpr.buildRelativePath(stepArr);
            }
        } else {
            if (filtExpr instanceof XPathFilterExpr) {
                return XPathPathExpr.buildFilterPath((XPathFilterExpr)filtExpr, stepArr);
            } else {
                return XPathPathExpr.buildFilterPath(new XPathFilterExpr(filtExpr, new XPathExpression[0]), stepArr);
            }
        }
    }
}
