package com.spiritwisestudios.crossroadsoffate.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import timber.log.Timber
import java.io.File

/**
 * Tests for the persistent error log: entry writing, boundary-aware
 * truncation, concurrent writers, and the Timber tree that feeds real
 * ERROR logs into the file.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ErrorLoggerTest {

    private lateinit var context: Context
    private val logFile: File get() = File(context.filesDir, "error_log.txt")

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        logFile.delete()
    }

    @After
    fun cleanup() {
        logFile.delete()
    }

    @Test
    fun saveErrorToFile_appendsTimestampedEntry() = runBlocking {
        ErrorLogger.saveErrorToFile(context, "Something broke", RuntimeException("boom"))

        val log = ErrorLogger.getErrorLog(context)!!
        assertTrue("Entry should carry a timestamp", log.startsWith("["))
        assertTrue(log.contains("ERROR: Something broke"))
        assertTrue(log.contains("Exception: RuntimeException"))
        assertTrue(log.contains("Message: boom"))
    }

    @Test
    fun clearErrorLog_removesTheFile() = runBlocking {
        ErrorLogger.saveErrorToFile(context, "entry")
        ErrorLogger.clearErrorLog(context)
        assertNull(ErrorLogger.getErrorLog(context))
    }

    @Test
    fun truncation_cutsOnAnEntryBoundary() = runBlocking {
        // Build an oversized log of well-formed entries, then trigger the
        // truncation path with one more write
        val separator = "\n-------------------------------------------------\n"
        val entry = "[2026-07-12 00:00:00] ERROR: filler ".padEnd(1024, 'x') + separator
        logFile.writeText(entry.repeat(1100)) // > 1MB
        val originalSize = logFile.length()

        ErrorLogger.saveErrorToFile(context, "the straw")

        val log = ErrorLogger.getErrorLog(context)!!
        assertTrue("Truncated log must shrink", log.length < originalSize)
        assertTrue("Truncated log must start at an entry boundary, not mid-record",
            log.startsWith("[2026-07-12"))
        assertTrue("The new entry must survive truncation", log.contains("the straw"))
    }

    @Test
    fun concurrentWrites_allLandIntact() = runBlocking {
        val writers = (1..16).map { i ->
            async(Dispatchers.IO) {
                ErrorLogger.saveErrorToFile(context, "concurrent-entry-$i")
            }
        }
        writers.awaitAll()

        val log = ErrorLogger.getErrorLog(context)!!
        (1..16).forEach { i ->
            assertTrue("Entry $i missing or torn", log.contains("ERROR: concurrent-entry-$i"))
        }
        assertEquals("Each entry must end with exactly one separator",
            16, Regex("-{49}").findAll(log).count())
    }

    @Test
    fun fileLoggingTree_persistsTimberErrors() {
        val tree = FileLoggingTree(context, CoroutineScope(Dispatchers.IO))
        Timber.plant(tree)
        try {
            Timber.e(IllegalStateException("kaboom"), "Save failed hard")

            // The tree writes asynchronously; poll briefly for the entry
            val log = runBlocking {
                var content: String? = null
                repeat(100) {
                    content = ErrorLogger.getErrorLog(context)
                    if (content?.contains("Save failed hard") == true) return@runBlocking content
                    delay(50)
                }
                content
            }
            assertTrue("Timber.e must land in the error log file",
                log?.contains("Save failed hard") == true)
            assertTrue(log?.contains("Exception: IllegalStateException") == true)
        } finally {
            Timber.uproot(tree)
        }
    }

    @Test
    fun fileLoggingTree_ignoresNonErrorPriorities() = runBlocking {
        val tree = FileLoggingTree(context, CoroutineScope(Dispatchers.IO))
        Timber.plant(tree)
        try {
            Timber.w("just a warning")
            Timber.i("just info")
            delay(200) // give any (incorrect) async write time to land
            assertNull("WARN/INFO must not be persisted", ErrorLogger.getErrorLog(context))
        } finally {
            Timber.uproot(tree)
        }
    }
}
