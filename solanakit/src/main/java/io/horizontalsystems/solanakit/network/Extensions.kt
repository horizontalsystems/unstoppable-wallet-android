package io.horizontalsystems.solanakit.network

import android.util.Log
import com.solana.networking.JsonRpcDriver
import com.solana.networking.RpcRequest
import kotlinx.coroutines.delay
import kotlinx.serialization.KSerializer

suspend inline fun <reified R> JsonRpcDriver.makeRequestResultWithRepeat(
    request: RpcRequest,
    serializer: KSerializer<R>,
    repeatCount: Int = 2
): Result<R?> {
    repeat(repeatCount) {
        var timeout = 15000L
        try {
            this.makeRequest(request, serializer).let { response ->
                (response.result)?.let { result ->
                    return Result.success(result)
                }

                response.error?.let {
                    return Result.failure(Error(it.message))
                }

                // an empty error and empty result means we did not find anything, return null
                return Result.success(null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val tooManyRequests = e.message?.contains("429") == true
            if (tooManyRequests) {
                Log.d(
                    "Solana kit",
                    "makeRequestResultWithRepeat waiting for ${timeout/1000} seconds to request ${request.method} with params ${request.params}"
                )
                /* retry-after header is not present in the response, so we can't use it to determine the delay */
                delay(timeout)
                timeout *= 1.5.toLong()
            } else {
                if (tooManyRequests) {
                    Log.d(
                        "Solana kit",
                        "makeRequestResultWithRepeat too many requests to request ${request.method} with params ${request.params}"
                    )
                }
            }
        }
    }
    return Result.failure(Error("Failed to make request"))
}
