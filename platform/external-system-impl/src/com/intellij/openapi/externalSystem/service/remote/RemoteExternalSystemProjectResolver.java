/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.externalSystem.service.remote;

import com.intellij.openapi.externalSystem.importing.ProjectResolverPolicy;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType;
import com.intellij.openapi.externalSystem.service.RemoteExternalSystemService;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@ApiStatus.Internal
public interface RemoteExternalSystemProjectResolver<S extends ExternalSystemExecutionSettings> extends RemoteExternalSystemService<S> {

  RemoteExternalSystemProjectResolver<ExternalSystemExecutionSettings> NULL_OBJECT = new RemoteExternalSystemProjectResolver<>() {
    //@formatter:off
    @Override public @Nullable DataNode<ProjectData> resolveProjectInfo(@NotNull ExternalSystemTaskId id, @NotNull String projectPath, boolean isPreviewMode, @Nullable ExternalSystemExecutionSettings settings, @Nullable ProjectResolverPolicy resolverPolicy) { return null; }
    @Override public void setSettings(@NotNull ExternalSystemExecutionSettings settings) { }
    @Override public void setNotificationListener(@NotNull ExternalSystemTaskNotificationListener notificationListener) { }
    @Override public boolean isTaskInProgress(@NotNull ExternalSystemTaskId id) { return false; }
    @Override public boolean cancelTask(@NotNull ExternalSystemTaskId id) { return false; }
    @Override public @NotNull Map<ExternalSystemTaskType, Set<ExternalSystemTaskId>> getTasksInProgress() { return Collections.emptyMap(); }
    //@formatter:on
  };

  @Nullable DataNode<ProjectData> resolveProjectInfo(
    @NotNull ExternalSystemTaskId id,
    @NotNull String projectPath,
    boolean isPreviewMode,
    @Nullable S settings,
    @Nullable ProjectResolverPolicy resolverPolicy
  ) throws RemoteException, ExternalSystemException, IllegalArgumentException, IllegalStateException;
}
