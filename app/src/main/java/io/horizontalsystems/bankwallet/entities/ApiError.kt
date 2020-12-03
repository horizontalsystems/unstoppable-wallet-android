package io.horizontalsystems.bankwallet.entities

sealed class ApiError : Exception() {
    object ApiLimitExceeded : ApiError()
    object ContractNotFound : ApiError()
    object TokenNotFound : ApiError()
    object InvalidResponse : ApiError()
}
