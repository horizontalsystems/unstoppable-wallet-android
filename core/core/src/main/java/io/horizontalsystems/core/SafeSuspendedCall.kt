package io.horizontalsystems.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

object SafeSuspendedCall {

    /**
     * Executes a suspend function with handling exception, temporarily replacing the global exception handler.
     */
    suspend fun <T> executeSuspendable(
        libraryCall: suspend () -> T
    ): T = suspendCancellableCoroutine { continuation ->

        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        var isCompleted = false

        // Temporary replace exception handler
        Thread.setDefaultUncaughtExceptionHandler { _, exception ->
            synchronized(continuation) {
                if (!isCompleted) {
                    isCompleted = true
                    Thread.setDefaultUncaughtExceptionHandler(originalHandler)
                    continuation.resumeWithException(exception)
                }
            }
        }

        // Run library call in a separate coroutine
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = libraryCall()

                synchronized(continuation) {
                    if (!isCompleted) {
                        isCompleted = true
                        Thread.setDefaultUncaughtExceptionHandler(originalHandler)
                        continuation.resume(result) {}
                    }
                }
            } catch (e: Exception) {
                synchronized(continuation) {
                    if (!isCompleted) {
                        isCompleted = true
                        Thread.setDefaultUncaughtExceptionHandler(originalHandler)
                        continuation.resumeWithException(e)
                    }
                }
            }
        }

        // Handle coroutine cancellation
        continuation.invokeOnCancellation {
            synchronized(continuation) {
                if (!isCompleted) {
                    isCompleted = true
                    Thread.setDefaultUncaughtExceptionHandler(originalHandler)
                }
            }
        }
    }
}