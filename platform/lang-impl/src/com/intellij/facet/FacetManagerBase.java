// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.facet;

import com.intellij.facet.impl.FacetEventsPublisher;
import com.intellij.facet.impl.FacetLoadingErrorDescription;
import com.intellij.facet.impl.invalid.InvalidFacet;
import com.intellij.facet.impl.invalid.InvalidFacetConfiguration;
import com.intellij.facet.impl.invalid.InvalidFacetManager;
import com.intellij.facet.impl.invalid.InvalidFacetType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ProjectLoadingErrorsNotifier;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.UnknownFeature;
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.UnknownFeaturesCollector;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.*;
import org.jetbrains.jps.model.serialization.facet.FacetState;

import java.util.Collection;
import java.util.Objects;

@ApiStatus.Internal
public abstract class FacetManagerBase extends FacetManager {
  private static final Logger LOG = Logger.getInstance(FacetManagerBase.class);
  public static final String FEATURE_TYPE = "com.intellij.facetType";

  @Override
  public @NotNull <F extends Facet<?>, C extends FacetConfiguration> F createFacet(final @NotNull FacetType<F, C> type, final @NotNull String name, final @NotNull C configuration,
                                                                                   final @Nullable Facet<?> underlying) {
    return createFacet(getModule(), type, name, configuration, underlying);
  }

  @ApiStatus.Internal
  public static void setFacetName(Facet<?> facet, String newName) {
    facet.setName(newName);
  }

  protected static @NotNull <F extends Facet<?>, C extends FacetConfiguration> F createFacet(@NotNull Module module, @NotNull FacetType<F, C> type,
                                                                                             @NotNull String name,
                                                                                             @NotNull C configuration,
                                                                                             @Nullable Facet<?> underlying) {
    final F facet = type.createFacet(module, name, configuration, underlying);
    assertTrue(facet.getModule() == module, facet, "module");
    assertTrue(facet.getConfiguration() == configuration, facet, "configuration");
    assertTrue(Objects.equals(facet.getName(), name), facet, "name");
    assertTrue(facet.getUnderlyingFacet() == underlying, facet, "underlyingFacet");
    return facet;
  }

  @Override
  public @NotNull <F extends Facet<?>, C extends FacetConfiguration> F createFacet(final @NotNull FacetType<F, C> type, final @NotNull String name, final @Nullable Facet<?> underlying) {
    C configuration = ProjectFacetManager.getInstance(getModule().getProject()).createDefaultConfiguration(type);
    return createFacet(type, name, configuration, underlying);
  }

  @Override
  public @NotNull <F extends Facet<?>, C extends FacetConfiguration> F addFacet(final @NotNull FacetType<F, C> type, final @NotNull String name, final @Nullable Facet<?> underlying) {
    final ModifiableFacetModel model = createModifiableModel();
    final F facet = createFacet(type, name, underlying);
    model.addFacet(facet);
    model.commit();
    return facet;
  }

  @Override
  public void facetConfigurationChanged(@NotNull Facet<?> facet) {
    FacetEventsPublisher.Companion.getInstance(facet.getModule().getProject()).fireFacetConfigurationChanged(facet);
  }

  @Override
  public Facet<?> @NotNull [] getAllFacets() {
    return getModel().getAllFacets();
  }

  @Override
  public @Nullable <F extends Facet<?>> F getFacetByType(FacetTypeId<F> typeId) {
    return getModel().getFacetByType(typeId);
  }

  protected abstract FacetModel getModel();

  @Override
  public @Nullable <F extends Facet<?>> F findFacet(final FacetTypeId<F> type, final String name) {
    return getModel().findFacet(type, name);
  }

  @Override
  public @Nullable <F extends Facet<?>> F getFacetByType(final @NotNull Facet<?> underlyingFacet, final FacetTypeId<F> typeId) {
    return getModel().getFacetByType(underlyingFacet, typeId);
  }

  @Override
  public @NotNull @Unmodifiable <F extends Facet<?>> Collection<F> getFacetsByType(final @NotNull Facet<?> underlyingFacet, final FacetTypeId<F> typeId) {
    return getModel().getFacetsByType(underlyingFacet, typeId);
  }

  @Override
  public @NotNull @Unmodifiable <F extends Facet<?>> Collection<F> getFacetsByType(FacetTypeId<F> typeId) {
    return getModel().getFacetsByType(typeId);
  }

  @Override
  public Facet<?> @NotNull [] getSortedFacets() {
    return getModel().getSortedFacets();
  }

  @Override
  public @NotNull String getFacetName(@NotNull Facet<?> facet) {
    return getModel().getFacetName(facet);
  }

  protected abstract Module getModule();

  @ApiStatus.Internal
  public static @NotNull InvalidFacet createInvalidFacet(@NotNull Module module, @NotNull FacetState state, @Nullable Facet<?> underlyingFacet,
                                                         @NotNull @NlsContexts.DialogMessage String errorMessage,
                                                         boolean unknownType, boolean reportError) {
    Project project = module.getProject();
    final InvalidFacetType type = InvalidFacetType.getInstance();
    final InvalidFacetConfiguration configuration = new InvalidFacetConfiguration(state, errorMessage);
    final InvalidFacet facet = createFacet(module, type, StringUtil.notNullize(state.getName()), configuration, underlyingFacet);
    if (reportError) {
      InvalidFacetManager invalidFacetManager = InvalidFacetManager.getInstance(project);
      if (!invalidFacetManager.isIgnored(facet)) {
        FacetLoadingErrorDescription description = new FacetLoadingErrorDescription(facet);
        ProjectLoadingErrorsNotifier.getInstance(project).registerError(description);

        if (unknownType) {
          UnknownFeaturesCollector.getInstance(project)
            .registerUnknownFeature(new UnknownFeature(description.getErrorType().getFeatureType(),
                                                       ProjectBundle.message("plugins.advertiser.feature.facet"),
                                                       state.getFacetType()));
        }
      }
    }
    return facet;
  }

  private static void assertTrue(final boolean value, final Facet<?> facet, final String parameter) {
    if (!value) {
      LOG.error("Facet type " + facet.getType().getClass().getName() + " violates the contract of FacetType.createFacet method about '" +
               parameter + "' parameter");
    }
  }

  @TestOnly
  public void checkConsistency() {
  }
}
