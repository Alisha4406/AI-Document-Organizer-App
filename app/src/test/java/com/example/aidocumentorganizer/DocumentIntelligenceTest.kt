package com.example.aidocumentorganizer

import com.example.aidocumentorganizer.utils.DocumentClassifier
import com.example.aidocumentorganizer.utils.DocumentInsightsEngine
import com.example.aidocumentorganizer.utils.DocumentQuestionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentIntelligenceTest {

    @Test
    fun classifier_recognizes_passport() {
        assertEquals(
            "Personal IDs",
            DocumentClassifier.classify("PASSPORT\nDate of Issue 10 March 2025\nDate of Expiry 10 March 2035")
        )
    }

    @Test
    fun insights_extract_amount_and_date() {
        val result = DocumentInsightsEngine.analyze(
            "Fee Voucher",
            "Education",
            "University Fee Voucher\nTotal Amount: Rs. 45,000\nDue Date: 15 October 2026"
        )
        assertTrue(result.keyFacts.any { it.contains("Rs. 45,000") })
        assertTrue(result.keyFacts.any { it.contains("15 October 2026") })
    }

    @Test
    fun question_engine_answers_amount() {
        val answer = DocumentQuestionEngine.answer(
            "How much do I have to pay?",
            "University Fee Voucher\nTotal Amount: Rs. 45,000",
            "Education",
            "Fee voucher summary"
        )
        assertEquals("Rs. 45,000", answer)
    }
}
