package com.docscan.pro.domain

/** A user-created folder. Documents with a null folderId live in the default (Unfiled) area. */
data class Folder(
    val id: String,
    val name: String,
)
