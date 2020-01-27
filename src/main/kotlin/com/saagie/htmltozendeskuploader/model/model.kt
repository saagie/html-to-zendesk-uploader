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
package com.saagie.htmltozendeskuploader.model

import com.google.gson.annotations.Expose
import java.io.File

private const val DEFAULT_LOCALE = "en-us"

data class NewSection(
    val name: String,
    val parentSectionId: Long? = null,
    val locale: String = DEFAULT_LOCALE,
    val position: Int
)

data class ExistingSection(
    val id: Long,
    val name: String,
    val parentSectionId: Long?,
    val locale: String = DEFAULT_LOCALE
)

private val imageRegex = """<img src="((?!https?:\/\/).*?)".*?>""".toRegex()

data class Article(
    @Expose(serialize = false, deserialize = true)
    val id: Long? = null,
    val title: String,
    val parentSectionId: Long,
    val body: String,
    val locale: String = DEFAULT_LOCALE,
    val draft: Boolean = true,
    val permissionGroupId: Long = 567571,
    val userSegmentId: Long? = null,
    @Expose(serialize = false, deserialize = false)
    val path: File
) {

    fun getBodyImages() = imageRegex
        .findAll(body)
        .map { it.groupValues[1] }
        .toList()

    fun replaceImgUrlWithAttachmentUrl(attachments: Map<String, ArticleAttachment>) = copy(
        body = imageRegex.replace(body) {
            it.value.replace(it.groupValues[1], attachments[it.groupValues[1]]?.contentUrl ?: it.groupValues[1])
        }
    )
}

data class ArticleAttachment(
    val id: Long,
    val articleId: Long?,
    val contentUrl: String,
    val inline: Boolean
)
