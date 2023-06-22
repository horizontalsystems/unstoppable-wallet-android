package cash.p.terminal.modules.balance.cex

import cash.p.terminal.modules.depositcex.DepositCexModule

interface ICexDepositService {
    suspend fun getCoins(): List<DepositCexModule.CexCoinViewItem>
    suspend fun getNetworks(coinUid: String): List<DepositCexModule.NetworkViewItem>
}
