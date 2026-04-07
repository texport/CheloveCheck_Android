package com.chelovecheck.domain.rollup

import com.chelovecheck.domain.model.CategoryIds
import com.chelovecheck.domain.model.CoicopCategory

/**
 * Maps any COICOP node id to a rollup bucket (preferred level-2 ancestor, else level-1).
 *
 * Analytics uses this as the **maximum depth** for spending: leaf (L3) ids are never shown;
 * they are folded here. Same as [coicopAnalyticsBucketId].
 */
fun rollupIdFor(categoryId: String, categories: Map<String, CoicopCategory>): String {
    if (categoryId == CategoryIds.UNCATEGORIZED) return CategoryIds.UNCATEGORIZED
    val path = mutableListOf<CoicopCategory>()
    var current = categories[categoryId] ?: return CategoryIds.UNCATEGORIZED
    while (true) {
        path.add(current)
        val parentId = current.parentId ?: break
        current = categories[parentId] ?: break
    }
    return path.firstOrNull { it.level == 2 }?.id
        ?: path.firstOrNull { it.level == 1 }?.id
        ?: CategoryIds.UNCATEGORIZED
}

/**
 * COICOP id normalized for analytics: at most **level 2**, otherwise level 1. Leaf (L3) ids are not used.
 */
fun coicopAnalyticsBucketId(categoryId: String, categories: Map<String, CoicopCategory>): String =
    rollupIdFor(categoryId, categories)

/**
 * COICOP id for **analytics spending totals** only: rollup to **level 1** (top COICOP divisions).
 */
/**
 * Walks ancestors to the COICOP **level-1** division id (`01` … `12`), or null if unknown.
 */
fun coicopL1Id(categoryId: String, categories: Map<String, CoicopCategory>): String? {
    if (categoryId == CategoryIds.UNCATEGORIZED) return null
    var current = categories[categoryId] ?: return null
    while (true) {
        if (current.level == 1) return current.id
        val pid = current.parentId ?: return null
        current = categories[pid] ?: return null
    }
}

fun coicopAnalyticsBucketIdL1(categoryId: String, categories: Map<String, CoicopCategory>): String {
    if (categoryId == CategoryIds.UNCATEGORIZED) return CategoryIds.UNCATEGORIZED
    val path = mutableListOf<CoicopCategory>()
    var current = categories[categoryId] ?: return CategoryIds.UNCATEGORIZED
    while (true) {
        path.add(current)
        val parentId = current.parentId ?: break
        current = categories[parentId] ?: break
    }
    return path.firstOrNull { it.level == 1 }?.id ?: CategoryIds.UNCATEGORIZED
}
