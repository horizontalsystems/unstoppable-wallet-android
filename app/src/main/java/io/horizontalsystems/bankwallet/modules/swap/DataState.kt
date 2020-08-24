package io.horizontalsystems.bankwallet.modules.swap

sealed class DataState<out T> {
    data class Success<out T>(val data: T) : DataState<T>()
    data class Error(val error: Throwable) : DataState<Nothing>()
    object Loading : DataState<Nothing>()

    val dataOrNull: T?
        get() {
            return (this as? Success)?.data
        }
}
