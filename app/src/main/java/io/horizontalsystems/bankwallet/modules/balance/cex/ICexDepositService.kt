package io.horizontalsystems.bankwallet.modules.balance.cex

import io.horizontalsystems.bankwallet.modules.depositcex.DepositCexModule

interface ICexDepositService {
    suspend fun getCoins(): List<DepositCexModule.CexCoinViewItem>
    suspend fun getNetworks(assetId: String): List<DepositCexModule.NetworkViewItem>
}
