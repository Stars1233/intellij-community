// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.notebooks.visualization.ui.jupyterToolbars

import com.intellij.notebooks.ui.SelectClickedCellEventHelper
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.ui.JBColor
import com.intellij.ui.NewUiValue
import com.intellij.ui.RoundedLineBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.StartupUiUtil
import org.jetbrains.annotations.ApiStatus
import java.awt.AlphaComposite
import java.awt.Cursor
import java.awt.Graphics
import java.awt.Graphics2D
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import javax.swing.BorderFactory
import javax.swing.JComponent

@ApiStatus.Internal
abstract class JupyterAbstractAboveCellToolbar(
  actionGroup: ActionGroup,
  toolbarTargetComponent: JComponent,
  place: String = ActionPlaces.EDITOR_INLAY
): ActionToolbarImpl(place, actionGroup, true) {
  protected var isBeingRemoved: Boolean = false

  init {
    val borderColor = when (NewUiValue.isEnabled()) {
      true -> JBColor.namedColor("Editor.Toolbar.borderColor", JBColor.border())
      else -> JBColor.GRAY
    }
    border = BorderFactory.createCompoundBorder(RoundedLineBorder(borderColor, getArcSize(), TOOLBAR_BORDER_THICKNESS),
                                                BorderFactory.createEmptyBorder(getVerticalPadding(), getHorizontalPadding(), getVerticalPadding(), getHorizontalPadding()))
    isOpaque = false
    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    targetComponent = toolbarTargetComponent
    putClientProperty(SelectClickedCellEventHelper.SKIP_CLICK_PROCESSING_FOR_CELL_SELECTION, true)
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as Graphics2D
    try {
      g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ALPHA)
      g2.color = this.background
      fillRect(g2)
    }
    finally {
      g2.dispose()
    }
  }

  protected open fun fillRect(g2: Graphics2D) {
    g2.fillRoundRect(0, 0, width, height, getArcSize(), getArcSize())
  }

  override fun updateUI() {
    super.updateUI()
    if (!StartupUiUtil.isDarkTheme) {
      background = JBColor.WHITE
    }
  }

  override fun addNotify() {
    super.addNotify()
    updateActionsImmediately(true)
  }

  override fun removeNotify() {
    isBeingRemoved = true
    super.removeNotify()
  }

  override fun updateActionsAsync(): Future<*> {
    return when (isBeingRemoved) {
      true -> CompletableFuture.completedFuture(null)
      else -> super.updateActionsAsync()
    }
  }

  protected abstract fun getArcSize(): Int
  protected abstract fun getHorizontalPadding(): Int
  protected open fun getVerticalPadding(): Int = getHorizontalPadding()

  companion object {
    const val ALPHA: Float = 1.0f
    val TOOLBAR_BORDER_THICKNESS: Int = JBUI.scale(1)
  }
}