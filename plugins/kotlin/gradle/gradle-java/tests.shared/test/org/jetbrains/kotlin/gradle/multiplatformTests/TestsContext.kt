// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.gradle.multiplatformTests

import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import org.jetbrains.kotlin.gradle.multiplatformTests.testFeatures.checkers.TestTasksChecker
import org.jetbrains.kotlin.gradle.multiplatformTests.testFeatures.checkers.workspace.GeneralWorkspaceChecks
import org.junit.runner.Description
import java.io.File

interface KotlinSyncTestsContext {
    val description: Description

    /**
     * Root of the actual project, i.e. a copy in the temp-directory, where the test runs
     */
    val testProjectRoot: File

    val testProject: Project

    val codeInsightTestFixture: CodeInsightTestFixture

    /**
     * Root of the project in the testdata, i.e. file somewhere in `intellij`-repo
     */
    val testDataDirectory: File

    val gradleJdkPath: File

    val testConfiguration: TestConfiguration

    val testFeatures: List<TestFeature<*>>

    val enabledFeatures: List<TestFeature<*>>

    val testProperties: KotlinTestProperties
}

/**
 * Basic implementation of [KotlinSyncTestsContext].
 *
 * It is not a drag-and-drop implementation. It meant to be used together with [AbstractKotlinMppGradleImportingTest]-like
 * runners that properly initialize lateinits, vars, etc.
 *
 * It leaves only [testProperties] not implemented, allowing for covariant overrides in inheritors
 * to provide more specific type
 */
abstract class AbstractKotlinSyncTestsContext(
    override val testFeatures: List<TestFeature<*>>
) : KotlinSyncTestsContext {
    override val testConfiguration: TestConfiguration = TestConfiguration()

    override lateinit var description: Description
    override lateinit var testProjectRoot: File
    override lateinit var testProject: Project
    override lateinit var gradleJdkPath: File

    var mutableCodeInsightTestFixture: CodeInsightTestFixture? = null
    override val codeInsightTestFixture: CodeInsightTestFixture get() = mutableCodeInsightTestFixture!!

    override val testDataDirectory: File by lazy { computeTestDataDirectory(description) }

    override val enabledFeatures: List<TestFeature<*>>
        get() {
            val config = testConfiguration.getConfiguration(GeneralWorkspaceChecks)

            return testFeatures.filter { feature ->
                if (config.disableCheckers != null && feature in config.disableCheckers!!) return@filter false
                if (config.onlyCheckers != null) return@filter feature in config.onlyCheckers!!

                feature.isEnabledByDefault()
            }
        }
}


class KotlinMppTestsContext(
    testFeatures: List<TestFeature<*>>
) : AbstractKotlinSyncTestsContext(testFeatures) {
    override val testProperties: KotlinMppTestProperties = KotlinMppTestProperties.construct(testConfiguration)

    override lateinit var description: Description
    override lateinit var testProjectRoot: File
    override lateinit var testProject: Project
    override lateinit var gradleJdkPath: File

    // Additionally mute TEST_TASKS checks due to issues with hosts on CI. See KT-56332
    override val enabledFeatures: List<TestFeature<*>>
        get() = super.enabledFeatures.filter { it !is TestTasksChecker }
}
