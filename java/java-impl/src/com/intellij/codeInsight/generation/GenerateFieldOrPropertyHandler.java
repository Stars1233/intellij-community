// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.codeInsight.generation;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.codeStyle.VariableKind;
import com.intellij.psi.util.PropertyMemberType;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Gregory.Shrago
 */
public class GenerateFieldOrPropertyHandler extends GenerateMembersHandlerBase {
  private final String myAttributeName;
  private final PsiType myType;
  private final PropertyMemberType myMemberType;
  private final PsiAnnotation[] myAnnotations;

  public GenerateFieldOrPropertyHandler(String attributeName, PsiType type, final PropertyMemberType memberType, final PsiAnnotation... annotations) {
    super("");
    myAttributeName = attributeName;
    myType = type;
    myMemberType = memberType;
    myAnnotations = annotations;
  }

  @Override
  protected ClassMember[] chooseOriginalMembers(PsiClass aClass, Project project) {
    return ClassMember.EMPTY_ARRAY;
  }


  @Override
  public @Unmodifiable @NotNull List<? extends GenerationInfo> generateMemberPrototypes(PsiClass aClass, ClassMember[] members) throws IncorrectOperationException {
    PsiElementFactory psiElementFactory = JavaPsiFacade.getElementFactory(aClass.getProject());
    try {
      String fieldName = getFieldName(aClass);
      PsiField psiField = psiElementFactory.createField(fieldName, myType);
      GenerationInfo[] infos = new GenerateGetterAndSetterHandler().generateMemberPrototypes(aClass, new PsiFieldMember(psiField));
      if (myAnnotations.length > 0) {
        PsiMember targetMember = null;
        if (myMemberType == PropertyMemberType.FIELD) {
          targetMember = psiField;
        }
        else {
          for (GenerationInfo info : infos) {
            PsiMember member = info.getPsiMember();
            if (!(member instanceof PsiMethod)) continue;
            if (myMemberType == PropertyMemberType.GETTER && PropertyUtilBase.isSimplePropertyGetter((PsiMethod)member) ||
                myMemberType == PropertyMemberType.SETTER && PropertyUtilBase.isSimplePropertySetter((PsiMethod)member)) {
              targetMember = member;
              break;
            }
          }
          if (targetMember == null) targetMember = findExistingMember(aClass, myMemberType);
        }
        PsiModifierList modifierList = targetMember != null? targetMember.getModifierList() : null;
        if (modifierList != null) {
          for (PsiAnnotation annotation : myAnnotations) {
            PsiAnnotation existing = modifierList.findAnnotation(annotation.getQualifiedName());
            if (existing != null) existing.replace(annotation);
            else modifierList.addAfter(annotation, null);
          }
        }
      }
      return ContainerUtil.concat(Collections.singletonList(new PsiGenerationInfo<>(psiField)), Arrays.asList(infos));
    }
    catch (IncorrectOperationException e) {
      assert false : e;
      return Collections.emptyList();
    }
  }

  public @Nullable PsiMember findExistingMember(@NotNull PsiClass aClass, @NotNull PropertyMemberType memberType) {
    if (memberType == PropertyMemberType.FIELD) {
      return aClass.findFieldByName(getFieldName(aClass), false);
    }
    else if (memberType == PropertyMemberType.GETTER) {
      try {
        PsiElementFactory psiElementFactory = JavaPsiFacade.getElementFactory(aClass.getProject());
        PsiField field = psiElementFactory.createField(myAttributeName, myType);
        PsiMethod[] templates = GetterSetterPrototypeProvider.generateGetterSetters(field, myMemberType == PropertyMemberType.GETTER);
        for (PsiMethod template : templates) {
          PsiMethod existingMethod = aClass.findMethodBySignature(template, true);
          if (existingMethod != null) return existingMethod;
        }
      }
      catch (IncorrectOperationException e) {
        assert false : e;
      }
    }
    return null;
  }

  private String getFieldName(PsiClass aClass) {
    return myMemberType == PropertyMemberType.FIELD? myAttributeName : JavaCodeStyleManager
      .getInstance(aClass.getProject()).propertyNameToVariableName(myAttributeName, VariableKind.FIELD);
  }

  @Override
  protected ClassMember[] getAllOriginalMembers(PsiClass aClass) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected GenerationInfo[] generateMemberPrototypes(PsiClass aClass, ClassMember originalMember) throws IncorrectOperationException {
    throw new UnsupportedOperationException();
  }
}
