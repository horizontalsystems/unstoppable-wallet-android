package io.horizontalsystems.core

import kotlinx.coroutines.CoroutineExceptionHandler
import java.io.PrintWriter
import java.io.StringWriter
import java.util.logging.Level
import java.util.logging.Logger

object FeatureCoroutineExceptionHandler {
    fun create(from: String): CoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val exceptionAsString: String = sw.toString()
        Logger.getLogger("CoroutineExceptHandler").log(
            Level.INFO,
            "CoroutineException: from: $from, exception: $exceptionAsString",
        )
        throw throwable
    }
}
