/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.ConstraintSystemForOverloadResolutionMode
import org.jetbrains.kotlin.config.JvmDefaultMode

object LanguageSettingsDirectives : SimpleDirectivesContainer() {
    val languageFeature = stringDirective(
        name = "LANGUAGE",
        description = """
            List of enabled and disabled language features.
            Usage: // !LANGUAGE: +SomeFeature -OtherFeature warn:FeatureWithEarning
        """.trimIndent()
    )

    @Suppress("RemoveExplicitTypeArguments")
    val apiVersion = valueDirective<ApiVersion>(
        name = "API_VERSION",
        description = "Version of Kotlin API",
        parser = this::parseApiVersion
    )
    // --------------------- Analysis Flags ---------------------

    val useExperimental = stringDirective(
        name = "USE_EXPERIMENTAL",
        description = "List of opted in annotations (AnalysisFlags.useExperimental)"
    )

    val ignoreDataFlowInAssert = directive(
        name = "IGNORE_DATA_FLOW_IN_ASSERT",
        description = "Enables corresponding analysis flag (AnalysisFlags.ignoreDataFlowInAssert)"
    )

    val constraintSystemForOverloadResolution = enumDirective<ConstraintSystemForOverloadResolutionMode>(
        name = "CONSTRAINT_SYSTEM_FOR_OVERLOAD_RESOLUTION",
        description = "Configures corresponding analysis flag (AnalysisFlags.constraintSystemForOverloadResolution)",
    )

    // --------------------- Jvm Analysis Flags ---------------------

    @Suppress("RemoveExplicitTypeArguments")
    val jvmDefaultMode = enumDirective<JvmDefaultMode>(
        name = "JVM_DEFAULT_MODE",
        description = "Configures corresponding analysis flag (JvmAnalysisFlags.jvmDefaultMode)",
        additionalParser = JvmDefaultMode.Companion::fromStringOrNull
    )

    val inheritMultiFileParts = directive(
        name = "INHERIT_MULTIFILE_PARTS",
        description = "Enables corresponding analysis flag (JvmAnalysisFlags.inheritMultifileParts)"
    )

    val sanitizeParenthesis = directive(
        name = "SANITIZE_PARENTHESES",
        description = "Enables corresponding analysis flag (JvmAnalysisFlags.sanitizeParentheses)"
    )

    // --------------------- Utils ---------------------

    fun parseApiVersion(versionString: String): ApiVersion = when (versionString) {
        "LATEST" -> ApiVersion.LATEST
        else -> ApiVersion.parse(versionString) ?: error("Unknown API version: $versionString")
    }
}
