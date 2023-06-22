package cash.p.terminal.modules.balance.cex

import cash.p.terminal.modules.depositcex.DepositCexModule

interface ICexDepositService {
    suspend fun getCoins(): List<DepositCexModule.CexCoinViewItem>
    suspend fun getNetworks(assetId: String): List<DepositCexModule.NetworkViewItem>
    suspend fun getAddress(assetId: String, networkId: String?): String
}
