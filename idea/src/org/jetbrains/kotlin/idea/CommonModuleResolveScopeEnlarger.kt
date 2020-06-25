/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.facet.ProjectFacetManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.JdkOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.impl.LibraryScopeCache
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.ResolveScopeEnlarger
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import org.jetbrains.kotlin.idea.caches.project.implementingModules
import org.jetbrains.kotlin.idea.core.isInTestSourceContentKotlinAware
import org.jetbrains.kotlin.idea.facet.KotlinFacetType
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.util.isInSourceContentWithoutInjected
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.jvm.isJvm

class CommonModuleResolveScopeEnlarger : ResolveScopeEnlarger() {
    override fun getAdditionalResolveScope(file: VirtualFile, project: Project): SearchScope? {
        val modulesWithFacet = ProjectFacetManager.getInstance(project).getModulesWithFacet(KotlinFacetType.TYPE_ID)
        if (modulesWithFacet.isEmpty()) return null
        val module = ProjectFileIndex.getInstance(project).getModuleForFile(file) ?: return null
        if (!module.platform.isCommon()) return null

        val moduleFileIndex = ModuleRootManager.getInstance(module).fileIndex
        if (!moduleFileIndex.isInSourceContentWithoutInjected(file) && !moduleFileIndex.isInTestSourceContentKotlinAware(file)) {
            return null
        }

        val implementingModule = module.implementingModules.find { it.platform.isJvm() } ?: return null

        var result = GlobalSearchScope.moduleScope(implementingModule)

        for (entry in ModuleRootManager.getInstance(implementingModule).orderEntries) {
            if (entry is JdkOrderEntry) {
                val scopeForSdk = LibraryScopeCache.getInstance(project).getScopeForSdk(entry)
                result = result.uniteWith(scopeForSdk)
            }
        }
        return result
    }
}