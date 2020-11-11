/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

object DiagnosticsDirectives : SimpleDirectivesContainer() {
    val withNewInference = directive(
        name = "WITH_NEW_INFERENCE",
        description = "Enables rendering different diagnostics for old and new inference"
    )

    val diagnostics = stringDirective(
        name = "DIAGNOSTICS",
        description = """
            Enables or disables rendering of specific diagnostics. 
            Syntax:
              Must be '[+-]DIAGNOSTIC_FACTORY_NAME'
              where '+' means 'include'
                    '-' means 'exclude'
              '+' May be used in case if some diagnostic was disabled by default in test runner
                and it should be enabled in specific test
        """.trimIndent()
    )
}
