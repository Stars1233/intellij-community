/*
 * Copyright 2007 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.intellij.plugins.relaxNG.model.descriptors;

import com.intellij.openapi.util.Condition;
import org.kohsuke.rngom.digested.DXmlTokenPattern;

final class NamedPatternFilter implements Condition<DXmlTokenPattern> {
  public static final NamedPatternFilter INSTANCE = new NamedPatternFilter();

  @Override
  public boolean value(DXmlTokenPattern pattern) {
    return !pattern.getName().listNames().isEmpty();
  }
}