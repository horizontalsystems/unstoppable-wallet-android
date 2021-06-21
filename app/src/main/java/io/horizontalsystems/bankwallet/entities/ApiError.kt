package io.horizontalsystems.bankwallet.entities

sealed class ApiError : Exception() {
    object ApiLimitExceeded : ApiError()
    object ContractNotFound : ApiError()
    object Bep2SymbolNotFound : ApiError()
    object InvalidResponse : ApiError()
}
