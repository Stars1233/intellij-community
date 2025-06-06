// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.externalSystem.service

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.getOrLogException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.ExternalSystemManager
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.service.project.ProjectRenameAware
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.util.ExternalSystemActivityKey
import com.intellij.openapi.externalSystem.util.ExternalSystemInProgressService
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.startup.StartupActivity
import com.intellij.platform.backend.observation.trackActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ExternalSystemStartupActivity : ProjectActivity {

  override suspend fun execute(project: Project) = project.trackActivity(ExternalSystemActivityKey) {
    project.serviceAsync<ExternalSystemInProgressService>().externalSystemActivityStarted()
    val esProjectsManager = readAction {
      ExternalProjectsManagerImpl.getInstance(project)
    }
    esProjectsManager.init()

    // do not compute in EDT
    val managers = ExternalSystemManager.EP_NAME.extensionList
    withContext(Dispatchers.EDT) {
      for (manager in managers) {
        runCatching {
          if (manager is StartupActivity) {
            manager.runActivity(project)
          }
          else if (manager is ProjectActivity) {
            manager.execute(project)
          }
        }.getOrLogException(logger<ExternalSystemStartupActivity>())
      }

      val isNewlyImportedProject = project.getUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT) == true
      val isNewlyCreatedProject = project.getUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT) == true
      if (!isNewlyImportedProject && isNewlyCreatedProject) {
        for (manager in managers) {
          runCatching {
            ExternalSystemUtil.refreshProjects(ImportSpecBuilder(project, manager.systemId).createDirectoriesForEmptyContentRoots())
          }.getOrLogException(logger<ExternalSystemStartupActivity>())
        }
      }

      ProjectRenameAware.beAware(project)
    }
  }
}
