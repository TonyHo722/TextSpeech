package com.example.textspeech

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.jsoup.Jsoup
import java.io.File

class ExtractionTest {
    @Test
    fun testPiaotiaExtraction() {
        runBlocking {
            val url = "https://www.piaotia.com/html/10/10104/7107212.html"
            val document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(15000)
                .get()
                
            document.select("script, style, noscript").remove()
            document.select("br").append("\\n")
            document.select("p").prepend("\\n\\n")

            val candidateElements = document.select("div, article, main, section, td")
            for (element in candidateElements) {
                val rawText = element.text().replace("\\n", "\n").replace(Regex("\n\\s+"), "\n").trim()
                val cleanedText = rawText.split("\n")
                    .filter { t -> 
                        val tr = t.trim()
                        if (tr.isEmpty()) false
                        else if (tr.length <= 2 && tr.matches(Regex("^[a-zA-Z0-9_-]+$"))) false
                        else true
                    }.joinToString("\n")
                if (cleanedText.length > 500) {
                     println("Found element <${element.tagName()}> id=${element.id()} class=${element.className()} length=${cleanedText.length}")
                     println(cleanedText.take(100))
                }
            }
        }
    }
}
