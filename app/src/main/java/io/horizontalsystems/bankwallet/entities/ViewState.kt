package io.horizontalsystems.bankwallet.entities

sealed class ViewState {
    class Error(val t: Throwable) : ViewState()
    object Success : ViewState()
}
