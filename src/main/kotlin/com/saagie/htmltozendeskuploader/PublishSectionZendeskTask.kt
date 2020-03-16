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
import com.saagie.htmltozendeskuploader.zendesk.HtmlToZendeskError
import com.saagie.htmltozendeskuploader.zendesk.Zendesk
import kotlin.properties.Delegates
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class PublishSectionZendeskTask : DefaultTask() {
    @Input
    lateinit var apiBasePath: String

    @Input
    lateinit var user: String

    @Input
    lateinit var password: String

    @Input
    lateinit var sectionName: String

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
        zendesk.getSection(sectionName)
            .flatMap { section ->
                zendesk.publishSection(section)
            }
            .handleErrorWith(errorHandler)
            .getOrHandle(errorHandler)
    }
}
