// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.gradle.multiplatformTests

import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.model.FrameworkMethod

/**
 * Custom test runner to change displayed name of tests to include "parametrization"
 */
class KotlinMppTestsJUnit4Runner(testClass: Class<*>) : BlockJUnit4ClassRunner(testClass) {
    override fun testName(method: FrameworkMethod?): String {
        val props = KotlinMppTestProperties.construct(TestConfiguration())
        val agpVersion = props.agpVersion
        val kotlinVersion = props.kotlinVersion
        val gradleVersion = props.gradleVersion

        return super.testName(method) + "[$kotlinVersion, $gradleVersion, AGP $agpVersion]"
    }
}
