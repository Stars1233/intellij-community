// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.python.community.execService.python.impl

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

internal object PyExecPythonBundle {
  private const val BUNDLE_FQN: @NonNls String = "messages.PyExecPythonBundle"
  private val BUNDLE = DynamicBundle(PyExecPythonBundle::class.java, BUNDLE_FQN)

  fun message(
    key: @PropertyKey(resourceBundle = BUNDLE_FQN) String,
    vararg params: Any,
  ): @Nls String {
    return BUNDLE.getMessage(key, *params)
  }

  fun messagePointer(
    key: @PropertyKey(resourceBundle = BUNDLE_FQN) String,
    vararg params: Any,
  ): Supplier<String> {
    return BUNDLE.getLazyMessage(key, *params)
  }
}
