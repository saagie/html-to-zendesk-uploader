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
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.10.1"
    id("io.gitlab.arturbosch.detekt") version "1.3.1"
    id("org.jmailen.kotlinter") version "2.1.3"
    id("org.kordamp.gradle.project") version "0.31.2"
    kotlin("jvm") version "1.3.61"
}

version = "0.0.1"
group = "com.saagie"

config {
    info {
        name = "htmltozendeskuploader"
        description = "Saagie gradle plugin to upload HTML documentation to Zendesk"
        inceptionYear = "2020"
        vendor = "Saagie"

        links {
            website = "https://www.saagie.com"
            scm = "https://github.com/saagie/html-to-zendesk-uploader"
        }

        licensing {
            licenses {
                license {
                    id = "Apache-2.0"
                }
            }
        }

        organization {
            name = "Saagie"
            url = "http://www.saagie.com"
        }

        people {
            person {
                id = "bilal"
                name = "Bilal Makhlouf"
                email = "bilal@saagie.com"
                roles = listOf("author", "developer")
            }
            person {
                id = "pierre"
                name = "Pierre Leresteux"
                email = "pierre@saagie.com"
                roles = listOf("developer")
            }
        }
    }
}

object VersionInfo {
    const val kotlin = "1.3.61"
    const val arrow = "0.10.4"
    const val fuel = "2.2.1"
    const val gson = "2.8.5"
}

val versions: VersionInfo by extra { VersionInfo }
val github = "https://github.com/saagie/html-to-zendesk-uploader"
val packageName = "com.saagie.htmltozendeskuploader"

repositories {
    jcenter()
    gradlePluginPortal()
}


dependencies {
    implementation(gradleApi())
    implementation(kotlin("stdlib-jdk8", version = versions.kotlin))
    implementation("com.google.code.gson:gson:${versions.gson}")
    implementation("com.github.kittinunf.fuel:fuel:${versions.fuel}")
    implementation("com.github.kittinunf.fuel:fuel-gson:${versions.fuel}")
    api("io.arrow-kt:arrow-syntax:${versions.arrow}")
    api("io.arrow-kt:arrow-core:${versions.arrow}")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.apiVersion = "1.3"
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}


detekt {
    input = files("src/main/kotlin", "src/test/kotlin")
    baseline = project.rootDir.resolve("detekt-baseline.xml")
}

kotlinter {
    ignoreFailures = false
    reporters = arrayOf("html")
    experimentalRules = false
    disabledRules = arrayOf("import-ordering","no-wildcard-imports")
}

gradlePlugin {
    plugins {
        create(project.name) {
            id = packageName
            displayName = "Saagie html-to-zendesk-uploader Plugin"
            description = "Saagie html-to-zendesk-uploader Plugin for Gradle"
            implementationClass = "$packageName.HtmlToZendeskTask"
        }
    }
}

pluginBundle {
    website = github
    vcsUrl = github
    tags = listOf("saagie", "html-to-zendesk-uploader", "zendesk", "html")
    mavenCoordinates {
        groupId = project.group.toString()
        artifactId = project.name
    }
}
