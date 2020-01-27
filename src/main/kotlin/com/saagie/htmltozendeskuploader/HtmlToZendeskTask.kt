package com.saagie.htmltozendeskuploader

/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 Bilal Makhlouf.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import arrow.core.flatMap
import arrow.core.getOrHandle
import arrow.core.handleErrorWith
import arrow.core.rightIfNotNull
import com.saagie.htmltozendeskuploader.model.Article
import com.saagie.htmltozendeskuploader.model.NewSection
import com.saagie.htmltozendeskuploader.zendesk.HtmlToZendeskError
import com.saagie.htmltozendeskuploader.zendesk.HtmlToZendeskError.InvalidFileStructure
import com.saagie.htmltozendeskuploader.zendesk.Zendesk
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTreeElement
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import kotlin.properties.Delegates

open class HtmlToZendeskTask : DefaultTask() {
    @InputDirectory
    lateinit var sourceDir: Any
    @Input
    lateinit var apiBasePath: String
    @Input
    lateinit var user: String
    @Input
    lateinit var password: String
    @get:Input
    var targetCategoryId by Delegates.notNull<Long>()

    private val zendesk: Zendesk by lazy {
        Zendesk(
            url = apiBasePath,
            user = user,
            password = password,
            categoryId = targetCategoryId
        )
    }

    private val errorHandler = { error: HtmlToZendeskError ->
        when (error) {
            is HtmlToZendeskError.ZendeskRequestError.UnexpectedRequestError -> throw RuntimeException(
                """|An error occured : $error
                   |${error.error.response}
                """.trimMargin(),
                error.error
            )
            else -> throw RuntimeException("An error occured : $error")
        }
    }

    @TaskAction
    fun action() {
        val sectionIdMapping = mutableMapOf<String, Long>()
        project.fileTree(sourceDir).visit {
            when {
                isDirectory -> {
                    this.toSection(sectionIdMapping[this.relativePath.parent.pathString])
                        .also {
                            println(
                                "Creating Section '${it.name}' into '${it.parentSectionId ?: "root category"}'"
                            )
                        }
                        .let(zendesk::createSectionOrOverwriteIfExist)
                        .handleErrorWith(errorHandler)
                        .getOrHandle(errorHandler)
                        .also { sectionIdMapping[relativePath.pathString] = it }
                }
                file.extension == "html" ->
                    sectionIdMapping[this.relativePath.parent.pathString]
                        .rightIfNotNull {
                            InvalidFileStructure(
                                "Article $file should be located in a section, not directly under a category"
                            )
                        }
                        .map { parentSectionId -> this.toArticle(parentSectionId) }
                        .flatMap(zendesk::createArticle)
                        .handleErrorWith(errorHandler)
            }
        }
    }

    private fun FileTreeElement.toSection(parentSectionId: Long?) =
        NewSection(
            name = name.substringAfter("-"),
            parentSectionId = parentSectionId,
            position = name.substringBefore("-", "0").toInt()
        )

    private fun FileTreeElement.toArticle(parentSectionId: Long) = Article(
        title = name.removeSuffix(".${file.extension}"),
        body = file.readText(),
        parentSectionId = parentSectionId,
        path = relativePath.getFile(project.file(sourceDir))
    )
}
