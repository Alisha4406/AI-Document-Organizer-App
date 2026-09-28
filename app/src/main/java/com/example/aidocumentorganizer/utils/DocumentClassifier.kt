package com.example.aidocumentorganizer.utils

// Category labels are stored in the database as plain strings, so they
// live here as constants shared by the classifier, the DB migration and
// the UI (see Ui/components/DocCategory.kt).
object DocCategories {
    const val EDUCATION = "Education"
    const val BILLS = "Bills"
    const val IDS = "Personal IDs"
    const val MEDICAL = "Medical"
    const val WORK = "Work"
    const val OTHER = "Other"

    val all = listOf(EDUCATION, BILLS, IDS, MEDICAL, WORK, OTHER)
}

/**
 * Rule-based document classification.
 *
 * Runs fully offline on the OCR text. It scores every category by how many
 * of its keywords appear and picks the best one. If you later add an LLM,
 * this is the single place to swap in: keep the same function signatures.
 */
object DocumentClassifier {

    private const val MIN_SCORE = 2

    /** Human-readable labels used by search, chips and AI summaries. */
    val categoryDescriptions: Map<String, String> = mapOf(
        DocCategories.EDUCATION to "Study, university and academic records",
        DocCategories.BILLS to "Invoices, utilities, receipts and payments",
        DocCategories.IDS to "Identity cards, passports and licences",
        DocCategories.MEDICAL to "Prescriptions, reports and healthcare records",
        DocCategories.WORK to "Employment, salary and professional documents",
        DocCategories.OTHER to "Documents that do not match a known category"
    )

    private val keywords: Map<String, List<String>> = mapOf(
        DocCategories.EDUCATION to listOf(
            "university", "college", "school", "semester", "fee challan",
            "challan", "admission", "student", "roll no", "assignment",
            "exam", "degree", "transcript", "course", "marks", "syllabus",
            "campus", "faculty", "lecture"
        ),
        DocCategories.BILLS to listOf(
            "bill", "invoice", "electricity", "utility", "amount due",
            "payable", "due date", "receipt", "total amount", "consumer",
            "meter", "units consumed", "payment", "tax", "subtotal"
        ),
        DocCategories.IDS to listOf(
            "national identity", "cnic", "passport", "driving licen",
            "identity card", "date of birth", "father name", "id card",
            "nadra", "citizen", "nationality", "date of issue",
            "date of expiry", "license no"
        ),
        DocCategories.MEDICAL to listOf(
            "hospital", "prescription", "patient", "doctor", "clinic",
            "diagnosis", "medicine", "lab report", "pharmacy", "dosage",
            "blood", "symptoms", "physician", "medical"
        ),
        DocCategories.WORK to listOf(
            "employee", "salary", "contract", "offer letter", "company",
            "resume", "curriculum vitae", "experience letter", "department",
            "designation", "employment", "payslip", "meeting", "project"
        )
    )

    fun classify(text: String): String {

        val lower = text.lowercase()

        if (lower.isBlank()) return DocCategories.OTHER

        var bestCategory = DocCategories.OTHER
        var bestScore = 0

        for ((category, words) in keywords) {

            var score = 0

            for (word in words) {
                // Cap each keyword at 3 hits so one repeated word
                // can't outweigh several different matches.
                score += minOf(countOccurrences(lower, word), 3)
            }

            if (score > bestScore) {
                bestScore = score
                bestCategory = category
            }
        }

        // A single high-confidence phrase can be enough (for example
        // "passport" or "prescription"), while weak matches remain Other.
        return if (bestScore >= MIN_SCORE || bestScore == 1 && lower.length < 80) {
            bestCategory
        } else {
            DocCategories.OTHER
        }
    }

    /** First readable line of the scan, trimmed - a sensible default name. */
    fun suggestTitle(text: String, category: String): String {

        val firstLine = text
            .lines()
            .map { it.trim() }
            .firstOrNull { line ->
                line.length >= 4 && line.any { it.isLetter() }
            }

        val cleaned = firstLine?.take(40)?.trim()

        return if (!cleaned.isNullOrBlank()) cleaned else "$category document"
    }

    private fun countOccurrences(text: String, word: String): Int {
        var count = 0
        var index = text.indexOf(word)
        while (index >= 0) {
            count++
            index = text.indexOf(word, index + word.length)
        }
        return count
    }
}
