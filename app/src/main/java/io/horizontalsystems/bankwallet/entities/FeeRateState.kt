package io.horizontalsystems.bankwallet.entities

sealed class FeeRateState {

    object Loading : FeeRateState()
    class Value(val value: Long) : FeeRateState()
    class Error(val error: Exception) : FeeRateState()

    val isLoading: Boolean
        get() = this is Loading

    val isValid: Boolean
        get() = this is Value

    val isError: Boolean
        get() = this is Error

}