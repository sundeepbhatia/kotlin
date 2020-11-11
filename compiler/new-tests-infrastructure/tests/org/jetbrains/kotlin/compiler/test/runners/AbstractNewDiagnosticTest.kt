/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.test.runners

import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.classic.handlers.DeclarationsDumpHandler
import org.jetbrains.kotlin.test.frontend.classic.handlers.OldNewInferenceMetaInfoProcessor
import org.jetbrains.kotlin.test.model.BackendKind
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKind
import org.jetbrains.kotlin.test.services.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator

abstract class AbstractNewDiagnosticTest : AbstractKotlinTest() {
    override val configuration: TestConfigurationBuilder.() -> Unit = {
        globalDefaults {
            frontend = FrontendKind.ClassicFrontend
            backend = BackendKind.NoBackend
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Source
        }

        assertions = JUnit5Assertions

        useConfigurators(::JvmEnvironmentConfigurator)
        useMetaInfoProcessors(::OldNewInferenceMetaInfoProcessor)
        useAdditionalSourceProviders(::AdditionalDiagnosticsSourceFilesProvider)

        useFrontendFacades(::ClassicFrontendFacade)
        useFrontendHandlers(
            ::DeclarationsDumpHandler,
            ::ClassicDiagnosticsHandler
        )
    }
}
