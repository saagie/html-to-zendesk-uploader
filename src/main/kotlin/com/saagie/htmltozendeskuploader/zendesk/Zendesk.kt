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
package com.saagie.htmltozendeskuploader.zendesk

import arrow.core.Either
import arrow.core.Tuple2
import arrow.core.extensions.either.applicative.applicative
import arrow.core.extensions.list.traverse.sequence
import arrow.core.extensions.tuple2.traverse.sequence
import arrow.core.firstOrNone
import arrow.core.fix
import arrow.core.flatMap
import arrow.core.handleErrorWith
import arrow.core.left
import arrow.core.right
import arrow.core.rightIfNotNull
import arrow.core.toOption
import arrow.core.toT
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.InlineDataPart
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.Method.DELETE
import com.github.kittinunf.fuel.core.Method.GET
import com.github.kittinunf.fuel.core.Method.POST
import com.github.kittinunf.fuel.core.Method.PUT
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.UploadRequest
import com.github.kittinunf.fuel.core.requests.upload
import com.saagie.htmltozendeskuploader.model.Article
import com.saagie.htmltozendeskuploader.model.ArticleAttachment
import com.saagie.htmltozendeskuploader.model.ExistingSection
import com.saagie.htmltozendeskuploader.model.NewSection
import com.saagie.htmltozendeskuploader.model.Translation
import com.saagie.htmltozendeskuploader.zendesk.HtmlToZendeskError.ZendeskRequestError.ResourceDoesNotExist
import com.saagie.htmltozendeskuploader.zendesk.HtmlToZendeskError.ZendeskRequestError.UnexpectedRequestError
import com.saagie.htmltozendeskuploader.zendesk.HtmlToZendeskError.ZendeskRequestError.UnexpectedRequestResult
import com.saagie.htmltozendeskuploader.zendesk.ZendeskRequest.DeleteSection
import java.io.File
import kotlin.reflect.KClass

sealed class ZendeskApiBody {
    data class SectionsBody(val sections: List<ExistingSection>) : ZendeskApiBody()
    data class NewSectionBody(val section: NewSection) : ZendeskApiBody()
    data class ExistingSectionBody(val section: ExistingSection) : ZendeskApiBody()
    data class ArticleBody(val article: Article) : ZendeskApiBody()
    data class ArticlesBody(val articles: List<Article>) : ZendeskApiBody()
    data class TranslationBody(val translation: Translation) : ZendeskApiBody()
    data class TranslationsBody(val translations: List<Translation>) : ZendeskApiBody()
    data class AttachmentIdsBody(val attachmentIds: List<Long>) : ZendeskApiBody()
    data class AttachmentBody(val articleAttachment: ArticleAttachment) : ZendeskApiBody()

    object EmptyBody : ZendeskApiBody()
}

sealed class ZendeskRequest<out T : ZendeskApiBody>(
    val method: Method,
    val path: String,
    val responseType: KClass<out T>,
    open val body: ZendeskApiBody = ZendeskApiBody.EmptyBody
) {

    open fun get(basePath: String) = Fuel.request(method, basePath + path, null)
        .apply {
            this@ZendeskRequest.body.toOption().map { jsonBody(gson.toJson(it)) }
        }

    data class GetSections(val categoryId: Long) : ZendeskRequest<ZendeskApiBody.SectionsBody>(
        GET, "/categories/$categoryId/sections.json", ZendeskApiBody.SectionsBody::class
    )

    data class CreateSection(val categoryId: Long, val section: NewSection) :
        ZendeskRequest<ZendeskApiBody.ExistingSectionBody>(
            POST,
            "/categories/$categoryId/sections.json",
            ZendeskApiBody.ExistingSectionBody::class,
            ZendeskApiBody.NewSectionBody(section)
        )

    data class DeleteSection(val sectionId: Long) : ZendeskRequest<ZendeskApiBody.EmptyBody>(
        DELETE,
        "/sections/$sectionId.json",
        ZendeskApiBody.EmptyBody::class
    )

    data class CreateArticle(val article: Article) : ZendeskRequest<ZendeskApiBody.ArticleBody>(
        POST,
        "/sections/${article.parentSectionId}/articles.json",
        ZendeskApiBody.ArticleBody::class,
        ZendeskApiBody.ArticleBody(article)
    )

    data class GetArticles(val sectionId: Long) : ZendeskRequest<ZendeskApiBody.ArticlesBody>(
        GET,
        "/sections/$sectionId/articles.json",
        ZendeskApiBody.ArticlesBody::class
    )

    data class UploadAttachedImage(val filePath: String) : ZendeskRequest<ZendeskApiBody.AttachmentBody>(
        POST,
        "/articles/attachments.json",
        ZendeskApiBody.AttachmentBody::class
    ) {
        override fun get(basePath: String): UploadRequest = super.get(basePath).upload()
            .add(FileDataPart(name = "file", file = File(filePath)))
            .add(InlineDataPart(name = "inline", content = "true"))
    }

    data class LinkAttachedImage(val articleId: Long, val attachmentIds: List<Long>) :
        ZendeskRequest<ZendeskApiBody.EmptyBody>(
            POST,
            "/articles/$articleId/bulk_attachments.json",
            ZendeskApiBody.EmptyBody::class,
            ZendeskApiBody.AttachmentIdsBody(attachmentIds)
        )

    data class GetArticleTranslations(val articleId: Long, val locale: String) :
        ZendeskRequest<ZendeskApiBody.TranslationsBody>(
            GET,
            "/articles/$articleId/translations.json",
            ZendeskApiBody.TranslationsBody::class
        )

    data class UpdateArticleTranslation(val translation: Translation) :
        ZendeskRequest<ZendeskApiBody.EmptyBody>(
            PUT,
            "/articles/${translation.sourceId}/translations/${translation.locale}.json",
            ZendeskApiBody.EmptyBody::class,
            ZendeskApiBody.TranslationBody(translation)
        )
}

class Zendesk(
    val url: String,
    val user: String,
    val password: String,
    val categoryId: Long,
    val pattern: String? = null
) {

    fun createSectionOrOverwriteIfExist(section: NewSection) =
        (pattern?.let {
            getSectionWithPattern(it, section.parentSectionId)
        } ?: getSection(section.name, section.parentSectionId))
            .flatMap {
                DeleteSection(it.id)
                    .run()
                    .handleErrorWith {
                        when (it) {
                            is ResourceDoesNotExist -> Unit.right()
                            else -> it.left()
                        }
                    }
                    .also { println("Section for version ${section.name} already exists. Deleting it.") }
                    .map { section }
            }
            .handleErrorWith {
                when (it) {
                    is ResourceDoesNotExist -> section.right()
                    else -> it.left()
                }
            }.flatMap { createSection(it) }
            .map { it.id }

    fun createArticle(article: Article) =
        uploadImages(article)
            .map { attachmentsMapping ->
                attachmentsMapping toT article.replaceImgUrlWithAttachmentUrl(attachmentsMapping)
            }
            .flatmapTupleRight(::postArticle)
            .flatMap { (attachments, articleId) ->
                if (attachments.isNotEmpty())
                    linkAttachmentsToArticle(attachments.values, articleId)
                else
                    Either.right(article)
            }

    fun publishSection(section: ExistingSection) =
        getArticles(section.id)
            .flatMap { articles ->
                articles.map {
                    publishArticle(it)
                }.sequence(Either.applicative()).fix()
            }
            .map { Unit }

    private fun getArticleTranslations(articleId: Long) =
        ZendeskRequest.GetArticleTranslations(articleId, "en-us").run()
            .map {
                it.translations
            }

    private fun getArticles(sectionId: Long) =
        ZendeskRequest.GetArticles(sectionId).run()
            .map {
                it.articles
            }

    private fun updateTranslation(translation: Translation) =
        ZendeskRequest.UpdateArticleTranslation(translation).run()

    private fun publishArticle(article: Article) =
        article.id.rightIfNotNull { HtmlToZendeskError.MissingArticleId }
            .flatMap { getArticleTranslations(it) }
            .flatMap { translations ->
                translations.map { translation ->
                    updateTranslation(translation.copy(draft = false))
                }.sequence(Either.applicative()).fix()
            }
            .map { Unit }

    private fun uploadArticleImage(path: String) =
        ZendeskRequest.UploadAttachedImage(path).run()
            .map { it.articleAttachment }

    private fun uploadImages(article: Article) =
        article.getBodyImages()
            .map { it to uploadArticleImage("${article.path.parent}/$it") }
            .map { (imgName, uploadResult) ->
                uploadResult.fold({
                    it.left()
                }, {
                    (imgName to it).right()
                })
            }
            .sequence(Either.applicative()).fix().map { it.fix().toMap() }

    private fun linkAttachmentsToArticle(attachments: Collection<ArticleAttachment>, articleId: Long) =
        ZendeskRequest.LinkAttachedImage(articleId, attachments.map(ArticleAttachment::id)).run()

    private fun postArticle(article: Article) =
        ZendeskRequest.CreateArticle(article).run()
            .flatMap {
                it.article.id.rightIfNotNull {
                    UnexpectedRequestResult("The id of the article that has been created is not set. $it ")
                }
            }

    fun getSection(name: String, parentSectionId: Long? = null) =
        ZendeskRequest.GetSections(categoryId).run()
            .flatMap {
                it.sections.firstOrNone { it.name == name && it.parentSectionId == parentSectionId }
                    .toEither { ResourceDoesNotExist }
            }

    private fun getSectionWithPattern(pattern: String, parentSectionId: Long? = null) =
        ZendeskRequest.GetSections(categoryId).run()
            .flatMap {
                it.sections.firstOrNone { section ->
                    pattern.toRegex().containsMatchIn(section.name) && section.parentSectionId == parentSectionId
                }.toEither { ResourceDoesNotExist }
            }

    private fun createSection(section: NewSection) =
        ZendeskRequest.CreateSection(categoryId, section)
            .run()
            .map { it.section }

    class ZendeskResponseDeserializable2<T : ZendeskApiBody>(val responseType: KClass<out T>) :
        ResponseDeserializable<T> {
        override fun deserialize(content: String): T = when (responseType) {
            ZendeskApiBody.EmptyBody::class -> ZendeskApiBody.EmptyBody as T
            else -> gson.fromJson(content, responseType.java)
        }
    }

    private fun <T : ZendeskApiBody> ZendeskRequest<T>.run(requestConfigBlock: Request.() -> Unit = {}) =
        get(url)
            .apply(requestConfigBlock)
            .also { println(it) }
            .authentication()
            .basic(user, password)
            .responseObject(ZendeskResponseDeserializable2(responseType))
            .third
            .fold(
                { it.right() },
                {
                    when (it.response.statusCode) {
                        404 -> ResourceDoesNotExist.left()
                        else -> UnexpectedRequestError(it).left()
                    }
                })
}

private fun <A, B, C, D> Either<A, Tuple2<B, C>>.flatmapTupleRight(block: (C) -> Either<A, D>) = flatMap {
    it.map(block).sequence(Either.applicative()).fix().map { it.fix() }
}
