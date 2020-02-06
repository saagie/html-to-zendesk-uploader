
# :arrow_up: Upload your sections and articles to Zendesk :arrow_up:  

From a file tree with folders and HTML articles, **upload** them to your ***Zendesk Help Center***.  
 
## **Goal** :star:
  
The goal of this plugin is to **provide a way to upload a whole file tree** containing sections & articles to ***Zendesk***.  
## **How does it work** :construction_worker:

This plugin will take a **root folder**, which may contains other folders or HTML articles, and will upload everything to ***Zendesk***. 
When you upload a root folder, the plugin will **create or overwrite it**. It means that it will search for an existing folder with the name you provide: **if it exists, it will overwrite it**, else it will create it.
When you want to overwrite a folder that have a **generated** name, you can use the `pattern` parameter (description below) to replace the right one.

## **How to use it** :books:

### **Gradle**
Add the following to your `build.gradle`:
```groovy
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "com.saagie:htmltozendeskuploader:0.3.0"
  }
}

apply plugin: "com.saagie.htmltozendeskuploader"
```
Then you can use the task ***HtmlToZendeskTask*** (with the list of parameters below).

### **Registering the task in Kotlin Script**
```kotlin
register<HtmlToZendeskTask>("THE_NAME_YOU_WANT_TO_GIVE") {
  group = TASK_GROUP
  sourceDir = SOURCE_DIR  
  targetCategoryId = ZENDESK_CATEGORY_ROOT  
  apiBasePath = ZENDESK_API_BASE_PATH  
  user = ZENDESK_USERNAME  
  password = ZENDESK_PASSWORD  
  pattern = VERSION_PATTERN 
}
```

### **Parameters of the task**
- **group**: *String*
Gradle group task in which will be the task.
- **sourceDir**: *InputDirectory*
Source directory containing the sections and the HTML files representing the articles. The source directory will be the section root in Zendesk, directly under the category root.
- **targetCategoryId**: *Long*
Zendesk category ID in which will be uploaded all the sections & articles.
- **apiBasePath**: *String*
Your Zendesk API endpoint.
- **user**: *String*
the username of the Zendesk account.
- **password**: *String*
Password of the Zendesk account.
- **pattern (optional)**: *String*
If the name of your root directory contains a generated name (with a version, or a date for example), and you need to replace this version, use a pattern to catch the name you want to replace. 
**Use a capturing group around the search criteria**.
*Example:* 
-- Let's say we have a **root section** called `Zendesk v0.5.1-20200602`. 
-- You want to replace it by `Zendesk v0.5.1-20201503` (which can be a new build of the version) but you **do not have** the complete name on your previous automated build. 
-- You can use a pattern, and **tell the plugin to search for a section to replace** matching the `0.5.1` so it will know it has to replace it. 
-- The pattern would then be `"""v(0.5.1)-"""` (**using a capturing group on the version number**).