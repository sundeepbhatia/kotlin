/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.ir.isTopLevel
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.INTERNAL
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrArithBuilder
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.isPure
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.name.Name
import kotlin.collections.component1
import kotlin.collections.component2

class PropertyLazyInitLowering(
    private val context: JsIrBackendContext
) : BodyLoweringPass {

    private val irBuiltIns
        get() = context.irBuiltIns

    private val calculator = JsIrArithBuilder(context)

    private val irFactory
        get() = context.irFactory

    var IrField.initializers by context.mapping.lazyInitialisedFields

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container is IrSimpleFunction || container is IrField) {
            val propertySymbol = when (container) {
                is IrSimpleFunction -> container.correspondingPropertySymbol
                is IrField -> container.correspondingPropertySymbol
                else -> error("Can be only SimpelFunction or IrField")
            }

            val topLevelProperty = propertySymbol
                ?.owner
                ?.takeIf { it.isTopLevel }
                ?.takeUnless { it.isConst }
                ?.takeIf { it.backingField?.initializer != null }
                ?: return

            val file = topLevelProperty.parent as? IrFile
                ?: return

            val initFun = context.fileToInitialisationFuns[file]
                ?: createInitialisationFunction(file)?.also { context.fileToInitialisationFuns[file] = it }
                ?: return

            val initialisationCall = JsIrBuilder.buildCall(
                target = initFun.symbol,
                type = initFun.returnType
            )

            if (container is IrSimpleFunction) {
                irBody.addInitialisation(initialisationCall, container)
            }
            if (container is IrField) {
                container.correspondingPropertySymbol?.owner?.getter
                    ?.let { irBody.addInitialisation(initialisationCall, it) }
                container.correspondingPropertySymbol?.owner?.setter
                    ?.let { irBody.addInitialisation(initialisationCall, it) }
            }
        }
    }

    private fun createInitialisationFunction(
        file: IrFile
    ): IrSimpleFunction? {
        val fileName = file.name

        val declarations = ArrayList(file.declarations)

        val fieldToInitializer = calculateFieldToExpression(
            declarations
        )

        if (fieldToInitializer.all { it.value.isPure(anyVariable = true) }) {
            return null
        }

        val initialisedField = irFactory.createInitialisationField(fileName)
            .apply {
                file.declarations.add(this)
                parent = file
            }

        return irFactory.addFunction(file) {
            name = Name.identifier("init properties $fileName")
            returnType = irBuiltIns.unitType
            visibility = INTERNAL
            origin = JsIrBuilder.SYNTHESIZED_DECLARATION
        }.apply {
            buildPropertiesInitializationBody(
                fieldToInitializer,
                initialisedField
            )
        }
    }

    private fun IrFactory.createInitialisationField(fileName: String): IrField =
        buildField {
            name = Name.identifier("properties initialised $fileName")
            type = irBuiltIns.booleanType
            isStatic = true
            isFinal = true
            origin = JsIrBuilder.SYNTHESIZED_DECLARATION
        }

    private fun IrSimpleFunction.buildPropertiesInitializationBody(
        initializers: Map<IrField, IrExpression>,
        initialisedField: IrField
    ) {
        body = irFactory.createBlockBody(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            buildBodyWithIfGuard(initializers, initialisedField)
        )
    }

    private fun buildBodyWithIfGuard(
        initializers: Map<IrField, IrExpression>,
        initialisedField: IrField
    ): List<IrStatement> {
        val statements = initializers
            .map { (field, expression) ->
                createIrSetField(field, expression)
            }

        val upGuard = createIrSetField(
            initialisedField,
            JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, true)
        )

        return JsIrBuilder.buildIfElse(
            type = irBuiltIns.unitType,
            cond = calculator.not(createIrGetField(initialisedField)),
            thenBranch = JsIrBuilder.buildComposite(
                type = irBuiltIns.unitType,
                statements = mutableListOf(upGuard).apply { addAll(statements) }
            )
        ).let { listOf(it) }
    }

    private fun calculateFieldToExpression(declarations: Collection<IrDeclaration>): Map<IrField, IrExpression> =
        declarations
            .asSequence()
            .map {
                when (it) {
                    is IrProperty -> it
                    is IrSimpleFunction -> {
                        it.correspondingPropertySymbol?.owner
                    }
                    else -> null
                }
            }
            .filterNotNull()
            .filter { it.isTopLevel }
            .filterNot { it.isConst }
            .distinct()
            .mapNotNull { it.backingField }
            .filter { it.initializer != null || it.initializers != null }
            .map { it to (it.initializers ?: it.initializer!!.expression) }
            .toMap()

}

private fun IrBody.addInitialisation(
    initCall: IrCall,
    container: IrSimpleFunction
) {
    when (this) {
        is IrExpressionBody -> {
            expression = JsIrBuilder.buildComposite(
                type = container.returnType,
                statements = listOf(
                    initCall,
                    expression
                )
            )
        }
        is IrBlockBody -> {
            statements.add(
                0,
                initCall
            )
        }
    }
}


private fun createIrGetField(field: IrField): IrGetField {
    return JsIrBuilder.buildGetField(
        symbol = field.symbol,
        receiver = null
    )
}

private fun createIrSetField(field: IrField, expression: IrExpression): IrSetField {
    return JsIrBuilder.buildSetField(
        symbol = field.symbol,
        receiver = null,
        value = expression,
        type = expression.type
    )
}

class RemoveInitializersForLazyProperties(
    context: JsIrBackendContext
) : DeclarationTransformer {

    var IrField.initializers by context.mapping.lazyInitialisedFields

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        val file = declaration.parent as? IrFile ?: return null
        val declarations = ArrayList(file.declarations)

        val fields = declarations
            .asSequence()
            .map {
                when (it) {
                    is IrProperty -> it
                    is IrSimpleFunction -> {
                        it.correspondingPropertySymbol?.owner
                    }
                    else -> null
                }
            }
            .filterNotNull()
            .filter { it.isTopLevel }
            .filterNot { it.isConst }
            .distinct()
            .mapNotNull { it.backingField }
            .filter { it.initializer != null }
            .map { it.initializer!!.expression }

        if (fields.all { it.isPure(anyVariable = true) }) {
            return null
        }
        if (declaration is IrProperty) {
            if (declaration.isTopLevel && !declaration.isConst) {
                declaration.backingField?.initializers = declaration.backingField?.initializer?.expression
                declaration.backingField?.initializer = null
            }
        }
        if (declaration is IrField) {
            val property = declaration.correspondingPropertySymbol?.owner ?: return null
            if (property.isTopLevel && !property.isConst) {
                declaration.initializers = declaration.initializer?.expression
                declaration.initializer = null
            }
        }
        if (declaration is IrSimpleFunction) {

            val field = declaration.correspondingPropertySymbol
                ?.owner
                ?.takeIf { it.isTopLevel }
                ?.takeUnless { it.isConst }
                ?.backingField

            if (field != null) {
                field.initializers = field.initializer?.expression
                field.initializer = null
            }
        }

        return null
    }
}