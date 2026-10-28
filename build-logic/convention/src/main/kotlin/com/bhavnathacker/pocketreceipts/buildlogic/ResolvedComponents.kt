package com.bhavnathacker.pocketreceipts.buildlogic

import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult

/**
 * Every external module in this resolution graph as `group:name:version`,
 * sorted and without duplicates. Project components are skipped.
 */
internal fun ResolvedComponentResult.moduleComponentIds(): List<String> {
    val ids = sortedSetOf<String>()
    val visited = mutableSetOf<ResolvedComponentResult>()
    val queue = ArrayDeque(listOf(this))
    while (queue.isNotEmpty()) {
        val component = queue.removeFirst()
        if (!visited.add(component)) continue
        (component.id as? ModuleComponentIdentifier)?.let { ids += "${it.group}:${it.module}:${it.version}" }
        component.dependencies
            .filterIsInstance<ResolvedDependencyResult>()
            .forEach { queue += it.selected }
    }
    return ids.toList()
}
