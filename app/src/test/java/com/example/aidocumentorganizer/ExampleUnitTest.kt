package com.example.aidocumentorganizer

import com.example.aidocumentorganizer.utils.DocCategories
import com.example.aidocumentorganizer.utils.DocumentClassifier
import com.example.aidocumentorganizer.utils.DocumentInsightsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentClassifierTest {

    @Test
    fun classifiesUniversityDocumentAsEducation() {
        val text = "University semester fee challan student roll no exam"
        assertEquals(DocCategories.EDUCATION, DocumentClassifier.classify(text))
    }

    @Test
    fun classifiesElectricityBill() {
        val text = "Electricity bill amount due consumer meter units consumed payment"
        assertEquals(DocCategories.BILLS, DocumentClassifier.classify(text))
    }

    @Test
    fun blankTextFallsBackToOther() {
        assertEquals(DocCategories.OTHER, DocumentClassifier.classify(""))
    }

    @Test
    fun insightsDetectAmountAndDate() {
        val insights = DocumentInsightsEngine.analyze(
            "Fee Voucher",
            DocCategories.EDUCATION,
            "Total amount: Rs. 45,000\nDue date: 15 October 2026"
        )
        assertTrue(insights.keyFacts.any { it.contains("Rs. 45,000") })
        assertTrue(insights.keyFacts.any { it.contains("15 October 2026") })
    }

    @Test
    fun titleSuggestionUsesReadableFirstLine() {
        val title = DocumentClassifier.suggestTitle("  University Fee Challan 2026\nAmount 45000", DocCategories.EDUCATION)
        assertTrue(title.startsWith("University Fee Challan"))
    }
}
