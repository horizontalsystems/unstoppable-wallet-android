package io.horizontalsystems.marketkit.providers

import io.reactivex.Flowable
import io.reactivex.Single
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

fun <T> Single<T>.retryWhenError(errorForRetry: KClass<*>, maxRetries: Int = 3): Single<T> {
    return retryWhen { errors ->
        var retryCounter = 0L
        errors.flatMap { error ->
            if (errorForRetry.isInstance(error) && retryCounter++ < maxRetries) {
                Flowable.timer(retryCounter, TimeUnit.SECONDS)
            } else {
                Flowable.error(error)
            }
        }
    }
}
