package com.bhavnathacker.pocketreceipts.buildlogic

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ModuleDependencyRulesTest {

    @Test
    fun `test that violations reports a feature depending on a data module`() {
        val violations = ModuleDependencyRules.violations(
            projectPath = ":feature:receipts",
            dependencyPaths = listOf(":data:receipts"),
        )

        assertThat(violations).containsExactly(
            ":feature:receipts must not depend on :data:receipts (features may only depend on :domain and :core:*)",
        )
    }

    @Test
    fun `test that violations reports a feature depending on another feature`() {
        val violations = ModuleDependencyRules.violations(
            projectPath = ":feature:receipts",
            dependencyPaths = listOf(":feature:ask"),
        )

        assertThat(violations).containsExactly(
            ":feature:receipts must not depend on :feature:ask (features may only depend on :domain and :core:*)",
        )
    }

    @Test
    fun `test that violations reports a data module depending on a feature`() {
        val violations = ModuleDependencyRules.violations(
            projectPath = ":data:ai",
            dependencyPaths = listOf(":feature:capture"),
        )

        assertThat(violations).containsExactly(
            ":data:ai must not depend on :feature:capture (data modules must not depend on features)",
        )
    }

    @Test
    fun `test that violations is empty when a feature depends on domain and core modules`() {
        val violations = ModuleDependencyRules.violations(
            projectPath = ":feature:receipts",
            dependencyPaths = listOf(":domain", ":core:ui", ":core:ui-test"),
        )

        assertThat(violations).isEmpty()
    }

    @Test
    fun `test that violations is empty when a data module depends on domain`() {
        val violations = ModuleDependencyRules.violations(
            projectPath = ":data:receipts",
            dependencyPaths = listOf(":domain", ":core:test"),
        )

        assertThat(violations).isEmpty()
    }

    @Test
    fun `test that violations is empty for the app module`() {
        val violations = ModuleDependencyRules.violations(
            projectPath = ":app",
            dependencyPaths = listOf(":feature:receipts", ":data:receipts"),
        )

        assertThat(violations).isEmpty()
    }

    @Test
    fun `test that violations does not report a feature depending on itself`() {
        val violations = ModuleDependencyRules.violations(
            projectPath = ":feature:receipts",
            dependencyPaths = listOf(":feature:receipts"),
        )

        assertThat(violations).isEmpty()
    }
}
