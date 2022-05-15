package io.horizontalsystems.bankwallet.modules.nft

import io.horizontalsystems.bankwallet.entities.ViewState

data class DataWithError<T>(
    val value: T,
    val error: Exception?
)

val <T> DataWithError<T>.viewState: ViewState?
    get() = when {
        error != null && value == null -> ViewState.Error(error)
        value != null -> ViewState.Success
        else -> null
    }
