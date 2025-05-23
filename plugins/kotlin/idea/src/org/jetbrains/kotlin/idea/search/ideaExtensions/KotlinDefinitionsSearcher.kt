// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.idea.search.ideaExtensions

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.search.searches.DefinitionsScopedSearch
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.toFakeLightClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.search.declarationsSearch.forEachImplementation
import org.jetbrains.kotlin.idea.search.declarationsSearch.forEachOverridingMethod
import org.jetbrains.kotlin.idea.search.declarationsSearch.toPossiblyFakeLightMethods
import org.jetbrains.kotlin.idea.util.actualsForExpected
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.contains
import org.jetbrains.kotlin.psi.psiUtil.isExpectDeclaration
import java.util.concurrent.Callable

class KotlinDefinitionsSearcher : QueryExecutor<PsiElement, DefinitionsScopedSearch.SearchParameters> {
    override fun execute(queryParameters: DefinitionsScopedSearch.SearchParameters, consumer: Processor<in PsiElement>): Boolean {
        val processor = skipDelegatedMethodsConsumer(consumer)
        val element = queryParameters.element
        val scope = queryParameters.scope

        return when (element) {
            is KtClass -> {
                val isExpectEnum = runReadAction { element.isEnum() && element.isExpectDeclaration() }
                if (isExpectEnum) {
                    processActualDeclarations(element, processor)
                } else {
                    processClassImplementations(element, processor) && processActualDeclarations(element, processor)
                }
            }

            is KtObjectDeclaration -> {
                processActualDeclarations(element, processor)
            }

            is KtLightClass -> {
                val useScope = runReadAction { element.useScope }
                if (useScope is LocalSearchScope)
                    processLightClassLocalImplementations(element, useScope, processor)
                else
                    true
            }

            is KtNamedFunction, is KtSecondaryConstructor -> {
                processFunctionImplementations(element as KtFunction, scope, processor) && processActualDeclarations(element, processor)
            }

            is KtProperty -> {
                processPropertyImplementations(element, scope, processor) && processActualDeclarations(element, processor)
            }

            is KtParameter -> {
                if (isFieldParameter(element)) {
                    processPropertyImplementations(element, scope, processor) && processActualDeclarations(element, processor)
                } else {
                    true
                }
            }

            else -> true
        }
    }

    companion object {

        private fun skipDelegatedMethodsConsumer(baseConsumer: Processor<in PsiElement>): Processor<PsiElement> = Processor { element ->
            if (isDelegated(element)) {
                return@Processor true
            }

            baseConsumer.process(element)
        }

        private fun isDelegated(element: PsiElement): Boolean = element is KtLightMethod && element.isDelegated

        private fun isFieldParameter(parameter: KtParameter): Boolean = runReadAction {
            KtPsiUtil.getClassIfParameterIsProperty(parameter) != null
        }

        private fun processClassImplementations(klass: KtClass, consumer: Processor<PsiElement>): Boolean {
            val psiClass = runReadAction { klass.toLightClass() ?: klass.toFakeLightClass() }

            val searchScope = runReadAction { psiClass.useScope }
            if (searchScope is LocalSearchScope) {
                return processLightClassLocalImplementations(psiClass, searchScope, consumer)
            }

            return runReadAction { ContainerUtil.process(ClassInheritorsSearch.search(psiClass, true).asIterable(), consumer) }
        }

        private fun processLightClassLocalImplementations(
            psiClass: KtLightClass,
            searchScope: LocalSearchScope,
            consumer: Processor<PsiElement>
        ): Boolean {
            // workaround for IDEA optimization that uses Java PSI traversal to locate inheritors in local search scope
            val globalScope = runReadAction {
                val virtualFiles =searchScope.scope.mapTo(HashSet()) { it.containingFile.virtualFile }
                GlobalSearchScope.filesScope(psiClass.project, virtualFiles)
            }
            return ContainerUtil.process(ClassInheritorsSearch.search(psiClass, globalScope, true).asIterable()) { candidate ->
                val candidateOrigin = candidate.unwrapped ?: candidate
                val inScope = runReadAction { candidateOrigin in searchScope }
                if (inScope) {
                    consumer.process(candidate)
                } else {
                    true
                }
            }
        }

        private fun processFunctionImplementations(
            function: KtFunction,
            scope: SearchScope,
            consumer: Processor<PsiElement>,
        ): Boolean =
            ReadAction.nonBlocking(Callable {
                function.toPossiblyFakeLightMethods().firstOrNull()?.forEachImplementation(scope, consumer::process) ?: true
            }).executeSynchronously()

        private fun processPropertyImplementations(
            declaration: KtNamedDeclaration,
            scope: SearchScope,
            consumer: Processor<PsiElement>
        ): Boolean = runReadAction {
            processPropertyImplementationsMethods(declaration.toPossiblyFakeLightMethods(), scope, consumer)
        }

        private fun processActualDeclarations(declaration: KtDeclaration, consumer: Processor<PsiElement>): Boolean = runReadAction {
            if (!declaration.isExpectDeclaration()) true
            else declaration.actualsForExpected().all(consumer::process)
        }

        fun processPropertyImplementationsMethods(
            accessors: Iterable<PsiMethod>,
            scope: SearchScope,
            consumer: Processor<PsiElement>
        ): Boolean = accessors.all { method ->
            method.forEachOverridingMethod(scope) { implementation ->
                if (isDelegated(implementation)) return@forEachOverridingMethod true

                val elementToProcess = runReadAction {
                    when (val mirrorElement = (implementation as? KtLightMethod)?.kotlinOrigin) {
                        is KtProperty, is KtParameter -> mirrorElement
                        is KtPropertyAccessor -> if (mirrorElement.parent is KtProperty) mirrorElement.parent else implementation
                        else -> implementation
                    }
                }

                consumer.process(elementToProcess)
            }
        }
    }
}
