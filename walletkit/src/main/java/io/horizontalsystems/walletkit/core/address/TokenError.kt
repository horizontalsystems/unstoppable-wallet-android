package io.horizontalsystems.walletkit.core.address

sealed class TokenError : Exception() {
    object InvalidTokenType : TokenError()
    object InvalidAddress : TokenError()
    object InvalidContractAddress : TokenError()
    object NoSyncSource : TokenError()
    object NoMethod : TokenError()
    object NetworkError : TokenError()
    object ContractError : TokenError()
}
