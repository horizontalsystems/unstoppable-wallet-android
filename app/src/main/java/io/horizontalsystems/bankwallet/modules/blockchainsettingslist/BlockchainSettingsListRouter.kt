package io.horizontalsystems.bankwallet.modules.blockchainsettingslist

import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.core.SingleLiveEvent

class BlockchainSettingsListRouter: BlockchainSettingsListModule.IRouter {

    val closeWithResultOk = SingleLiveEvent<Unit>()
    val openBlockchainSettings = SingleLiveEvent<CoinType>()
    val close = SingleLiveEvent<Unit>()

    override fun closeWithResultOk() {
        closeWithResultOk.call()
    }

    override fun close() {
        close.call()
    }

    override fun openBlockchainSetting(coinType: CoinType) {
        openBlockchainSettings.postValue(coinType)
    }
}
