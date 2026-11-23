package com.bhavnathacker.pocketreceipts.data.ai

import com.bhavnathacker.pocketreceipts.domain.spending.InterpretedPeriod
import com.bhavnathacker.pocketreceipts.domain.spending.InterpretedQuestion
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SpendingQuestionReplyParserTest {
    private lateinit var underTest: SpendingQuestionReplyParser

    @BeforeEach
    fun setUp() {
        underTest = SpendingQuestionReplyParser()
    }

    @Test
    fun `test that invoke reads a month`() {
        assertThat(underTest("""{"category": "GROCERIES", "period": {"type": "month", "year": 2026, "month": 10}}"""))
            .isEqualTo(InterpretedQuestion.Spending("GROCERIES", InterpretedPeriod.Month(2026, 10)))
    }

    @Test
    fun `test that invoke reads a month with a null year`() {
        assertThat(underTest("""{"category": null, "period": {"type": "month", "year": null, "month": 12}}"""))
            .isEqualTo(InterpretedQuestion.Spending(null, InterpretedPeriod.Month(null, 12)))
    }

    @Test
    fun `test that invoke reads a quarter`() {
        assertThat(underTest("""{"category": "SHOPPING", "period": {"type": "quarter", "year": 2026, "quarter": 4}}"""))
            .isEqualTo(InterpretedQuestion.Spending("SHOPPING", InterpretedPeriod.Quarter(2026, 4)))
    }

    @Test
    fun `test that invoke reads a year`() {
        assertThat(underTest("""{"category": null, "period": {"type": "year", "year": 2025}}"""))
            .isEqualTo(InterpretedQuestion.Spending(null, InterpretedPeriod.Year(2025)))
    }

    @Test
    fun `test that invoke reads a range`() {
        assertThat(underTest("""{"category": null, "period": {"type": "range", "from": "2026-11-01", "to": "2026-11-21"}}"""))
            .isEqualTo(InterpretedQuestion.Spending(null, InterpretedPeriod.Range("2026-11-01", "2026-11-21")))
    }

    @Test
    fun `test that invoke reads all time`() {
        assertThat(underTest("""{"category": "DINING", "period": {"type": "all"}}"""))
            .isEqualTo(InterpretedQuestion.Spending("DINING", InterpretedPeriod.AllTime))
    }

    @Test
    fun `test that invoke reads a period type in any case`() {
        assertThat(underTest("""{"category": null, "period": {"type": " Year ", "year": 2025}}"""))
            .isEqualTo(InterpretedQuestion.Spending(null, InterpretedPeriod.Year(2025)))
    }

    @Test
    fun `test that invoke reads numbers written as strings`() {
        assertThat(underTest("""{"category": null, "period": {"type": "month", "year": "2026", "month": "10"}}"""))
            .isEqualTo(InterpretedQuestion.Spending(null, InterpretedPeriod.Month(2026, 10)))
    }

    @Test
    fun `test that invoke reads the text null as a missing category`() {
        assertThat(underTest("""{"category": "null", "period": {"type": "all"}}"""))
            .isEqualTo(InterpretedQuestion.Spending(null, InterpretedPeriod.AllTime))
    }

    @Test
    fun `test that invoke ignores code fences and text around the object`() {
        val reply = "Here is the query:\n```json\n{\"category\": null, \"period\": {\"type\": \"year\", \"year\": 2025}}\n```"

        assertThat(underTest(reply)).isEqualTo(InterpretedQuestion.Spending(null, InterpretedPeriod.Year(2025)))
    }

    @Test
    fun `test that invoke reads unsupported`() {
        assertThat(underTest("""{"unsupported": true}""")).isEqualTo(InterpretedQuestion.Unsupported)
    }

    @Test
    fun `test that invoke reads a reply with no period as unsupported`() {
        assertThat(underTest("""{"category": "GROCERIES"}""")).isEqualTo(InterpretedQuestion.Unsupported)
    }

    @Test
    fun `test that invoke reads an unknown period type as unsupported`() {
        assertThat(underTest("""{"category": null, "period": {"type": "decade"}}""")).isEqualTo(InterpretedQuestion.Unsupported)
    }

    @Test
    fun `test that invoke keeps a number that isn't whole as missing`() {
        assertThat(underTest("""{"category": null, "period": {"type": "month", "year": 2026, "month": "Oct"}}"""))
            .isEqualTo(InterpretedQuestion.Spending(null, InterpretedPeriod.Month(2026, null)))
    }

    @Test
    fun `test that invoke throws when the reply has no object`() {
        assertThrows<UnreadableReplyException> { underTest("I can't help with that.") }
    }

    @Test
    fun `test that invoke throws when the object can't be read`() {
        assertThrows<UnreadableReplyException> { underTest("""{"category": "GROCERIES", "period": {"type": }""") }
    }
}
