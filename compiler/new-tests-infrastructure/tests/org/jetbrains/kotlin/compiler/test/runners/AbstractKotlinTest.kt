/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.test.runners

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.testRunner

abstract class AbstractKotlinTest {
    abstract val configuration: TestConfigurationBuilder.() -> Unit

    fun runTest(@TestDataFile filePath: String) {
        testRunner(configuration).runTest(filePath)
    }
}
