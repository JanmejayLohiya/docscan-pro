package com.docscan.pro.domain

/** A single page of a document (points at the on-disk image). */
data class Page(
    val id: String,
    val orderIndex: Int,
    val imagePath: String,
)
