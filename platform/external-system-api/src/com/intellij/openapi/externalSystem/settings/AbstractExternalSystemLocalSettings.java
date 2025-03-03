// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.externalSystem.settings;

import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.execution.ExternalTaskExecutionInfo;
import com.intellij.openapi.externalSystem.model.project.ExternalProjectPojo;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.SmartList;
import com.intellij.util.containers.CollectionFactory;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

import static com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.*;

/**
 * Holds local project-level external system-related settings (should be kept at the '*.iws' or 'workspace.xml').
 * <p/>
 * For example, we don't want to store recent tasks list at common external system settings, hence, that data
 * is kept at user-local settings.
 * <p/>
 * <b>Note:</b> non-abstract subclasses of this class are expected to be marked by {@link State} annotation configured
 * to be stored under a distinct name at a {@link StoragePathMacros#CACHE_FILE}.
 */
public abstract class AbstractExternalSystemLocalSettings<S extends AbstractExternalSystemLocalSettings.State> {
  protected S state;

  private final @NotNull ProjectSystemId myExternalSystemId;
  private final @NotNull Project myProject;

  protected AbstractExternalSystemLocalSettings(@NotNull ProjectSystemId externalSystemId, @NotNull Project project, @NotNull S state) {
    myExternalSystemId = externalSystemId;
    myProject = project;
    this.state = state;
  }

  protected AbstractExternalSystemLocalSettings(@NotNull ProjectSystemId externalSystemId, @NotNull Project project) {
    //noinspection unchecked
    this(externalSystemId, project, (S)new State());
  }

  /**
   * Asks current settings to drop all information related to external projects which root configs are located at the given paths.
   *
   * @param linkedProjectPathsToForget target root external project paths
   */
  public void forgetExternalProjects(@NotNull Set<String> linkedProjectPathsToForget) {
    Map<ExternalProjectPojo, Collection<ExternalProjectPojo>> projects = state.availableProjects;
    for (Iterator<Map.Entry<ExternalProjectPojo, Collection<ExternalProjectPojo>>> it = projects.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<ExternalProjectPojo, Collection<ExternalProjectPojo>> entry = it.next();
      if (linkedProjectPathsToForget.contains(entry.getKey().getPath())) {
        it.remove();
      }
    }

    if (!ContainerUtil.isEmpty(state.recentTasks)) {
      for (Iterator<ExternalTaskExecutionInfo> it = state.recentTasks.iterator(); it.hasNext(); ) {
        ExternalTaskExecutionInfo taskInfo = it.next();
        String path = taskInfo.getSettings().getExternalProjectPath();
        if (linkedProjectPathsToForget.contains(path) ||
            linkedProjectPathsToForget.contains(getRootProjectPath(path, myExternalSystemId, myProject))) {
          it.remove();
        }
      }
    }

    for (Iterator<Map.Entry<String, SyncType>> it = state.projectSyncType.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<String, SyncType> entry = it.next();
      if (linkedProjectPathsToForget.contains(entry.getKey())
          || linkedProjectPathsToForget.contains(getRootProjectPath(entry.getKey(), myExternalSystemId, myProject))) {
        it.remove();
      }
    }

    Map<String, Long> modificationStamps = state.modificationStamps;
    for (String path : linkedProjectPathsToForget) {
      modificationStamps.remove(path);
    }
  }

  public @NotNull Map<ExternalProjectPojo, Collection<ExternalProjectPojo>> getAvailableProjects() {
    return state.availableProjects;
  }

  public void setAvailableProjects(@NotNull Map<ExternalProjectPojo, Collection<ExternalProjectPojo>> projects) {
    state.availableProjects = projects;
  }

  public @NotNull @Unmodifiable List<ExternalTaskExecutionInfo> getRecentTasks() {
    return ContainerUtil.notNullize(state.recentTasks);
  }

  public @NotNull Map<String, Long> getExternalConfigModificationStamps() {
    return state.modificationStamps;
  }

  public @NotNull Map<String, SyncType> getProjectSyncType() {
    return state.projectSyncType;
  }

  public @Nullable S getState() {
    return state;
  }

  public void loadState(@NotNull State state) {
    //noinspection unchecked
    this.state = (S)state;
    pruneOutdatedEntries();
  }

  public void invalidateCaches() {
    state.recentTasks.clear();
    state.availableProjects.clear();
    state.modificationStamps.clear();
    state.projectSyncType.clear();
  }

  private void pruneOutdatedEntries() {
    ExternalSystemManager<?, ?, ?, ?, ?> manager = getManager(myExternalSystemId);
    assert manager != null;
    Set<String> pathsToForget = new HashSet<>();
    for (ExternalProjectPojo pojo : state.availableProjects.keySet()) {
      pathsToForget.add(pojo.getPath());
    }
    for (ExternalTaskExecutionInfo taskInfo : ContainerUtil.notNullize(state.recentTasks)) {
      pathsToForget.add(taskInfo.getSettings().getExternalProjectPath());
    }

    AbstractExternalSystemSettings<?, ?, ?> settings = manager.getSettingsProvider().fun(myProject);
    for (ExternalProjectSettings projectSettings : settings.getLinkedProjectsSettings()) {
      pathsToForget.remove(projectSettings.getExternalProjectPath());
    }
    for (Module module : ModuleManager.getInstance(myProject).getModules()) {
      if (!isExternalSystemAwareModule(myExternalSystemId, module)) continue;
      pathsToForget.remove(getExternalProjectPath(module));
    }

    if (!pathsToForget.isEmpty()) {
      forgetExternalProjects(pathsToForget);
    }
  }

  public static class State {
    public final List<ExternalTaskExecutionInfo> recentTasks = new SmartList<>();
    public Map<ExternalProjectPojo, Collection<ExternalProjectPojo>> availableProjects = CollectionFactory.createSmallMemoryFootprintMap();
    public Map<String/* linked project path */, Long/* last config modification stamp */> modificationStamps = CollectionFactory.createSmallMemoryFootprintMap();
    public Map<String/* linked project path */, SyncType> projectSyncType = CollectionFactory.createSmallMemoryFootprintMap();
  }

  public enum SyncType {
    PREVIEW, IMPORT, RE_IMPORT
  }
}
