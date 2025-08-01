// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.intellij.build

import OpenSourceCommunityInstallersBuildTarget
import com.intellij.openapi.application.PathManager
import com.intellij.platform.buildScripts.testFramework.createBuildOptionsForTest
import com.intellij.platform.buildScripts.testFramework.runEssentialPluginsTest
import com.intellij.platform.buildScripts.testFramework.runTestBuild
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.intellij.build.BuildPaths.Companion.COMMUNITY_ROOT
import org.jetbrains.intellij.build.impl.BuildContextImpl
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class IdeaCommunityBuildTest {
  @Test
  fun build(testInfo: TestInfo) {
    val homePath = PathManager.getHomeDirFor(javaClass)!!
    val productProperties = IdeaCommunityProperties(COMMUNITY_ROOT.communityRoot)
    runTestBuild(
      homeDir = COMMUNITY_ROOT.communityRoot,
      testInfo = testInfo,
      productProperties = productProperties,
    ) {
      it.classOutDir = it.classOutDir ?: "$homePath/out/classes"
      /**
       * [com.intellij.platform.buildScripts.testFramework.customizeBuildOptionsForTest] modified [BuildOptions.buildStepsToSkip]
       * which should never be changed for this test because it's expected to match the production behavior
       */
      it.buildStepsToSkip = OpenSourceCommunityInstallersBuildTarget.OPTIONS.buildStepsToSkip +
                            // no need to publish TeamCity artifacts from a test
                            BuildOptions.TEAMCITY_ARTIFACTS_PUBLICATION_STEP
      // this step is disabled for all other build tests
      it.buildStepsToSkip -= BuildOptions.CROSS_PLATFORM_DISTRIBUTION_STEP
    }
  }

  @Test
  fun jpsStandalone(testInfo: TestInfo) {
    val homePath = PathManager.getHomeDirFor(javaClass)!!
    runBlocking(Dispatchers.Default) {
      runTestBuild(testInfo, context = {
        val productProperties = IdeaCommunityProperties(COMMUNITY_ROOT.communityRoot)
        val options = createBuildOptionsForTest(productProperties = productProperties, homeDir = homePath, skipDependencySetup = true, testInfo = testInfo)
        BuildContextImpl.createContext(
          projectHome = homePath,
          productProperties = productProperties,
          setupTracer = false,
          options = options,
        )
      }) {
        buildCommunityStandaloneJpsBuilder(targetDir = it.paths.artifactDir.resolve("jps"), context = it)
      }
    }
  }

  @Test
  fun `essential plugins depend only on essential plugins`() {
    val homePath = PathManager.getHomeDirFor(javaClass)!!
    runEssentialPluginsTest(homePath = homePath,
                            productProperties = IdeaCommunityProperties(COMMUNITY_ROOT.communityRoot),
                            buildTools = ProprietaryBuildTools.DUMMY)
  }
}