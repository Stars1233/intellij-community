// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.idea.maven.project.importing

import com.intellij.platform.util.progress.RawProgressReporter
import kotlinx.coroutines.runBlocking
import org.jetbrains.idea.maven.buildtool.MavenLogEventHandler
import org.jetbrains.idea.maven.project.MavenEmbeddersManager
import org.jetbrains.idea.maven.project.MavenPluginResolver
import org.junit.Test

class MavenProjectsTreeReadingPluginTest : MavenProjectsTreeTestCase() {
  
  @Test
  fun testDoNotUpdateChildAfterParentWasResolved() = runBlocking {
    createProjectPom("""
                     <groupId>test</groupId>
                     <artifactId>parent</artifactId>
                     <version>1</version>
                     """.trimIndent())
    val child = createModulePom("child",
                                """
                                <groupId>test</groupId>
                                <artifactId>child</artifactId>
                                <version>1</version>
                                <parent>
                                  <groupId>test</groupId>
                                  <artifactId>parent</artifactId>
                                  <version>1</version>
                                </parent>
                                """.trimIndent())
    val listener = MyLoggingListener()
    tree.addListener(listener, getTestRootDisposable())
    updateAll(projectPom, child)
    val parentProject = tree.findProject(projectPom)!!
    val embeddersManager = MavenEmbeddersManager(project)
    try {
      resolve(project,
              parentProject,
              mavenGeneralSettings,
              embeddersManager)
      val pluginResolver = MavenPluginResolver(tree)
      val progressReporter = object : RawProgressReporter {}
      pluginResolver.resolvePlugins(listOf(parentProject),
                                    embeddersManager,
                                    progressReporter,
                                    MavenLogEventHandler)
    }
    finally {
      embeddersManager.releaseInTests()
    }
    assertEquals(
      log()
        .add("updated", "parent", "child")
        .add("deleted")
        .add("resolved", "parent")
        .add("plugins", "parent"),
      listener.log)
    tree.updateAll(false, mavenGeneralSettings, mavenEmbedderWrappers, rawProgressReporter)
    assertEquals(
      log()
        .add("updated", "parent", "child")
        .add("deleted")
        .add("resolved", "parent")
        .add("plugins", "parent"),
      listener.log)
  }
}