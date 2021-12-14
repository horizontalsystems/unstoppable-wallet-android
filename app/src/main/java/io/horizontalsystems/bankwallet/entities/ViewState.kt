package io.horizontalsystems.bankwallet.entities

sealed class ViewState {
    class Error(val t: Throwable) : ViewState()
    object Success : ViewState()

    fun merge(other: ViewState?) = when {
        this is Success && other is Success -> Success
        this is Error -> this
        other is Error -> other
        else -> null
    }
}
