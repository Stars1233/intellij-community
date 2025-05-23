// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.tools.projectWizard.wizard.services

import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import org.jetbrains.idea.maven.utils.MavenUtil
import org.jetbrains.kotlin.idea.base.test.AndroidStudioTestUtils
import org.jetbrains.kotlin.tools.projectWizard.cli.TestWizardService
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.service.ProjectImportingWizardService
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.ModuleIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemSettings
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.isGradle
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.nio.file.Path

class GradleProjectImportingTestWizardService(private val project: Project) : ProjectImportingWizardService, TestWizardService {
    override fun isSuitableFor(buildSystemType: BuildSystemType): Boolean =
        buildSystemType.isGradle || buildSystemType == BuildSystemType.Maven

    override fun importProject(
        reader: Reader,
        path: Path,
        modulesIrs: List<ModuleIR>,
        buildSystem: BuildSystemType,
        buildSystemSettings: BuildSystemSettings?
    ): TaskResult<Unit> {
        AndroidStudioTestUtils.specifyAndroidSdk(path.toFile())

        var importingErrorMessage: String? = null
        var importingErrorDetails: String = ""

        ExternalSystemUtil.refreshProjects(
            ImportSpecBuilder(project, buildSystem.externalSystemId() ?: error("Unsupported build system $buildSystem"))
                .use(ProgressExecutionMode.MODAL_SYNC)
                .callback(object : ExternalProjectRefreshCallback {
                    override fun onSuccess(externalProject: DataNode<ProjectData>?) {
                        if (externalProject == null) {
                            importingErrorMessage = "Got null External project after import"
                        } else {
                            service<ProjectDataManager>().importData(externalProject, project)
                        }
                    }

                    override fun onFailure(errorMessage: String, errorDetails: String?) {
                        importingErrorMessage = errorMessage
                        if (errorDetails != null) {
                            importingErrorDetails = errorDetails
                        }
                    }
                })
        )

        return importingErrorMessage?.let { message ->
            Failure(
                ProjectImportingError(
                    reader { KotlinPlugin.version.propertyValue.version.toString() },
                    message,
                    importingErrorDetails,
                )
            )
        } ?: UNIT_SUCCESS
    }

    private fun BuildSystemType.externalSystemId() = when (this) {
        BuildSystemType.GradleKotlinDsl -> GradleConstants.SYSTEM_ID
        BuildSystemType.GradleGroovyDsl -> GradleConstants.SYSTEM_ID
        BuildSystemType.Jps -> null
        BuildSystemType.Maven -> MavenUtil.SYSTEM_ID
    }
}