/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.incremental.dumpCollection
import java.io.File

class ComplementarySourceFilesMap(
    storageFile: File,
    private val pathConverter: FileToPathConverter
) : BasicStringMap<Collection<String>>(storageFile, PathStringDescriptor, StringCollectionExternalizer) {
    val LOG = Logger.getInstance(this.javaClass.name)

    private fun debug(msg: String) {
        LOG.info(Thread.currentThread().name + "\t" + msg)
    }

    operator fun set(sourceFile: File, complementaryFiles: Collection<File>) {
        debug("storage[${pathConverter.toPath(sourceFile)}] = ${pathConverter.toPaths(complementaryFiles)}")
        storage[pathConverter.toPath(sourceFile)] = pathConverter.toPaths(complementaryFiles)
    }

    operator fun get(sourceFile: File): Collection<File> {
        debug("storage[${pathConverter.toPath(sourceFile)}].orEmpty()")
        val paths = storage[pathConverter.toPath(sourceFile)].orEmpty()
        return pathConverter.toFiles(paths)
    }

    override fun dumpValue(value: Collection<String>) =
        value.dumpCollection()

    fun remove(file: File): Collection<File> =
        get(file).also {
            debug("storage.remove(${pathConverter.toPath(file)}")
            storage.remove(pathConverter.toPath(file)) }
}