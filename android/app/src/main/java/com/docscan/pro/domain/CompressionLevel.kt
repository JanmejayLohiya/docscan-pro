package com.docscan.pro.domain

/**
 * Output size/quality levels for a PDF. Compression shrinks the file mainly by
 * capping page resolution ([maxEdge]) and re-encoding pages at [quality]. FR-4.7
 */
enum class CompressionLevel(
    val label: String,
    val description: String,
    val maxEdge: Int,
    val quality: Int,
) {
    HIGH("High quality", "Largest file, best detail", 2400, 92),
    BALANCED("Balanced", "Good quality, smaller file", 1600, 80),
    SMALL("Small size", "Smallest file, lower detail", 1200, 65),
}
