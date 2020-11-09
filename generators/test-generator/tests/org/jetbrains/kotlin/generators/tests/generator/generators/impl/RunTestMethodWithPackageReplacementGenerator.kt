/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.generator.generators.impl

import org.jetbrains.kotlin.generators.tests.generator.MethodModel
import org.jetbrains.kotlin.generators.tests.generator.RunTestMethodWithPackageReplacementModel
import org.jetbrains.kotlin.generators.tests.generator.generators.MethodGenerator
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.utils.Printer

object RunTestMethodWithPackageReplacementGenerator : MethodGenerator<RunTestMethodWithPackageReplacementModel>() {
    override val kind: MethodModel.Kind
        get() = RunTestMethodWithPackageReplacementModel.Kind

    override fun generateBody(method: RunTestMethodWithPackageReplacementModel, p: Printer) {
        with(method) {
            val className = TargetBackend::class.java.simpleName
            val additionalArguments = if (additionalRunnerArguments.isNotEmpty())
                additionalRunnerArguments.joinToString(separator = ", ", prefix = ", ")
            else ""
            p.println("KotlinTestUtils.$testRunnerMethodName(filePath -> $testMethodName(filePath, packageName), $className.$targetBackend, testDataFilePath$additionalArguments);")
        }
    }

    override fun generateSignature(method: RunTestMethodWithPackageReplacementModel, p: Printer) {
        with(method) {
            p.print("private void $name(String testDataFilePath, String packageName) throws Exception")
        }
    }
}
