package bitcoin.wallet.core.managers

import bitcoin.wallet.core.IWalletDataProvider
import bitcoin.wallet.core.WalletData

class StubWalletDataProvider : IWalletDataProvider {

    override val walletData: WalletData
        get() = WalletData(Factory.wordsManager.savedWords() ?: listOf())

}
