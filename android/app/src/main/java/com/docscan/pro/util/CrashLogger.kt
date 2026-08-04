package com.docscan.pro.util

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Minimal, privacy-preserving crash telemetry: records uncaught exceptions to a
 * capped local file (app-private storage) and then hands off to the platform's
 * default handler. Nothing is sent off-device — no third-party analytics SDK —
 * which keeps the app free and private. A real backend sink can be layered on
 * later if ever needed.
 */
object CrashLogger {

    private const val MAX_BYTES = 64 * 1024

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { write(appContext, thread, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** Path to the on-device crash log, for support/export. */
    fun logFile(context: Context): File = File(context.filesDir, "crash.log")

    private fun write(context: Context, thread: Thread, throwable: Throwable) {
        val trace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        val entry = buildString {
            append("---- crash @ ").append(System.currentTimeMillis()).append(" ----\n")
            append("thread=").append(thread.name).append('\n')
            append(trace).append('\n')
        }
        val log = logFile(context)
        // Keep the file bounded so it never grows without limit.
        if (log.exists() && log.length() > MAX_BYTES) log.delete()
        log.appendText(entry)
    }
}
