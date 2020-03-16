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

import com.saagie.htmltozendeskuploader.model.Article
import java.io.File
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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
        const val TRANSLATION_UPDATED_RESPONSE = "{\n" +
            "    \"translation\": {\n" +
            "        \"source_id\": 360012610839,\n" +
            "        \"source_type\": \"Article\",\n" +
            "        \"locale\": \"en-us\",\n" +
            "        \"draft\": false\n" +
            "    }\n" +
            "}"
        const val ARTICLES_RESPONSE =
            "{" +
                "\"count\":0," +
                "\"next_page\":null," +
                "\"page\":1," +
                "\"page_count\":0," +
                "\"per_page\":30," +
                "\"previous_page\":null," +
                "\"articles\":[{id:37486578}, {id:37481477}]," +
                "\"sort_by\":\"position\"," +
                "\"sort_order\":\"asc\"" +
            "}"
        const val FIRST_TRANSLATIONS_RESPONSE =
            "{\n" +
                "    \"translations\": [\n" +
                "        {\n" +
                "            \"id\": 360028516079,\n" +
                "            \"source_id\": 360012610839,\n" +
                "            \"source_type\": \"Article\",\n" +
                "            \"locale\": \"en-us\",\n" +
                "            \"draft\": false\n" +
                "        }\n" +
                "    ],\n" +
                "    \"page\": 1,\n" +
                "    \"previous_page\": null,\n" +
                "    \"next_page\": null,\n" +
                "    \"per_page\": 100,\n" +
                "    \"page_count\": 1,\n" +
                "    \"count\": 1\n" +
                "}"
        const val SECOND_TRANSLATIONS_RESPONSE =
            "{\n" +
                "    \"translations\": [\n" +
                "        {\n" +
                "            \"id\": 360028516080,\n" +
                "            \"source_id\": 360012610839,\n" +
                "            \"source_type\": \"Article\",\n" +
                "            \"locale\": \"en-us\",\n" +
                "            \"draft\": false\n" +
                "        }\n" +
                "    ],\n" +
                "    \"page\": 1,\n" +
                "    \"previous_page\": null,\n" +
                "    \"next_page\": null,\n" +
                "    \"per_page\": 100,\n" +
                "    \"page_count\": 1,\n" +
                "    \"count\": 1\n" +
                "}"
    }
}
