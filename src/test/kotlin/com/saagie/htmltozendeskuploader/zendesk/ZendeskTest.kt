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
package com.saagie.htmltozendeskuploader

import com.saagie.htmltozendeskuploader.model.Article
import com.saagie.htmltozendeskuploader.zendesk.Zendesk
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

internal class ZendeskTest {

    @Rule
    @JvmField
    val server = MockWebServer()

    @Test
    fun `should create article without attachment`() {

        server.enqueue(MockResponse().setBody(ARTICLE_CREATED_RESPONSE).setResponseCode(201))
        server.start()
        val baseUrl = server.url("zendeskMock")

        val anArticle = Article(
            id = 1, title = "toto",
            parentSectionId = 360003533299,
            body = "toto toto",
            path = File("samples/")
        )
        val zendeskApi = Zendesk(
            baseUrl.toString(),
            "user",
            "pass",
            360002232959)

        val result = zendeskApi.createArticle(anArticle)

        assertTrue { result.isRight() }
        server.shutdown()
    }

    companion object {
        const val ARTICLE_CREATED_RESPONSE = "{article: {id:37486578, section_id:98838}}"
    }
}
