package io.horizontalsystems.bankwallet.entities

sealed class FeeState {

    object Loading : FeeState()
    class Value(val value: Long) : FeeState()
    class Error(val error: Exception) : FeeState()

    val isLoading: Boolean
        get() = this is Loading

    val isValid: Boolean
        get() = this is Value

    val isError: Boolean
        get() = this is Error

}