// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.tools.build.bazel.uiDesigner.lw;

public interface IButtonGroup {
  String getName();
  boolean isBound();
  String[] getComponentIds();
}
