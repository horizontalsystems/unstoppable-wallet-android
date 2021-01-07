package io.horizontalsystems.bankwallet.entities

sealed class DataState<out T> {
    data class Success<out T>(val data: T) : DataState<T>()
    data class Error(val error: Throwable) : DataState<Nothing>()
    object Loading : DataState<Nothing>()

    val loading: Boolean
        get() = this is Loading

    val dataOrNull: T?
        get() = (this as? Success)?.data

    val errorOrNull: Throwable?
        get() = (this as? Error)?.error
}
