/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.codeMetaInfo.CodeMetaInfoRenderer
import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.moduleStructure

class GlobalMetadataInfoHandler(private val testServices: TestServices) : TestService {
    private val infosPerFile: MutableMap<TestFile, MutableList<CodeMetaInfo>> =
        mutableMapOf<TestFile, MutableList<CodeMetaInfo>>().withDefault { mutableListOf() }

    fun addMetadataInfosForFile(file: TestFile, codeMetaInfos: List<CodeMetaInfo>) {
        val infos = infosPerFile.getOrPut(file) { mutableListOf() }
        infos += codeMetaInfos
    }

    fun compareAllMetaDataInfos() {
        // TODO: adapt to multiple testdata files
        val moduleStructure = testServices.moduleStructure
        val allFiles = moduleStructure.modules.flatMap { it.files }
        val builder = StringBuilder()
        for (file in allFiles) {
            CodeMetaInfoRenderer.renderTagsToText(
                builder,
                infosPerFile.getValue(file),
                testServices.sourceFileProvider.getContentOfSourceFile(file)
            )
        }
        val actualText = builder.toString()
        testServices.assertions.assertEqualsToFile(moduleStructure.originalTestDataFiles.single(), actualText)
    }
}

val TestServices.globalMetadataInfoHandler: GlobalMetadataInfoHandler by TestServices.testServiceAccessor()
