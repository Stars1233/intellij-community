// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.debugger.sequence.trace.impl.handler.collections

import com.intellij.debugger.streams.core.trace.TerminatorCallHandler
import com.intellij.debugger.streams.core.trace.dsl.CodeBlock
import com.intellij.debugger.streams.core.trace.dsl.Dsl
import com.intellij.debugger.streams.core.trace.dsl.Expression
import com.intellij.debugger.streams.core.trace.dsl.impl.TextExpression
import com.intellij.debugger.streams.core.wrapper.IntermediateStreamCall
import com.intellij.debugger.streams.core.wrapper.TerminatorStreamCall

class CollectionTerminatorHandler(
    private val call: TerminatorStreamCall,
    private val resultExpression: String,
    private val dsl: Dsl,
    private val internalHandler: BothSemanticsHandler
) : TerminatorCallHandler, CollectionHandlerBase(Int.MAX_VALUE, dsl, call, internalHandler) {

    override fun prepareResult(): CodeBlock {
        val prepareResult = internalHandler.prepareResult(dsl, variables)
        val additionalCallsAfter = internalHandler.additionalCallsAfter(call, dsl)
        if (additionalCallsAfter.isEmpty()) {
            return prepareResult
        }

        return dsl.block {
            +createCallAfterExpression(additionalCallsAfter)
            add(prepareResult)
        }
    }

    override fun additionalCallsBefore(): List<IntermediateStreamCall> =
        internalHandler.additionalCallsBefore(call, dsl)

    override fun transformCall(call: TerminatorStreamCall): TerminatorStreamCall {
        return internalHandler.transformAsTerminalCall(call, variables, dsl)
    }

    private fun createCallAfterExpression(additionalCallsAfter: List<IntermediateStreamCall>): Expression {
        var result: Expression = TextExpression(resultExpression)
        for (call in additionalCallsAfter) {
            val args = call.arguments.map { TextExpression(it.text) }.toTypedArray()
            result = result.call(call.name + call.genericArguments, *args)
        }

        return result
    }
}