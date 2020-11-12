/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava.fields

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.asJava.FirLightClassModifierList
import org.jetbrains.kotlin.idea.asJava.FirLightField
import org.jetbrains.kotlin.idea.asJava.asPsiType
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.idea.util.ifTrue
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry

internal class FirLightFieldForEnumEntry(
    private val enumEntrySymbol: KtEnumEntrySymbol,
    containingClass: KtLightClass,
    override val lightMemberOrigin: LightMemberOrigin?
) : FirLightField(containingClass, lightMemberOrigin), PsiEnumConstant {

    private val _modifierList by lazyPub {
        FirLightClassModifierList(
            containingDeclaration = this@FirLightFieldForEnumEntry,
            modifiers = setOf(PsiModifier.STATIC, PsiModifier.FINAL, PsiModifier.PUBLIC),
            annotations = emptyList()
        )
    }

    override fun getModifierList(): PsiModifierList? = _modifierList

    override val kotlinOrigin: KtDeclaration? = enumEntrySymbol.psi as? KtEnumEntry

    private val _initializingClass: PsiEnumConstantInitializer? by lazyPub {
        enumEntrySymbol.hasInitializer.ifTrue {
            FirLightClassForEnumEntry(
                enumEntrySymbol = enumEntrySymbol,
                enumConstant = this@FirLightFieldForEnumEntry,
                manager = manager
            )
        }
    }

    override fun getInitializingClass(): PsiEnumConstantInitializer? = _initializingClass
    override fun getOrCreateInitializingClass(): PsiEnumConstantInitializer =
        _initializingClass ?: cannotModify()

    override fun getArgumentList(): PsiExpressionList? = null
    override fun resolveMethod(): PsiMethod? = null
    override fun resolveConstructor(): PsiMethod? = null

    override fun resolveMethodGenerics(): JavaResolveResult = JavaResolveResult.EMPTY

    override fun hasInitializer() = true
    override fun computeConstantValue(visitedVars: MutableSet<PsiVariable>?) = this

    override fun getName(): String = enumEntrySymbol.name.asString()

    private val _type: PsiType by lazyPub {
        enumEntrySymbol.type.asPsiType(enumEntrySymbol, this@FirLightFieldForEnumEntry, FirResolvePhase.TYPES)
    }

    override fun getType(): PsiType = _type
    override fun getInitializer(): PsiExpression? = null

    override fun hashCode(): Int = enumEntrySymbol.hashCode()

    override fun equals(other: Any?): Boolean =
        other is FirLightFieldForEnumEntry &&
                enumEntrySymbol == other.enumEntrySymbol
}