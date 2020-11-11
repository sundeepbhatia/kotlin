/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.project

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.caches.project.cacheInvalidatingOnRootModifications
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.vfilefinder.KotlinBuiltInsMetadataIndex
import java.util.concurrent.ConcurrentHashMap

interface KotlinStdlibCache {
    fun isStdlib(libraryInfo: LibraryInfo): Boolean

    companion object {
        fun getInstance(project: Project): KotlinStdlibCache =
            ServiceManager.getService(project, KotlinStdlibCache::class.java)
                ?: error("Failed to load service ${KotlinStdlibCache::class.java.name}")
    }
}

class KotlinStdlibCacheImpl(val project: Project) : KotlinStdlibCache {
    private val cache = project.cacheInvalidatingOnRootModifications {
        ConcurrentHashMap<LibraryInfo, Boolean>()
    }

    class LibraryScope(
        project: Project,
        private val directories: Set<VirtualFile>
    ) : DelegatingGlobalSearchScope(GlobalSearchScope.allScope(project)) {
        private val fileSystems = directories.mapTo(hashSetOf(), VirtualFile::getFileSystem)

        override fun contains(file: VirtualFile): Boolean {
            if (file.fileSystem !in fileSystems) return false

            var parent: VirtualFile = file
            while (true) {
                if (parent in directories) return true
                parent = parent.parent ?: return false
            }
        }

        override fun toString() = "All files under: $directories"
    }

    override fun isStdlib(libraryInfo: LibraryInfo): Boolean {
        return cache.getOrPut(libraryInfo) {
            runReadAction {
                FileBasedIndex.getInstance().getContainingFiles(
                    KotlinBuiltInsMetadataIndex.KEY,
                    StandardNames.BUILT_INS_PACKAGE_FQ_NAME,
                    LibraryScope(project, libraryInfo.library.rootProvider.getFiles(OrderRootType.CLASSES).toSet())
                ).isNotEmpty()
            }
        }
    }
}

fun LibraryInfo.isCoreKotlinLibrary(project: Project): Boolean =
    isKotlinStdlib(project) || isKotlinStdlibDependency(project)

fun LibraryInfo.isKotlinStdlib(project: Project): Boolean =
    KotlinStdlibCache.getInstance(project).isStdlib(this)

// TODO: proper condition and caching
fun LibraryInfo.isKotlinStdlibDependency(project: Project): Boolean =
    this.name.asString().split(":").any { it.contains("stdlib-common") }