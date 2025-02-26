package io.horizontalsystems.solanakit.core

import io.horizontalsystems.solanakit.transactions.SolanaFmService

class TokenProvider(private val solanaFmService: SolanaFmService) {

    suspend fun getTokenInfo(mintAddress: String) = solanaFmService.tokenInfo(mintAddress)

}
