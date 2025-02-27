// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.featureStatistics;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtilRt;
import org.jdom.Element;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings("NotNullFieldNotInitialized")
public final class FeatureDescriptor {
  private @NotNull String myId;
  private @Nullable String myDisplayName;
  private final @Nullable String myGroupId;
  private @Nullable String myTipId;
  private @Nullable Set<String> myDependencies;
  private int myDaysBeforeFirstShowUp = 1;
  private int myDaysBetweenSuccessiveShowUps = 3;
  private int myMinUsageCount = 1;
  private int myUtilityScore = 3;  // should be from 1 to 5, required for tips sorting in Tips of the Day
  private boolean myNeedToBeShownInGuide = true;
  private final List<FeatureUsageEvent.Action> myActionEvents = new ArrayList<>();
  private final List<FeatureUsageEvent.Intention> myIntentionEvents = new ArrayList<>();

  private int myUsageCount;
  private long myLastTimeShown;
  private long myLastTimeUsed;
  private int myShownCount;
  private final @Nullable ProductivityFeaturesProvider myProvider;

  private static final @NonNls String ATTRIBUTE_COUNT = "count";
  private static final @NonNls String ATTRIBUTE_LAST_SHOWN = "last-shown";
  private static final @NonNls String ATTRIBUTE_LAST_USED = "last-used";
  private static final @NonNls String ATTRIBUTE_SHOWN_COUNT = "shown-count";
  private static final @NonNls String ATTRIBUTE_ID = "id";
  private static final @NonNls String ATTRIBUTE_TIP_ID = "tip-id";
  private static final @NonNls String ATTRIBUTE_FIRST_SHOW = "first-show";
  private static final @NonNls String ATTRIBUTE_SUCCESSIVE_SHOW = "successive-show";
  private static final @NonNls String ATTRIBUTE_MIN_USAGE_COUNT = "min-usage-count";
  private static final @NonNls String ATTRIBUTE_UTILITY_SCORE = "utility-score";
  private static final @NonNls String ATTRIBUTE_SHOW_IN_GUIDE = "show-in-guide";
  private static final @NonNls String ATTRIBUTE_CLASS_NAME = "class-name";
  private static final @NonNls String ELEMENT_DEPENDENCY = "dependency";
  private static final @NonNls String ELEMENT_TRACK_ACTION = "track-action";
  private static final @NonNls String ELEMENT_TRACK_INTENTION = "track-intention";

  @ApiStatus.Internal
  public FeatureDescriptor(@NotNull GroupDescriptor group, @Nullable ProductivityFeaturesProvider provider, @NotNull Element featureElement) {
    myGroupId = group.getId();
    myProvider = provider;
    readExternal(featureElement);
  }

  public FeatureDescriptor(@NonNls @NotNull String id,
                           @NonNls @Nullable String groupId,
                           @NonNls @Nullable String tipId,
                           @NotNull String displayName,
                           int daysBeforeFirstShowUp,
                           int daysBetweenSuccessiveShowUps,
                           @Nullable Set<String> dependencies,
                           int minUsageCount,
                           @Nullable ProductivityFeaturesProvider provider) {
    this(id, groupId, tipId, displayName, daysBeforeFirstShowUp, daysBetweenSuccessiveShowUps, dependencies, minUsageCount, 3, provider);
  }

  public FeatureDescriptor(@NonNls @NotNull String id,
                           @NonNls @Nullable String groupId,
                           @NonNls @Nullable String tipId,
                           @NotNull String displayName,
                           int daysBeforeFirstShowUp,
                           int daysBetweenSuccessiveShowUps,
                           @Nullable Set<String> dependencies,
                           int minUsageCount,
                           int utilityScore,
                           @Nullable ProductivityFeaturesProvider provider) {
    myId = id;
    myGroupId = groupId;
    myTipId = tipId;
    myDisplayName = displayName;
    myDaysBeforeFirstShowUp = daysBeforeFirstShowUp;
    myDaysBetweenSuccessiveShowUps = daysBetweenSuccessiveShowUps;
    myDependencies = dependencies;
    myMinUsageCount = minUsageCount;
    myUtilityScore = utilityScore;
    myProvider = provider;
  }

  private void readExternal(Element element) {
    myId = Objects.requireNonNull(element.getAttributeValue(ATTRIBUTE_ID));
    myTipId = element.getAttributeValue(ATTRIBUTE_TIP_ID);
    String needToBeShownInGuide = element.getAttributeValue(ATTRIBUTE_SHOW_IN_GUIDE);
    if (needToBeShownInGuide != null) {
      myNeedToBeShownInGuide = Boolean.parseBoolean(needToBeShownInGuide);
    }
    myDaysBeforeFirstShowUp = StringUtil.parseInt(element.getAttributeValue(ATTRIBUTE_FIRST_SHOW), myDaysBeforeFirstShowUp);
    myDaysBetweenSuccessiveShowUps = StringUtil.parseInt(element.getAttributeValue(ATTRIBUTE_SUCCESSIVE_SHOW), myDaysBetweenSuccessiveShowUps);
    myMinUsageCount = StringUtil.parseInt(element.getAttributeValue(ATTRIBUTE_MIN_USAGE_COUNT), myMinUsageCount);
    myUtilityScore = StringUtil.parseInt(element.getAttributeValue(ATTRIBUTE_UTILITY_SCORE), myUtilityScore);
    List<Element> actionEvents = element.getChildren(ELEMENT_TRACK_ACTION);
    for (Element actionElement : actionEvents) {
      @NonNls String actionId = actionElement.getAttributeValue(ATTRIBUTE_ID);
      if (actionId != null) {
        myActionEvents.add(FeatureUsageEvent.createActionEvent(myId, actionId));
      }
    }
    List<Element> intentionEvents = element.getChildren(ELEMENT_TRACK_INTENTION);
    for (Element intentionElement : intentionEvents) {
      @NonNls String intentionClassName = intentionElement.getAttributeValue(ATTRIBUTE_CLASS_NAME);
      if (intentionClassName != null) {
        myIntentionEvents.add(FeatureUsageEvent.createIntentionEvent(myId, intentionClassName));
      }
    }
    List<Element> dependencies = element.getChildren(ELEMENT_DEPENDENCY);
    if (!dependencies.isEmpty()) {
      myDependencies = new HashSet<>();
      for (Element dependencyElement : dependencies) {
        myDependencies.add(dependencyElement.getAttributeValue(ATTRIBUTE_ID));
      }
    }
  }

  public @NotNull String getId() {
    return myId;
  }

  public @Nullable String getGroupId() {
    return myGroupId;
  }

  public @Nullable String getTipId() {
    return myTipId;
  }

  public List<FeatureUsageEvent.Action> getActionEvents() {
    return myActionEvents;
  }

  public List<FeatureUsageEvent.Intention> getIntentionEvents() {
    return myIntentionEvents;
  }

  public @NotNull String getDisplayName() {
    if (myDisplayName == null) {
      myDisplayName = FeatureStatisticsBundle.message(myId);
    }
    return myDisplayName;
  }

  public boolean isNeedToBeShownInGuide() {
    return myNeedToBeShownInGuide;
  }

  public int getUsageCount() {
    return myUsageCount;
  }

  public Class<? extends ProductivityFeaturesProvider> getProvider() {
    if (myProvider == null) {
      return null;
    }
    return myProvider.getClass();
  }

  @ApiStatus.Internal
  public void triggerUsed() {
    myLastTimeUsed = System.currentTimeMillis();
    myUsageCount++;
  }

  public boolean isUnused() {
    return myUsageCount < myMinUsageCount;
  }

  public void adjustUsageInfo(int newUsageCount, long newLastTimeUsed) {
    myUsageCount = Math.max(myUsageCount, newUsageCount);
    myLastTimeUsed = Math.max(myLastTimeUsed, newLastTimeUsed);
  }

  @Override
  public String toString() {

    return "id = [" +
           myId +
           "], displayName = [" +
           myDisplayName +
           "], groupId = [" +
           myGroupId +
           "], usageCount = [" +
           myUsageCount +
           "]";
  }

  public int getDaysBeforeFirstShowUp() {
    return myDaysBeforeFirstShowUp;
  }

  public int getDaysBetweenSuccessiveShowUps() {
    return myDaysBetweenSuccessiveShowUps;
  }

  public int getUtilityScore() {
    return myUtilityScore;
  }

  public long getLastTimeShown() {
    return myLastTimeShown;
  }

  public String[] getDependencyFeatures() {
    if (myDependencies == null) return ArrayUtilRt.EMPTY_STRING_ARRAY;
    return ArrayUtilRt.toStringArray(myDependencies);
  }

  @ApiStatus.Internal
  public void triggerShown() {
    myLastTimeShown = System.currentTimeMillis();
    myShownCount++;
  }

  public long getLastTimeUsed() {
    return myLastTimeUsed;
  }

  public int getShownCount() {
    return myShownCount;
  }

  @ApiStatus.Internal
  public void copyStatistics(FeatureDescriptor statistics) {
    myUsageCount = statistics.getUsageCount();
    myLastTimeShown = statistics.getLastTimeShown();
    myLastTimeUsed = statistics.getLastTimeUsed();
    myShownCount = statistics.getShownCount();
  }

  @ApiStatus.Internal
  public void readStatistics(Element element) {
    String count = element.getAttributeValue(ATTRIBUTE_COUNT);
    String lastShown = element.getAttributeValue(ATTRIBUTE_LAST_SHOWN);
    String lastUsed = element.getAttributeValue(ATTRIBUTE_LAST_USED);
    String shownCount = element.getAttributeValue(ATTRIBUTE_SHOWN_COUNT);

    myUsageCount = count == null ? 0 : Integer.parseInt(count);
    myLastTimeShown = lastShown == null ? 0 : Long.parseLong(lastShown);
    myLastTimeUsed = lastUsed == null ? 0 : Long.parseLong(lastUsed);
    myShownCount = shownCount == null ? 0 : Integer.parseInt(shownCount);
  }

  @ApiStatus.Internal
  public void writeStatistics(Element element) {
    element.setAttribute(ATTRIBUTE_COUNT, String.valueOf(getUsageCount()));
    element.setAttribute(ATTRIBUTE_LAST_SHOWN, String.valueOf(getLastTimeShown()));
    element.setAttribute(ATTRIBUTE_LAST_USED, String.valueOf(getLastTimeUsed()));
    element.setAttribute(ATTRIBUTE_SHOWN_COUNT, String.valueOf(getShownCount()));
  }
}