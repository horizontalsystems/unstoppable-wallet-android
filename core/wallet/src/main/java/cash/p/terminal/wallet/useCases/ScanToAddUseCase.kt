package cash.p.terminal.wallet.useCases

import cash.p.terminal.wallet.entities.TokenQuery

interface ScanToAddUseCase {
    suspend fun addTokensByScan(
        blockchainsToDerive: List<TokenQuery> = emptyList(),
        cardId: String,
        accountId: String
    ): Boolean
}