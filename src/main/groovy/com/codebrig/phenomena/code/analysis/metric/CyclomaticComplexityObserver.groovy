package com.codebrig.phenomena.code.analysis.metric

import com.codebrig.omnisrc.SourceNode
import com.codebrig.omnisrc.SourceNodeFilter
import com.codebrig.omnisrc.observe.filter.FunctionFilter
import com.codebrig.omnisrc.observe.filter.MultiFilter
import com.codebrig.omnisrc.observe.filter.conditional.ElseIfConditionalFilter
import com.codebrig.omnisrc.observe.filter.conditional.IfConditionalFilter
import com.codebrig.omnisrc.observe.filter.conditional.SwitchCaseConditionalFilter
import com.codebrig.omnisrc.observe.filter.exception.CatchFilter
import com.codebrig.omnisrc.observe.filter.loop.DoWhileLoopFilter
import com.codebrig.omnisrc.observe.filter.loop.ForEachLoopFilter
import com.codebrig.omnisrc.observe.filter.loop.ForLoopFilter
import com.codebrig.omnisrc.observe.filter.loop.WhileLoopFilter
import com.codebrig.omnisrc.observe.filter.operator.logical.AndOperatorFilter
import com.codebrig.omnisrc.observe.filter.operator.logical.OrOperatorFilter
import com.codebrig.omnisrc.observe.filter.operator.misc.TernaryOperatorFilter
import com.codebrig.phenomena.code.CodeObserver
import com.codebrig.phenomena.code.ContextualNode
import com.google.common.base.Charsets
import com.google.common.io.Resources

/**
 * Creates an additional attribute on function declarations
 * with the function's cyclomatic complexity.
 *
 * @version 0.2
 * @since 0.2
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
class CyclomaticComplexityObserver implements CodeObserver {

    private final FunctionFilter functionFilter
    private final MultiFilter complexityFilter

    CyclomaticComplexityObserver() {
        functionFilter = new FunctionFilter()
        complexityFilter = MultiFilter.matchAny(
                new IfConditionalFilter(),
                new ElseIfConditionalFilter(),
                new WhileLoopFilter(),
                new DoWhileLoopFilter(),
                new ForLoopFilter(),
                new ForEachLoopFilter(),
                new SwitchCaseConditionalFilter(),
                new CatchFilter(),
                new AndOperatorFilter(),
                new OrOperatorFilter(),
                new TernaryOperatorFilter()
        )
    }

    @Override
    void applyObservation(ContextualNode node, ContextualNode parentNode) {
        def cyclomaticComplexity = 1
        def stack = new Stack<SourceNode>()
        stack.push(node)

        while (!stack.isEmpty()) {
            stack.pop().children.each {
                // don't follow inner functions (python, go, etc)
                if (!functionFilter.evaluate(it)) {
                    stack.push(it)

                    if (complexityFilter.evaluate((it))) {
                        cyclomaticComplexity++
                    }
                }
            }
        }
        node.hasAttribute("cyclomaticComplexity", cyclomaticComplexity as long)
    }

    @Override
    SourceNodeFilter getFilter() {
        return functionFilter
    }

    @Override
    String getSchema() {
        return Resources.toString(Resources.getResource(
                "schema/metric/cyclomatic-complexity-schema.gql"), Charsets.UTF_8)
    }
}
