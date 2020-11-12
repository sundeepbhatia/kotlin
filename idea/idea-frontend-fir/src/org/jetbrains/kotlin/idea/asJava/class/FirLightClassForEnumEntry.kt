/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.idea.asJava.FirLightClassBase
import org.jetbrains.kotlin.idea.asJava.FirLightPsiJavaCodeReferenceElementWithNoReference
import org.jetbrains.kotlin.idea.asJava.applyIf
import org.jetbrains.kotlin.idea.asJava.fields.FirLightFieldForEnumEntry
import org.jetbrains.kotlin.idea.frontend.api.fir.analyzeWithSymbolAsContext
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolVisibility
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.psi.KtClassOrObject

internal class FirLightClassForEnumEntry(
    private val enumEntrySymbol: KtEnumEntrySymbol,
    private val enumConstant: FirLightFieldForEnumEntry,
    manager: PsiManager
) : FirLightClassBase(manager), PsiEnumConstantInitializer {

    override fun getBaseClassType(): PsiClassType = enumConstant.type as PsiClassType //???TODO

    override fun getBaseClassReference(): PsiJavaCodeReferenceElement =
        FirLightPsiJavaCodeReferenceElementWithNoReference(enumConstant) //???TODO

    override fun getArgumentList(): PsiExpressionList? = null

    override fun getEnumConstant(): PsiEnumConstant = enumConstant

    override fun isInQualifiedNew(): Boolean = false

    override fun equals(other: Any?): Boolean =
        other is FirLightClassForEnumEntry &&
                this.enumEntrySymbol == other.enumEntrySymbol

    override fun hashCode(): Int =
        enumEntrySymbol.hashCode()

    override fun copy(): PsiElement =
        FirLightClassForEnumEntry(enumEntrySymbol, enumConstant, manager)

    override fun toString(): String = "FirLightClassForEnumEntry for $name"

    override fun getNameIdentifier(): PsiIdentifier? = null //TODO

    override fun getModifierList(): PsiModifierList? = null //TODO

    override fun hasModifierProperty(name: String): Boolean = false //TODO

    override fun getContainingClass(): PsiClass? = null //TODO

    override fun isDeprecated(): Boolean = false

    override fun getTypeParameters(): Array<PsiTypeParameter> = emptyArray()

    override fun getTypeParameterList(): PsiTypeParameterList? = null

    override fun getQualifiedName(): String? = null

    override fun isInterface(): Boolean = false

    override fun isAnnotationType(): Boolean = false

    override fun isEnum(): Boolean = false

    override fun getExtendsList(): PsiReferenceList? = null //TODO

    override fun getImplementsList(): PsiReferenceList? = null //TODO

    override fun getSuperClass(): PsiClass? = null //TODO

    override fun getInterfaces(): Array<PsiClass> = PsiClass.EMPTY_ARRAY //TODO

    override fun getSupers(): Array<PsiClass> = PsiClass.EMPTY_ARRAY //TODO

    override fun getSuperTypes(): Array<PsiClassType> = PsiClassType.EMPTY_ARRAY

    override fun getParent(): PsiElement? = containingClass ?: containingFile

    override fun getScope(): PsiElement? = parent

    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean = false //TODO

    override fun isInheritorDeep(baseClass: PsiClass?, classToByPass: PsiClass?): Boolean = false //TODO

    override val kotlinOrigin: KtClassOrObject? = null

    override val originKind: LightClassOriginKind = LightClassOriginKind.SOURCE

    override fun getOwnFields(): MutableList<PsiField> = mutableListOf()

    override fun getOwnMethods(): MutableList<KtLightMethod> {
        val result = mutableListOf<KtLightMethod>()

        analyzeWithSymbolAsContext(enumEntrySymbol) {
            val callableSymbols = enumEntrySymbol.getDeclaredMemberScope().getCallableSymbols()
            val visibleDeclarations = callableSymbols.applyIf(isInterface) {
                filterNot { it is KtFunctionSymbol && it.visibility == KtSymbolVisibility.PRIVATE }
            }.applyIf(isEnum) {
                filterNot { function ->
                    function is KtFunctionSymbol && function.name.asString().let { it == "values" || it == "valueOf" }
                }
            }

            createMethods(visibleDeclarations, isTopLevel = false, result)
        }

        return result
    }

    override fun getOwnInnerClasses(): MutableList<PsiClass> = mutableListOf()
}
