/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.generator.generators.impl

import org.jetbrains.kotlin.generators.tests.generator.MethodModel
import org.jetbrains.kotlin.generators.tests.generator.RunTestMethodWithPackageReplacementModel
import org.jetbrains.kotlin.generators.tests.generator.generators.MethodGenerator
import org.jetbrains.kotlin.generators.util.CoroutinesTestMethodModel
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.utils.Printer

object CoroutinesTestMethodGenerator : MethodGenerator<CoroutinesTestMethodModel>() {
    override val kind: MethodModel.Kind
        get() = CoroutinesTestMethodModel.Kind

    override fun generateSignature(method: CoroutinesTestMethodModel, p: Printer) {
        generateDefaultSignature(method, p)
    }

    override fun generateBody(method: CoroutinesTestMethodModel, p: Printer) {
        with(method) {
            val filePath = KotlinTestUtils.getFilePath(file) + if (file.isDirectory) "/" else ""
            val packageName = if (isLanguageVersion1_3) "kotlin.coroutines" else "kotlin.coroutines.experimental"

            p.println(RunTestMethodWithPackageReplacementModel.METHOD_NAME, "(\"$filePath\", \"$packageName\");")
        }
    }
}
