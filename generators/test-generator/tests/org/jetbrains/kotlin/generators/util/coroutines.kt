/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.util

import org.jetbrains.kotlin.generators.tests.generator.MethodModel
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File
import java.util.regex.Pattern

fun isCommonCoroutineTest(file: File): Boolean {
    return InTextDirectivesUtils.isDirectiveDefined(file.readText(), "COMMON_COROUTINES_TEST")
}

fun createCommonCoroutinesTestMethodModels(
    rootDir: File,
    file: File,
    filenamePattern: Pattern,
    checkFilenameStartsLowerCase: Boolean?,
    targetBackend: TargetBackend,
    skipIgnored: Boolean,
    skipExperimental: Boolean
): Collection<MethodModel> {
    return if (targetBackend.isIR || targetBackend == TargetBackend.JS)
        listOf(
            CoroutinesTestMethodModel(
                rootDir,
                file,
                filenamePattern,
                checkFilenameStartsLowerCase,
                targetBackend,
                skipIgnored,
                true
            )
        )
    else {
        mutableListOf(
            CoroutinesTestMethodModel(
                rootDir,
                file,
                filenamePattern,
                checkFilenameStartsLowerCase,
                targetBackend,
                skipIgnored,
                true
            )
        ).apply {
            if (!skipExperimental) {
                this += CoroutinesTestMethodModel(
                    rootDir,
                    file,
                    filenamePattern,
                    checkFilenameStartsLowerCase,
                    targetBackend,
                    skipIgnored,
                    false
                )

            }
        }
    }
}
