package org.javarosa.xpath.test;

import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.LivePrototypeFactory;
import org.javarosa.core.util.test.ExternalizableTest;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class XPathParseTest {

    @Parameterized.Parameters(name = "{index}: test({0}) expected={1}")
    public static Iterable<String[]> testData() {
        return Arrays.asList(new String[][]{
                // no null expressions
                {"", null},
                {"     ", null},
                {"  \t \n  \r ", null},
                // numbers
                {"10", "{num:10.0}"},
                {"123.", "{num:123.0}"},
                {"734.04", "{num:734.04}"},
                {"0.12345", "{num:0.12345}"},
                {".666", "{num:0.666}"},
                {"00000333.3330000", "{num:333.333}"},
                {"1230000000000000000000", "{num:1.23E21}"},
                {"0.00000000000000000123", "{num:1.23E-18}"},
                {"0", "{num:0.0}"},
                {"0.", "{num:0.0}"},
                {".0", "{num:0.0}"},
                {"0.0", "{num:0.0}"},
                // strings
                {"\"\"", "{str:''}"},
                {"\"   \"", "{str:'   '}"},
                {"''", "{str:''}"},
                {"'\"'", "{str:'\"'}"},
                {"\"'\"", "{str:'''}"},
                {"'mary had a little lamb'", "{str:'mary had a little lamb'}"},
                {"'unterminated string...", null},
                // variables
                {"$var", "{var:var}"},
                {"$qualified:name", "{var:qualified:name}"},
                {"$x:*", null},
                {"$", null},
                {"$$asdf", null},
                // parens nesting
                {"(5)", "{num:5.0}"},
                {"(( (( (5 )) )))  ", "{num:5.0}"},
                {")", null},
                {"(", null},
                {"()", null},
                {"(((3))", null},
                // operators
                {"5 + 5", "{binop-expr:+,{num:5.0},{num:5.0}}"},
                {"-5", "{unop-expr:num-neg,{num:5.0}}"},
                {"- 5", "{unop-expr:num-neg,{num:5.0}}"},
                {"----5", "{unop-expr:num-neg,{unop-expr:num-neg,{unop-expr:num-neg,{unop-expr:num-neg,{num:5.0}}}}}"},
                {"6 * - 7", "{binop-expr:*,{num:6.0},{unop-expr:num-neg,{num:7.0}}}"},
                {"0--0", "{binop-expr:-,{num:0.0},{unop-expr:num-neg,{num:0.0}}}"},
                {"+-", null},
                {"5 * 5", "{binop-expr:*,{num:5.0},{num:5.0}}"},
                {"5 div 5", "{binop-expr:/,{num:5.0},{num:5.0}}"},
                {"5/5", null},
                {"5 mod 5", "{binop-expr:%,{num:5.0},{num:5.0}}"},
                {"5%5", null},
                {"3mod4", "{binop-expr:%,{num:3.0},{num:4.0}}"},
                {"5 divseparate-token", "{binop-expr:/,{num:5.0},{path-expr:rel,{{step:child,separate-token}}}}"}, //not quite sure if this is legal xpath or not, but it *can* be parsed unambiguously
                {"5 = 5", "{binop-expr:==,{num:5.0},{num:5.0}}"},
                {"5 != 5", "{binop-expr:!=,{num:5.0},{num:5.0}}"},
                {"5 == 5", null},
                {"5 <> 5", null},
                {"5 < 5", "{binop-expr:<,{num:5.0},{num:5.0}}"},
                {"5 <= 5", "{binop-expr:<=,{num:5.0},{num:5.0}}"},
                {"5 > 5", "{binop-expr:>,{num:5.0},{num:5.0}}"},
                {"5 >= 5", "{binop-expr:>=,{num:5.0},{num:5.0}}"},
                {">=", null},
                {"'asdf'!=", null},
                {"5 and 5", "{binop-expr:and,{num:5.0},{num:5.0}}"},
                {"5 or 5", "{binop-expr:or,{num:5.0},{num:5.0}}"},
                {"5 | 5", "{binop-expr:union,{num:5.0},{num:5.0}}"},
                // operator associativity
                {"1 or 2 or 3", "{binop-expr:or,{num:1.0},{binop-expr:or,{num:2.0},{num:3.0}}}"},
                {"1 and 2 and 3", "{binop-expr:and,{num:1.0},{binop-expr:and,{num:2.0},{num:3.0}}}"},
                {"1 = 2 != 3 != 4 = 5", "{binop-expr:==,{binop-expr:!=,{binop-expr:!=,{binop-expr:==,{num:1.0},{num:2.0}},{num:3.0}},{num:4.0}},{num:5.0}}"},
                {"1 < 2 >= 3 <= 4 > 5", "{binop-expr:>,{binop-expr:<=,{binop-expr:>=,{binop-expr:<,{num:1.0},{num:2.0}},{num:3.0}},{num:4.0}},{num:5.0}}"},
                {"1 + 2 - 3 - 4 + 5", "{binop-expr:+,{binop-expr:-,{binop-expr:-,{binop-expr:+,{num:1.0},{num:2.0}},{num:3.0}},{num:4.0}},{num:5.0}}"},
                {"1 mod 2 div 3 div 4 * 5", "{binop-expr:*,{binop-expr:/,{binop-expr:/,{binop-expr:%,{num:1.0},{num:2.0}},{num:3.0}},{num:4.0}},{num:5.0}}"},
                {"1|2|3", "{binop-expr:union,{binop-expr:union,{num:1.0},{num:2.0}},{num:3.0}}"},
                // operator precedence
                {"1 < 2 = 3 > 4 and 5 <= 6 != 7 >= 8 or 9 and 10",
                        "{binop-expr:or,{binop-expr:and,{binop-expr:==,{binop-expr:<,{num:1.0},{num:2.0}},{binop-expr:>,{num:3.0},{num:4.0}}},{binop-expr:!=,{binop-expr:<=,{num:5.0},{num:6.0}},{binop-expr:>=,{num:7.0},{num:8.0}}}},{binop-expr:and,{num:9.0},{num:10.0}}}"},
                {"1 * 2 + 3 div 4 < 5 mod 6 | 7 - 8",
                        "{binop-expr:<,{binop-expr:+,{binop-expr:*,{num:1.0},{num:2.0}},{binop-expr:/,{num:3.0},{num:4.0}}},{binop-expr:-,{binop-expr:%,{num:5.0},{binop-expr:union,{num:6.0},{num:7.0}}},{num:8.0}}}"},
                {"- 4 * 6", "{binop-expr:*,{unop-expr:num-neg,{num:4.0}},{num:6.0}}"},
                {"8|-9", null}, //disallowed by the xpath spec
                {"6*(3+4)and(5or2)", "{binop-expr:and,{binop-expr:*,{num:6.0},{binop-expr:+,{num:3.0},{num:4.0}}},{binop-expr:or,{num:5.0},{num:2.0}}}"},
                // function calls
                {"function()", "{func-expr:function,{}}"},

                // test built-in xpath function parsing / serialization
                // not necessarily sensical function inputs, but includes valid argument counts
                {"abs(1)", "{func-expr:abs,{{num:1.0}}}"},
                {"acos(1)", "{func-expr:acos,{{num:1.0}}}"},
                {"asin(1)", "{func-expr:asin,{{num:1.0}}}"},
                {"atan(1)", "{func-expr:atan,{{num:1.0}}}"},
                {"atan2(1,1)", "{func-expr:atan2,{{num:1.0},{num:1.0}}}"},
                {"boolean(1)", "{func-expr:boolean,{{num:1.0}}}"},
                {"boolean-from-string(1)", "{func-expr:boolean-from-string,{{num:1.0}}}"},
                {"ceiling(1)", "{func-expr:ceiling,{{num:1.0}}}"},
                {"checklist(1, 1)", "{func-expr:checklist,{{num:1.0},{num:1.0}}}"},
                {"coalesce(1)", "{func-expr:coalesce,{{num:1.0}}}"},
                {"concat(1)", "{func-expr:concat,{{num:1.0}}}"},
                {"cond(1,2,3)", "{func-expr:cond,{{num:1.0},{num:2.0},{num:3.0}}}"},
                {"contains(1,1)", "{func-expr:contains,{{num:1.0},{num:1.0}}}"},
                {"cos(1)", "{func-expr:cos,{{num:1.0}}}"},
                {"count(1)", "{func-expr:count,{{num:1.0}}}"},
                {"count-selected(1)", "{func-expr:count-selected,{{num:1.0}}}"},
                {"date(1)", "{func-expr:date,{{num:1.0}}}"},
                {"depend(1)", "{func-expr:depend,{{num:1.0}}}"},
                {"distance(1,1)", "{func-expr:distance,{{num:1.0},{num:1.0}}}"},
                {"double(1)", "{func-expr:double,{{num:1.0}}}"},
                {"ends-with(1,1)", "{func-expr:ends-with,{{num:1.0},{num:1.0}}}"},
                {"exp(1)", "{func-expr:exp,{{num:1.0}}}"},
                {"false()", "{func-expr:false,{}}"},
                {"floor(1)", "{func-expr:floor,{{num:1.0}}}"},
                {"format-date-for-calendar(1,1)", "{func-expr:format-date-for-calendar,{{num:1.0},{num:1.0}}}"},
                {"format-date(1,1)", "{func-expr:format-date,{{num:1.0},{num:1.0}}}"},
                {"if(1,1,1)", "{func-expr:if,{{num:1.0},{num:1.0},{num:1.0}}}"},
                {"int(1)", "{func-expr:int,{{num:1.0}}}"},
                {"join(1)", "{func-expr:join,{{num:1.0}}}"},
                {"log(1)", "{func-expr:log,{{num:1.0}}}"},
                {"log10(1)", "{func-expr:log10,{{num:1.0}}}"},
                {"lower-case(1)", "{func-expr:lower-case,{{num:1.0}}}"},
                {"max(1)", "{func-expr:max,{{num:1.0}}}"},
                {"min(1)", "{func-expr:min,{{num:1.0}}}"},
                {"not(1)", "{func-expr:not,{{num:1.0}}}"},
                {"now()", "{func-expr:now,{}}"},
                {"number(1)", "{func-expr:number,{{num:1.0}}}"},
                {"pi()", "{func-expr:pi,{}}"},
                {"position()", "{func-expr:position,{}}"},
                {"pow(1,1)", "{func-expr:pow,{{num:1.0},{num:1.0}}}"},
                {"regex(1,1)", "{func-expr:regex,{{num:1.0},{num:1.0}}}"},
                {"replace(1,1,1)", "{func-expr:replace,{{num:1.0},{num:1.0},{num:1.0}}}"},
                {"round(1)", "{func-expr:round,{{num:1.0}}}"},
                {"selected-at(1,1)", "{func-expr:selected-at,{{num:1.0},{num:1.0}}}"},
                {"selected(1,1)", "{func-expr:selected,{{num:1.0},{num:1.0}}}"},
                {"sin(1)", "{func-expr:sin,{{num:1.0}}}"},
                {"sqrt(1)", "{func-expr:sqrt,{{num:1.0}}}"},
                {"starts-with(1,1)", "{func-expr:starts-with,{{num:1.0},{num:1.0}}}"},
                {"string(1)", "{func-expr:string,{{num:1.0}}}"},
                {"string-length(1)", "{func-expr:string-length,{{num:1.0}}}"},
                {"substr(1,1,1)", "{func-expr:substr,{{num:1.0},{num:1.0},{num:1.0}}}"},
                {"substring-after(1,1)", "{func-expr:substring-after,{{num:1.0},{num:1.0}}}"},
                {"substring-before(1,1)", "{func-expr:substring-before,{{num:1.0},{num:1.0}}}"},
                {"sum(1)", "{func-expr:sum,{{num:1.0}}}"},
                {"tan(1)", "{func-expr:tan,{{num:1.0}}}"},
                {"today()", "{func-expr:today,{}}"},
                {"translate(1,1,1)", "{func-expr:translate,{{num:1.0},{num:1.0},{num:1.0}}}"},
                {"true()", "{func-expr:true,{}}"},
                {"upper-case(1)", "{func-expr:upper-case,{{num:1.0}}}"},
                {"weighted-checklist(1, 1)", "{func-expr:weighted-checklist,{{num:1.0},{num:1.0}}}"},

                {"func:tion()", "{func-expr:func:tion,{}}"},
                {"function(   )", "{func-expr:function,{}}"},
                {"function (5)", "{func-expr:function,{{num:5.0}}}"},
                {"function   ( 5, 'arg', 4 * 12)", "{func-expr:function,{{num:5.0},{str:'arg'},{binop-expr:*,{num:4.0},{num:12.0}}}}"},
                {"function ( 4, 5, 6 ", null},
                {"4andfunc()", "{binop-expr:and,{num:4.0},{func-expr:func,{}}}"},
                // function calls that are actually node tests
                {"node()", "{path-expr:rel,{{step:child,node()}}}"},
                {"text()", "{path-expr:rel,{{step:child,text()}}}"},
                {"comment()", "{path-expr:rel,{{step:child,comment()}}}"},
                {"processing-instruction()", "{path-expr:rel,{{step:child,proc-instr()}}}"},
                {"processing-instruction('asdf')", "{path-expr:rel,{{step:child,proc-instr('asdf')}}}"},
                {"node(5)", null},
                {"text('str')", null},
                {"comment(name)", null},
                {"processing-instruction(5)", null},
                {"processing-instruction('asdf','qwer')", null},
                {"child::func()", null},
                // filter expressions
                {"bunch-o-nodes()[3]", "{filt-expr:{func-expr:bunch-o-nodes,{}},{{num:3.0}}}"},
                {"bunch-o-nodes()[3]['predicates'!='galore']", "{filt-expr:{func-expr:bunch-o-nodes,{}},{{num:3.0},{binop-expr:!=,{str:'predicates'},{str:'galore'}}}}"},
                {"(bunch-o-nodes)[3]", "{filt-expr:{path-expr:rel,{{step:child,bunch-o-nodes}}},{{num:3.0}}}"},
                {"bunch-o-nodes[3]", "{path-expr:rel,{{step:child,bunch-o-nodes,{{num:3.0}}}}}"},
                // path steps
                {".", "{path-expr:rel,{{step:self,node()}}}"},
                {"..", "{path-expr:rel,{{step:parent,node()}}}"},
                {"..[4]", null},
                {"preceding::..", null},
                // name tests
                {"name", "{path-expr:rel,{{step:child,name}}}"},
                {"qual:name", "{path-expr:rel,{{step:child,qual:name}}}"},
                {"a:b:c", null},
                {"inv#lid_N~AME", null},
                {".abc", null},
                {"5abc", null},
                {"_rea--ll:y.funk..y_N4M3", "{path-expr:rel,{{step:child,_rea--ll:y.funk..y_N4M3}}}"},
                {"namespace:*", "{path-expr:rel,{{step:child,namespace:*}}}"},
                {"*", "{path-expr:rel,{{step:child,*}}}"},
                {"*****", "{binop-expr:*,{binop-expr:*,{path-expr:rel,{{step:child,*}}},{path-expr:rel,{{step:child,*}}}},{path-expr:rel,{{step:child,*}}}}"},
                // axes
                {"child::*", "{path-expr:rel,{{step:child,*}}}"},
                {"parent::*", "{path-expr:rel,{{step:parent,*}}}"},
                {"descendant::*", "{path-expr:rel,{{step:descendant,*}}}"},
                {"ancestor::*", "{path-expr:rel,{{step:ancestor,*}}}"},
                {"following-sibling::*", "{path-expr:rel,{{step:following-sibling,*}}}"},
                {"preceding-sibling::*", "{path-expr:rel,{{step:preceding-sibling,*}}}"},
                {"following::*", "{path-expr:rel,{{step:following,*}}}"},
                {"preceding::*", "{path-expr:rel,{{step:preceding,*}}}"},
                {"attribute::*", "{path-expr:rel,{{step:attribute,*}}}"},
                {"namespace::*", "{path-expr:rel,{{step:namespace,*}}}"},
                {"self::*", "{path-expr:rel,{{step:self,*}}}"},
                {"descendant-or-self::*", "{path-expr:rel,{{step:descendant-or-self,*}}}"},
                {"ancestor-or-self::*", "{path-expr:rel,{{step:ancestor-or-self,*}}}"},
                {"bad-axis::*", null},
                {"::*", null},
                {"child::.", null},
                {"@attr", "{path-expr:rel,{{step:attribute,attr}}}"},
                {"@*", "{path-expr:rel,{{step:attribute,*}}}"},
                {"@ns:*", "{path-expr:rel,{{step:attribute,ns:*}}}"},
                {"@attr::*", null},
                // predicates
                {"descendant::node()[@attr='blah'][4]", "{path-expr:rel,{{step:descendant,node(),{{binop-expr:==,{path-expr:rel,{{step:attribute,attr}}},{str:'blah'}},{num:4.0}}}}}"},
                {"[2+3]", null},
                // paths
                {"rel/ative/path", "{path-expr:rel,{{step:child,rel},{step:child,ative},{step:child,path}}}"},
                {"rel/ative/path/", null},
                {"/abs/olute/path['etc']", "{path-expr:abs,{{step:child,abs},{step:child,olute},{step:child,path,{{str:'etc'}}}}}"},
                {"filter()/expr/path", "{path-expr:{filt-expr:{func-expr:filter,{}},{}},{{step:child,expr},{step:child,path}}}"},
                {"fil()['ter']/expr/path", "{path-expr:{filt-expr:{func-expr:fil,{}},{{str:'ter'}}},{{step:child,expr},{step:child,path}}}"},
                {"(another-filter)/expr/path", "{path-expr:{filt-expr:{path-expr:rel,{{step:child,another-filter}}},{}},{{step:child,expr},{step:child,path}}}"},
                {"filter-expr/(must-come)['first']", null},
                {"/", "{path-expr:abs,{}}"},
                {"//", null},
                {"//all", "{path-expr:abs,{{step:descendant-or-self,node()},{step:child,all}}}"},
                {"a/.//../z", "{path-expr:rel,{{step:child,a},{step:self,node()},{step:descendant-or-self,node()},{step:parent,node()},{step:child,z}}}"},
                {"6andpath", "{binop-expr:and,{num:6.0},{path-expr:rel,{{step:child,path}}}}"},
                // real-world examples
                {"/patient/sex = 'male' and /patient/age > 15",
                        "{binop-expr:and,{binop-expr:==,{path-expr:abs,{{step:child,patient},{step:child,sex}}},{str:'male'}},{binop-expr:>,{path-expr:abs,{{step:child,patient},{step:child,age}}},{num:15.0}}}"},
                {"../jr:hist-data/labs[@type=\"cd4\"]",
                        "{path-expr:rel,{{step:parent,node()},{step:child,jr:hist-data},{step:child,labs,{{binop-expr:==,{path-expr:rel,{{step:attribute,type}}},{str:'cd4'}}}}}}"},
                {"function_call(26*(7+3), //*, /im/child::an/ancestor::x[3][true()]/path)",
                        "{func-expr:function_call,{{binop-expr:*,{num:26.0},{binop-expr:+,{num:7.0},{num:3.0}}},{path-expr:abs,{{step:descendant-or-self,node()},{step:child,*}}},{path-expr:abs,{{step:child,im},{step:child,an},{step:ancestor,x,{{num:3.0},{func-expr:true,{}}}},{step:child,path}}}}}"}
        });
    }


    private final String inputString;
    private final String expectedParseOutput;

    public XPathParseTest(String inputString, String expectedParseOutput) {
        this.inputString = inputString;
        this.expectedParseOutput = expectedParseOutput;
    }

    @Test
    public void testPathParse() {
        if (this.expectedParseOutput != null) {
            testXPathValid(this.inputString, this.expectedParseOutput);
        } else {
            testXPathInvalid(this.inputString);
        }
    }

    private static void testXPathValid(String expr, String expected) {
        try {
            XPathExpression xpe = XPathParseTool.parseXPath(expr);
            String result = (xpe != null ? xpe.toString() : null);

            if (result == null || !result.equals(expected)) {
                fail("XPath Parse Failed! Incorrect parse tree." +
                        "\n    expression:[" + expr + "]" +
                        "\n    expected:[" + expected + "]" +
                        "\n    result:  [" + result + "]");
            }

            //test serialization of parse tree
            ExternalizableTest.testExternalizable(new ExtWrapTagged(xpe), new ExtWrapTagged(), new LivePrototypeFactory(), "XPath");
        } catch (XPathSyntaxException xse) {
            fail("XPath Parse Failed! Unexpected syntax error." +
                    "\n    expression:[" + expr + "]");
        }
    }

    private static void testXPathInvalid(String expr) {
        try {
            XPathExpression xpe = XPathParseTool.parseXPath(expr);
            String result = (xpe != null ? xpe.toString() : null);

            fail("XPath Parse Failed! Did not get syntax error as expected." +
                    "\n    expression:[" + expr + "]" +
                    "\n    result:[" + (result == null ? "(null)" : result) + "]");
        } catch (XPathSyntaxException xse) {
            //success: syntax error as expected
        }
    }
}
