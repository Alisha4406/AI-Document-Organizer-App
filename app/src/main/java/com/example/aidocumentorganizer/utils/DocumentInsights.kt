package com.example.aidocumentorganizer.utils

/**
 * Lightweight, privacy-friendly document intelligence.
 *
 * The engine intentionally runs on-device. It extracts structured signals
 * from OCR instead of pretending that a keyword matcher is a cloud LLM.
 * The same interface can later be backed by an optional local/remote model.
 */
data class DocumentInsights(
    val summary: String,
    val keyFacts: List<String>
)

object DocumentInsightsEngine {

    private val dateRegex = Regex(
        """\b(?:\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\d{1,2}\s+[A-Za-z]{3,9}\s+\d{4}|[A-Za-z]{3,9}\s+\d{1,2},?\s+\d{4})\b"""
    )
    private val moneyRegex = Regex(
        """(?i)(?:rs\.?|pkr|\$|€|£)\s*[0-9][0-9,]*(?:\.[0-9]{1,2})?"""
    )
    private val labeledValueRegex = Regex(
        """(?im)^\s*(name|student|institution|university|company|employee|patient|account|reference|invoice|cnic|passport|license)\s*[:#-]\s*(.+?)\s*$"""
    )

    fun analyze(title: String, category: String, text: String): DocumentInsights {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val preview = lines.take(3).joinToString(" ")
        val description = DocumentClassifier.categoryDescriptions[category]
            ?: "Organized document"

        val summary = when {
            lines.isEmpty() ->
                "$title is organized under $category. No readable OCR text is available yet."
            preview.length <= 190 ->
                "$title is a $category document. $description. Key text: $preview"
            else ->
                "$title is a $category document. $description. Key text: ${preview.take(187)}..."
        }

        val facts = buildList {
            moneyRegex.findAll(text).map { it.value.trim() }.distinct().take(3)
                .forEach { add("Amount: $it") }

            dateRegex.findAll(text).map { it.value.trim() }.distinct().take(4)
                .forEach { add("Date: $it") }

            labeledValueRegex.findAll(text).map {
                "${it.groupValues[1].replaceFirstChar(Char::uppercaseChar)}: ${it.groupValues[2].trim()}"
            }.distinct().take(4).forEach(::add)

            add("Category: $category")
            if (lines.size > 3) add("OCR lines: ${lines.size}")
        }.distinct().take(8)

        return DocumentInsights(summary, facts)
    }
}

object DocumentQuestionEngine {

    private val dateRegex = DocumentInsightsEngine.run {
        Regex("""\b(?:\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\d{1,2}\s+[A-Za-z]{3,9}\s+\d{4}|[A-Za-z]{3,9}\s+\d{1,2},?\s+\d{4})\b""")
    }
    private val moneyRegex = Regex(
        """(?i)(?:rs\.?|pkr|\$|€|£)\s*[0-9][0-9,]*(?:\.[0-9]{1,2})?"""
    )

    fun answer(question: String, text: String, category: String, summary: String): String {
        val q = question.trim().lowercase()
        if (q.isBlank()) return "Ask a question about the document."

        val amounts = moneyRegex.findAll(text).map { it.value.trim() }.distinct().toList()
        val dates = dateRegex.findAll(text).map { it.value.trim() }.distinct().toList()

        return when {
            q.containsAny("amount", "price", "cost", "pay", "fee", "total") ->
                amounts.joinToString(", ").ifBlank { "No monetary amount was found in the OCR text." }

            q.containsAny("date", "when", "deadline", "due", "expiry", "expires") ->
                dates.joinToString(", ").ifBlank { "No date was found in the OCR text." }

            q.containsAny("category", "type", "kind") ->
                "This document is categorized as $category."

            q.containsAny("page", "pages") -> {
                val scannedPages = Regex("(?m)^--- Page \\d+ ---").findAll(text).count()
                    .coerceAtLeast(1)
                "The document contains $scannedPages scanned page(s)."
            }

            q.containsAny("summary", "about", "document") ->
                summary

            else -> {
                // Prefer a concise OCR line containing a meaningful question word.
                val keyword = q.split(Regex("\\W+"))
                    .firstOrNull { it.length >= 4 && it !in STOP_WORDS }
                val matchingLine = keyword?.let { key ->
                    text.lines().firstOrNull { it.contains(key, ignoreCase = true) }
                }
                matchingLine?.trim()?.takeIf { it.isNotBlank() }
                    ?: "I couldn't find a direct answer in the OCR text. Try asking about the amount, dates, category, pages, or summary."
            }
        }
    }

    private fun String.containsAny(vararg values: String): Boolean =
        values.any { contains(it) }

    private val STOP_WORDS = setOf(
        "what", "when", "where", "which", "this", "that", "does", "have",
        "document", "tell", "about", "from", "with", "there", "please"
    )
}
