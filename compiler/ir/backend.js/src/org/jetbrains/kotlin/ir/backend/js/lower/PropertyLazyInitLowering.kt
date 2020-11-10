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
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.name.Name
import kotlin.collections.component1
import kotlin.collections.component2

class PropertyLazyInitLowering2(
    private val context: JsIrBackendContext
) : BodyLoweringPass {

    private val irBuiltIns
        get() = context.irBuiltIns

    private val calculator = JsIrArithBuilder(context)

    private val irFactory
        get() = context.irFactory

    private var IrField.fields by context.mapping.lazyInitialisedFields

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container is IrSimpleFunction) {
            val topLevelProperty = container.correspondingPropertySymbol
                ?.owner
                ?.takeIf { it.isTopLevel }
                ?.takeUnless { it.isConst }
                ?.takeIf { it.backingField?.initializer != null }
                ?: return

            val file = topLevelProperty.parent as? IrFile
                ?: return

            var initFun = context.fileToInitialisationFuns[file]

            if (initFun == null) {
                val fileName = file.name

                val declarations = ArrayList(file.declarations)
                val functions = declarations
                    .flatMap {
                        if (it is IrProperty) {
                            listOf(it.getter, it.setter)
                        } else listOf(it)
                    }
                    .filterIsInstance<IrSimpleFunction>()

                val fieldToInitializer = calculateFieldToExpression(
                    functions
                )

                fieldToInitializer.keys.forEach { it.fields = true }

                val initialisedField = irFactory.createInitialisationField(fileName)
                    .apply {
                        file.declarations.add(this)
                        parent = file
                    }

                initFun = irFactory.addFunction(file) {
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

                context.fileToInitialisationFuns[file] = initFun

                when (irBody) {
                    is IrExpressionBody -> {
                        irBody.expression = JsIrBuilder.buildComposite(
                            type = container.returnType,
                            statements = listOf(
                                JsIrBuilder.buildCall(
                                    target = initFun.symbol,
                                    type = initFun.returnType
                                ),
                                irBody.expression
                            )
                        )
                    }
                    is IrBlockBody -> {
                        irBody.statements.add(
                            0,
                            JsIrBuilder.buildCall(
                                target = initFun.symbol,
                                type = initFun.returnType
                            )
                        )
                    }
                }
            }
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
}

class NullizeDeclarations(
    context: JsIrBackendContext
) : DeclarationTransformer {

    private var IrField.fields by context.mapping.lazyInitialisedFields

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrSimpleFunction) {
            val field = declaration.correspondingPropertySymbol
                ?.owner
                ?.backingField

            if (field != null && field.fields == true) {
                field.initializer = null
            }
        }

        return null
    }
}

private fun calculateFieldToExpression(functions: Collection<IrSimpleFunction>): Map<IrField, IrExpression> =
    functions
        .asSequence()
        .mapNotNull { it.correspondingPropertySymbol }
        .map { it.owner }
        .filter { it.isTopLevel }
        .filterNot { it.isConst }
        .distinct()
        .mapNotNull { it.backingField }
        .filter { it.initializer != null }
        .map { it to it.initializer!!.expression }
        .toMap()

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

private fun IrFactory.bodyWithFunctionCall(
    body: IrBody,
    functionToCall: IrSimpleFunction
): IrBody = createBlockBody(
    body.startOffset,
    body.endOffset,
    mutableListOf<IrStatement>(
        JsIrBuilder.buildCall(
            target = functionToCall.symbol,
            type = functionToCall.returnType
        )
    ).apply { addAll(body.statements) }
)