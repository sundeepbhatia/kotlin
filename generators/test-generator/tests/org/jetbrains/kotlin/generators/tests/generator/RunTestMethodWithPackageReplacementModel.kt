/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.generator

import org.jetbrains.kotlin.test.TargetBackend

class RunTestMethodWithPackageReplacementModel(
    val targetBackend: TargetBackend,
    val testMethodName: String,
    val testRunnerMethodName: String,
    val additionalRunnerArguments: List<String>
) : MethodModel {
    object Kind : MethodModel.Kind()

    override val kind: MethodModel.Kind
        get() = Kind

    override val name = METHOD_NAME
    override val dataString: String? = null

    override fun imports(): Collection<Class<*>> {
        return super.imports() + setOf(TargetBackend::class.java)
    }

    companion object {
        const val METHOD_NAME = "runTestWithPackageReplacement"
    }
}
