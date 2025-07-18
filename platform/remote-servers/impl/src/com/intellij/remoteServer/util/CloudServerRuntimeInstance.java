// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.remoteServer.util;

import com.intellij.remoteServer.agent.RemoteAgentManager;
import com.intellij.remoteServer.agent.RemoteAgentProxyFactory;
import com.intellij.remoteServer.agent.util.CloudAgent;
import com.intellij.remoteServer.agent.util.CloudAgentConfigBase;
import com.intellij.remoteServer.agent.util.CloudRemoteApplication;
import com.intellij.remoteServer.configuration.deployment.DeploymentConfiguration;
import com.intellij.remoteServer.runtime.Deployment;
import com.intellij.remoteServer.runtime.ServerTaskExecutor;
import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * @author michael.golubev
 */
@ApiStatus.Internal
public abstract class CloudServerRuntimeInstance
  <DC extends DeploymentConfiguration,
    A extends CloudAgent,
    SC extends CloudAgentConfigBase>
  extends ServerRuntimeInstance<DC> {

  private final A myAgent;
  private final SC myConfiguration;
  private final ServerTaskExecutor myTasksExecutor;

  private final AgentTaskExecutor myAgentTaskExecutor;

  public CloudServerRuntimeInstance(SC configuration,
                                    ServerTaskExecutor tasksExecutor,
                                    List<Path> libraries,
                                    List<Class<?>> commonJarClasses,
                                    String specificsModuleName,
                                    String specificJarPath,
                                    Class<A> agentInterface,
                                    String agentClassName) throws Exception {
    this(null, configuration, tasksExecutor,
         libraries, commonJarClasses, specificsModuleName, specificJarPath, agentInterface, agentClassName);
  }

  public CloudServerRuntimeInstance(@Nullable RemoteAgentProxyFactory proxyFactory,
                                    SC configuration,
                                    ServerTaskExecutor tasksExecutor,
                                    List<Path> libraries,
                                    List<Class<?>> commonJarClasses,
                                    String specificsModuleName,
                                    String specificJarPath,
                                    Class<A> agentInterface,
                                    String agentClassName) throws Exception {
    myConfiguration = configuration;
    myTasksExecutor = tasksExecutor;

    RemoteAgentManager agentManager = RemoteAgentManager.getInstance();
    @NotNull RemoteAgentProxyFactory remoteAgentProxyFactory =
      proxyFactory != null ? proxyFactory : agentManager.createReflectiveThreadProxyFactory(getClass().getClassLoader());

    myAgent = agentManager.createAgent(remoteAgentProxyFactory,
                                       libraries,
                                       commonJarClasses,
                                       specificsModuleName,
                                       specificJarPath,
                                       agentInterface,
                                       agentClassName,
                                       getClass());

    myAgentTaskExecutor = new AgentTaskExecutor();
  }

  public SC getConfiguration() {
    return myConfiguration;
  }

  public ServerTaskExecutor getTaskExecutor() {
    return myTasksExecutor;
  }

  public A getAgent() {
    return myAgent;
  }

  protected final AgentTaskExecutor getAgentTaskExecutor() {
    return myAgentTaskExecutor;
  }

  @Override
  public void computeDeployments(final @NotNull ComputeDeploymentsCallback callback) {
    getTaskExecutor().submit(() -> {
      try {
        for (CloudApplicationRuntime application : getApplications()) {
          Deployment deployment
            = callback.addDeployment(application.getApplicationName(),
                                     application,
                                     application.getStatus(),
                                     application.getStatusText());
          application.setDeploymentModel(deployment);
        }
        callback.succeeded();
      }
      catch (ServerRuntimeException e) {
        callback.errorOccurred(e.getMessage());
      }
    }, callback);
  }

  protected @Unmodifiable List<CloudApplicationRuntime> getApplications() throws ServerRuntimeException {
    return getAgentTaskExecutor().execute(() -> {
      CloudRemoteApplication[] applications = getAgent().getApplications();
      if (applications == null) {
        return Collections.emptyList();
      }
      return ContainerUtil.map(applications, application -> createApplicationRuntime(application));
    });
  }

  protected abstract CloudApplicationRuntime createApplicationRuntime(CloudRemoteApplication application);
}
