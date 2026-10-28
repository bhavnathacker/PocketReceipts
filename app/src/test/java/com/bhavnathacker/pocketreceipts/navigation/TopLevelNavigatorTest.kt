package com.bhavnathacker.pocketreceipts.navigation

import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TopLevelNavigatorTest {

    private data object Receipts : NavKey
    private data object Ask : NavKey
    private data object Settings : NavKey
    private data object Detail : NavKey

    private lateinit var backStack: MutableList<NavKey>
    private lateinit var underTest: TopLevelNavigator

    @BeforeEach
    fun setUp() {
        backStack = mutableListOf(Receipts)
        underTest = TopLevelNavigator(
            backStack = backStack,
            startKey = Receipts,
            topLevelKeys = setOf(Receipts, Ask, Settings),
        )
    }

    @Test
    fun `test that selectedKey is the start key initially`() {
        assertThat(underTest.selectedKey).isEqualTo(Receipts)
    }

    @Test
    fun `test that select adds the destination on top of the start key`() {
        underTest.select(Ask)

        assertThat(backStack).containsExactly(Receipts, Ask).inOrder()
        assertThat(underTest.selectedKey).isEqualTo(Ask)
    }

    @Test
    fun `test that select replaces the current top-level destination when another is selected`() {
        underTest.select(Ask)

        underTest.select(Settings)

        assertThat(backStack).containsExactly(Receipts, Settings).inOrder()
        assertThat(underTest.selectedKey).isEqualTo(Settings)
    }

    @Test
    fun `test that select leaves only the start key when the start destination is selected`() {
        underTest.select(Settings)

        underTest.select(Receipts)

        assertThat(backStack).containsExactly(Receipts)
        assertThat(underTest.selectedKey).isEqualTo(Receipts)
    }

    @Test
    fun `test that select does not change the back stack when the selected destination is reselected`() {
        underTest.select(Ask)

        underTest.select(Ask)

        assertThat(backStack).containsExactly(Receipts, Ask).inOrder()
    }

    @Test
    fun `test that select does not change the back stack when the start destination is reselected`() {
        underTest.select(Receipts)

        assertThat(backStack).containsExactly(Receipts)
    }

    @Test
    fun `test that selectedKey is the top-level key nearest the top when a nested screen is shown`() {
        underTest.select(Ask)
        backStack.add(Detail)

        assertThat(underTest.selectedKey).isEqualTo(Ask)
    }

    @Test
    fun `test that select clears nested screens when another destination is selected`() {
        underTest.select(Ask)
        backStack.add(Detail)

        underTest.select(Settings)

        assertThat(backStack).containsExactly(Receipts, Settings).inOrder()
    }
}
