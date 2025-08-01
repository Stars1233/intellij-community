/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package org.jetbrains.jps.model;

import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;

public interface JpsNamedElement extends JpsElement {
  @NotNull
  @NlsSafe String getName();

  /**
   * @deprecated modifications of JpsModel were never fully supported, and they won't be since JpsModel will be superseded by 
   * {@link com.intellij.platform.workspace.storage.EntityStorage the workspace model}; 
   * also, if the name of an element is changed, it won't be reflected in {@link JpsNamedElementCollection}. 
   */
  @Deprecated(forRemoval = true)
  void setName(@NotNull String name);
}
