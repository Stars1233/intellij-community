// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.polySymbols.documentation

import com.intellij.model.psi.impl.targetSymbols
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.polySymbols.PolySymbol
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.ApiStatus
import kotlin.math.max

@ApiStatus.Internal
class PolySymbolDocumentationTargetProvider : DocumentationTargetProvider {
  override fun documentationTargets(file: PsiFile, offset: Int): List<DocumentationTarget> {
    val location = getContextElement(file, offset)
    return targetSymbols(file, offset).mapNotNull {
      (it as? PolySymbol)?.getDocumentationTarget(location)
    }
  }

  private fun getContextElement(file: PsiFile, offset: Int): PsiElement? =
    if (offset == file.textLength)
      file.findElementAt(max(0, offset - 1))
    else
      file.findElementAt(offset)
}