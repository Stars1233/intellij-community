// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.roots.ui.configuration

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.ProjectBundle
import com.intellij.openapi.projectRoots.SdkType
import com.intellij.openapi.roots.ui.SdkAppearanceService
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.popup.ListSeparator
import com.intellij.ui.*
import com.intellij.ui.components.JBLabel
import com.intellij.util.IconUtil
import java.awt.BorderLayout
import java.awt.Component
import java.util.function.Function
import java.util.function.Supplier
import javax.accessibility.AccessibleContext
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel

internal class SdkListPresenter2<T>(
  combo: ComboBox<T>?,
  private val modelSupplier: Supplier<SdkListModel>,
  private val listItemProducer: Function<T, SdkListItem>,
): GroupedComboBoxRenderer<T>(combo) {
  private lateinit var myArrow: JBLabel

  override fun separatorFor(value: T): ListSeparator? {
    return when (val text = modelSupplier.get().getSeparatorTextAbove(listItemProducer.apply(value))) {
      null -> null
      else -> ListSeparator(text)
    }
  }

  override fun layoutComponent(component: JComponent): JComponent {
    val panel = super.layoutComponent(component)
    myArrow = JBLabel()
    panel.add(myArrow, BorderLayout.EAST)
    return panel
  }

  override fun getListCellRendererComponent(list: JList<out T>?,
                                            value: T,
                                            index: Int,
                                            isSelected: Boolean,
                                            cellHasFocus: Boolean): Component {
    val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
    myArrow.isVisible = false

    if (index == -1 && modelSupplier.get().isSearching) {
      val panel: JPanel = object : CellRendererPanel(BorderLayout()) {
        override fun getAccessibleContext(): AccessibleContext = component.accessibleContext
      }
      component.background = null
      panel.add(component, BorderLayout.CENTER)
      val progressIcon = JBLabel(AnimatedIcon.Default.INSTANCE)
      panel.add(progressIcon, BorderLayout.EAST)
      return panel
    }

    val sdkListItem = listItemProducer.apply(value)

    if (sdkListItem is SdkListItem.GroupItem) {
      myArrow.isVisible = true
      myArrow.icon = if (isSelected) AllIcons.Icons.Ide.MenuArrowSelected else AllIcons.Icons.Ide.MenuArrow
    }

    if (component is JComponent) {
      component.toolTipText = if (sdkListItem is SdkListItem.SuggestedItem && SdkListPresenter.presentDetectedSdkPath(sdkListItem.homePath).contains("...")) sdkListItem.homePath else null
    }

    return component
  }

  override fun customize(item: SimpleColoredComponent, value: T, index: Int, isSelected: Boolean, cellHasFocus: Boolean) {
    when (val sdkListItem = listItemProducer.apply(value)) {
      is SdkListItem.InvalidSdkItem -> {
        item.append(ProjectBundle.message("jdk.combo.box.invalid.item", sdkListItem.sdkName), SimpleTextAttributes.ERROR_ATTRIBUTES)
      }

      is SdkListItem.NoneSdkItem -> {
        SdkAppearanceService.getInstance().forNullSdk(isSelected).customize(item)
      }

      is SdkListItem.SuggestedItem -> {
        var icon = sdkListItem.sdkType.icon
        if (icon != null && sdkListItem.isSymlink) {
          icon = AllIcons.Nodes.Related
        }
        item.icon = icon ?: IconUtil.addIcon

        item.append(sdkListItem.version)
        item.append(" ${SdkListPresenter.presentDetectedSdkPath(sdkListItem.homePath)}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
      }

      is SdkListItem.ProjectSdkItem -> {
        val sdk = modelSupplier.get().resolveProjectSdk()
        if (sdk != null) {
          item.icon = (sdk.sdkType as SdkType).icon
          item.append(ProjectBundle.message("project.roots.project.jdk.inherited"))
          item.append(" ${sdk.name}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        } else {
          item.append(ProjectBundle.message("jdk.combo.box.project.item"), SimpleTextAttributes.ERROR_ATTRIBUTES)
        }
      }

      is SdkListItem.SdkItem -> {
        SdkAppearanceService.getInstance()
          .forSdk(sdkListItem.sdk, false, isSelected, false)
          .customize(item)

        val version = sdkListItem.sdk.versionString ?: (sdkListItem.sdk.sdkType as SdkType).presentableName
        item.append(" $version", SimpleTextAttributes.GRAYED_ATTRIBUTES)
      }

      is SdkListItem.GroupItem -> {
        item.icon = sdkListItem.icon
        item.append(sdkListItem.caption)
      }

      is SdkListItem.SdkReferenceItem -> {
        SdkAppearanceService.getInstance()
          .forSdk(sdkListItem.sdkType, sdkListItem.name, null, sdkListItem.hasValidPath, false, isSelected)
          .customize(item)

        val version = sdkListItem.versionString ?: sdkListItem.sdkType.presentableName
        item.append(" $version", SimpleTextAttributes.GRAYED_ATTRIBUTES)
      }

      is SdkListItem.ActionItem -> {
        val template = sdkListItem.action.templatePresentation

        if (sdkListItem.group != null) {
          item.icon = when (sdkListItem.role) {
            SdkListItem.ActionRole.ADD -> sdkListItem.action.sdkType.icon ?: AllIcons.General.Add
            SdkListItem.ActionRole.DOWNLOAD -> template.icon
          }
          item.append(sdkListItem.action.listSubItemText)
        } else {
          item.icon = template.icon
          item.append(sdkListItem.action.listItemText)
        }
      }

      else -> SdkAppearanceService.getInstance().forNullSdk(isSelected).customize(item)
    }
  }
}