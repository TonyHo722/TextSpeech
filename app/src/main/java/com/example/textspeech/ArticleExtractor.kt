package com.example.textspeech

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object ArticleExtractor {
    private const val TAG = "ArticleExtractor"

    /**
     * Extracts readable text from a given URL.
     * Uses hybrid strategy: Jsoup DOM-based cleanup first, with raw string regex fallback for malformed sites.
     */
    suspend fun extractTextFromUrl(url: String): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "Extracting text from: $url")
        try {
            val response = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(15000)
                .execute()

            val document = response.parse()
            val title = document.title()
            Log.d(TAG, "Title: $title")

            // Step 1: Remove all known UI chrome / boilerplate elements by CSS selector
            document.select(
                "script, style, noscript, header, footer, nav, " +
                ".header, .footer, .nav, .sidebar, .ad, .ads, .advertisement, " +
                "#header, #footer, #nav, #sidebar"
            ).remove()

            // For Chinese novel sites: convert <br> to newlines before text extraction
            document.select("br").append("\\n")
            document.select("p").prepend("\\n\\n")

            // Step 2: Find candidate text block using "longest text" heuristic
            var longestText = ""
            val candidateElements = document.select("div, article, main, section, td, body")

            candidateElements.forEach { element ->
                val rawText = element.text()
                    .replace("\\n", "\n")
                    .replace(Regex("\n\\s+"), "\n")
                    .trim()
                if (rawText.length > longestText.length) {
                    longestText = rawText
                }
            }

            Log.d(TAG, "Heuristic extraction length: ${longestText.length}")

            // Step 3: Fallback for severely broken HTML (e.g. unclosed script tags missing body)
            if (longestText.length < 500) {
                Log.d(TAG, "Triggering regex fallback...")
                var rawHtml = document.html()
                rawHtml = rawHtml.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                rawHtml = rawHtml.replace(Regex("<p\\s*/?>", RegexOption.IGNORE_CASE), "\n\n")
                rawHtml = rawHtml.replace(Regex("<[^>]+>"), "")
                longestText = Jsoup.parse(rawHtml).text()
                Log.d(TAG, "Fallback extraction length: ${longestText.length}")
            }

            // Step 4: Post-processing cleanup line by line
            val cleanedLines = longestText.split("\n").filter { line ->
                val t = line.trim()
                when {
                    t.isEmpty() -> false

                    // Remove isolated junk single/double ASCII chars (anti-scraping noise like "A", "B")
                    t.length <= 2 && t.matches(Regex("^[a-zA-Z0-9_-]+$")) -> false

                    // Remove lines that are purely Latin/ASCII font tokens like "fontbigbig font1 font2"
                    t.matches(Regex("^[a-zA-Z0-9\\s_\\-]+$")) && t.length < 60 -> false

                    // Remove section dividers like ************
                    t.matches(Regex("^[*\\-=_~]{3,}$")) -> false

                    // Remove nav/footer/copyright lines that contain ANY of these keywords
                    // regardless of length (the nav bar is one long line on piaotia)
                    containsBoilerplate(t) -> false

                    else -> true
                }
            }

            val sb = StringBuilder()
            sb.append(title.takeIf { it.isNotBlank() } ?: "Extracted Article").append("\n\n")
            sb.append(cleanedLines.joinToString("\n\n"))

            val result = sb.toString().trim()
            Log.d(TAG, "Final extracted length: ${result.length}")
            result

        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract content", e)
            "Failed to extract content: ${e.message}"
        }
    }

    /**
     * Returns true if the line is likely a navigation/footer/copyright boilerplate.
     * Checks for a combination of common Chinese web novel site keywords.
     */
    private fun containsBoilerplate(line: String): Boolean {
        val navKeywords = listOf(
            "飘天文学", "piaotia.com", "返回书架", "加入书架", "加入书签",
            "推荐本书", "收藏本书", "章节目录", "返回目录", "返回顶部",
            "上一章", "下一章", "返回书页", "快捷键", "重要声明",
            "Copyright ©", "All rights reserved", "选择背景颜色", "选择字体大小",
            "繁體中文", "fontbig"
        )
        // A line is boilerplate if it contains 2 or more of these keywords
        val matchCount = navKeywords.count { line.contains(it) }
        return matchCount >= 2
    }
}
