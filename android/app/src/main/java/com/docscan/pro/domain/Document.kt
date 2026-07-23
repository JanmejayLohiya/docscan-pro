package com.docscan.pro.domain

/** Presentation/domain view of a scanned document. */
data class Document(
    val id: String,
    val name: String,
    val pageCount: Int,
    val sizeBytes: Long,
    val format: String,
    val syncState: String,
    val filePath: String,
    val createdAt: Long,
    val ocrText: String? = null,
)
