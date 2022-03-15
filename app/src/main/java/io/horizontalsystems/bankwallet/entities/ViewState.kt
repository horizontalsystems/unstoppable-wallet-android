package io.horizontalsystems.bankwallet.entities

sealed class ViewState {
    class Error(val t: Throwable) : ViewState()
    object Success : ViewState()
    object Loading : ViewState()

    fun merge(other: ViewState?) = when {
        this is Success && other is Success -> Success
        this is Error -> this
        other is Error -> other
        this is Loading -> Loading
        other is Loading -> Loading
        else -> null
    }
}

val <T> Result<T>.viewState: ViewState?
    get() = when {
        isSuccess -> ViewState.Success
        isFailure -> exceptionOrNull()?.let { ViewState.Error(it) }
        else -> null
    }
