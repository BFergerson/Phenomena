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
 * todo: description
 *
 * @version 0.2
 * @since 0.2
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
class CyclomaticComplexity implements CodeObserver {

    private final FunctionFilter functionFilter
    private final MultiFilter complexityFilter

    CyclomaticComplexity() {
        functionFilter = new FunctionFilter()
        complexityFilter = new MultiFilter(MultiFilter.MatchStyle.ANY)
        complexityFilter.accept(new IfConditionalFilter())
        complexityFilter.accept(new ElseIfConditionalFilter())
        complexityFilter.accept(new WhileLoopFilter())
        complexityFilter.accept(new DoWhileLoopFilter())
        complexityFilter.accept(new ForLoopFilter())
        complexityFilter.accept(new ForEachLoopFilter())
        complexityFilter.accept(new SwitchCaseConditionalFilter())
        complexityFilter.accept(new CatchFilter())
        complexityFilter.accept(new AndOperatorFilter())
        complexityFilter.accept(new OrOperatorFilter())
        complexityFilter.accept(new TernaryOperatorFilter())
    }

    @Override
    void applyObservation(ContextualNode node, ContextualNode parentNode, ContextualNode previousNode) {
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
        node.hasAttribute("cyclomatic_complexity", cyclomaticComplexity as long)
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
