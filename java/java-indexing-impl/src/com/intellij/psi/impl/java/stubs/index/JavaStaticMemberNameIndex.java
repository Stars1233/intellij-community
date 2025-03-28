// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.psi.impl.java.stubs.index;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMember;
import com.intellij.psi.impl.search.JavaSourceFilterScope;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public final class JavaStaticMemberNameIndex extends StringStubIndexExtension<PsiMember> {
  private static final JavaStaticMemberNameIndex ourInstance = new JavaStaticMemberNameIndex();

  public static JavaStaticMemberNameIndex getInstance() {
    return ourInstance;
  }

  @Override
  public @NotNull StubIndexKey<String, PsiMember> getKey() {
    return JavaStubIndexKeys.JVM_STATIC_MEMBERS_NAMES;
  }

  public Collection<PsiMember> getStaticMembers(final String name, final Project project, final @NotNull GlobalSearchScope scope) {
    return StubIndex.getElements(getKey(), name, project, new JavaSourceFilterScope(scope), PsiMember.class);
  }
}