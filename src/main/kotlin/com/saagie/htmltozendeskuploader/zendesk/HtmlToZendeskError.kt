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
package zendesk

import com.github.kittinunf.fuel.core.FuelError

sealed class HtmlToZendeskError {
    sealed class ZendeskRequestError : HtmlToZendeskError() {
        object ResourceDoesNotExist : ZendeskRequestError()
        data class UnexpectedRequestError(val error: FuelError) : ZendeskRequestError()
        data class UnexpectedRequestResult(val error: String) : ZendeskRequestError()
    }
    data class InvalidFileStructure(val message: String) : HtmlToZendeskError()
}