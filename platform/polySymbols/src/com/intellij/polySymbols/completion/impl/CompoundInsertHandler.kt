// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.polySymbols.completion.impl

import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.polySymbols.completion.PolySymbolCodeCompletionItemInsertHandler

internal class CompoundInsertHandler private constructor(val handlers: List<PolySymbolCodeCompletionItemInsertHandler>) : PolySymbolCodeCompletionItemInsertHandler {

  override fun prepare(context: InsertionContext, item: LookupElement, completeAfterInsert: Boolean): Runnable? {
    val runnables = handlers.asSequence()
      .sortedByDescending { it.priority }
      .distinct()
      .mapNotNull { it.prepare(context, item, completeAfterInsert) }
      .toList()
      .ifEmpty { return null }

    return Runnable {
      runnables.forEach { it.run() }
    }
  }

  companion object {

    fun merge(
      a: PolySymbolCodeCompletionItemInsertHandler?,
      b: PolySymbolCodeCompletionItemInsertHandler?,
    ): PolySymbolCodeCompletionItemInsertHandler? {
      if (a == null) return b
      if (b == null) return a

      val handlers = if (a is CompoundInsertHandler) {
        a.handlers.toMutableList()
      }
      else {
        mutableListOf(a)
      }
      if (b is CompoundInsertHandler) {
        handlers.addAll(b.handlers)
      }
      else {
        handlers.add(b)
      }
      return CompoundInsertHandler(handlers)
    }
  }

}