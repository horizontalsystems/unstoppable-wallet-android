package io.horizontalsystems.bankwallet.entities

sealed class ViewState {
    object Error : ViewState()
    object Success : ViewState()
}
