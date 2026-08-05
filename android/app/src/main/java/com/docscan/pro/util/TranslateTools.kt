package com.docscan.pro.util

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

/**
 * On-device translation via ML Kit — free, offline (after a one-time per-language
 * model download), no API key. Source language is auto-detected.
 */

/** Detects the BCP-47 language tag of [text], or "en" if undetermined. */
suspend fun detectLanguageTag(text: String): String {
    val client = LanguageIdentification.getClient()
    return try {
        val tag = client.identifyLanguage(text).await()
        if (tag == "und") "en" else tag
    } finally {
        client.close()
    }
}

/**
 * Translates [text] into [targetTag] (a BCP-47 code like "hi"). Downloads the
 * needed model on first use. Returns the original text if source == target, or
 * blank when [text] is blank.
 */
suspend fun translateText(text: String, targetTag: String): String {
    if (text.isBlank()) return ""
    val sourceCode = TranslateLanguage.fromLanguageTag(detectLanguageTag(text))
        ?: TranslateLanguage.ENGLISH
    val targetCode = TranslateLanguage.fromLanguageTag(targetTag)
        ?: error("Unsupported target language: $targetTag")
    if (sourceCode == targetCode) return text

    val translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(sourceCode)
            .setTargetLanguage(targetCode)
            .build(),
    )
    return try {
        translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
        translator.translate(text).await()
    } finally {
        translator.close()
    }
}

/**
 * Translates many strings with a single translator + one model download — used
 * for the in-layout overlay, which translates each text block of each page.
 * Source is detected from the first non-blank entry.
 */
suspend fun translateAll(texts: List<String>, targetTag: String): List<String> {
    if (texts.isEmpty()) return emptyList()
    val sample = texts.firstOrNull { it.isNotBlank() } ?: return texts
    val sourceCode = TranslateLanguage.fromLanguageTag(detectLanguageTag(sample))
        ?: TranslateLanguage.ENGLISH
    val targetCode = TranslateLanguage.fromLanguageTag(targetTag)
        ?: error("Unsupported target language: $targetTag")
    if (sourceCode == targetCode) return texts

    val translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(sourceCode)
            .setTargetLanguage(targetCode)
            .build(),
    )
    return try {
        translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
        texts.map { if (it.isBlank()) it else translator.translate(it).await() }
    } finally {
        translator.close()
    }
}
