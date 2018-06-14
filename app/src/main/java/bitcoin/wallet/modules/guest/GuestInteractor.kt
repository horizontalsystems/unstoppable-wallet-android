package bitcoin.wallet.modules.guest

import bitcoin.wallet.lib.WalletDataManager

class GuestInteractor: GuestModule.IInteractor {

    override lateinit var delegate: GuestModule.IInteractorDelegate
    override lateinit var walletDataProvider: WalletDataManager

    override fun createWallet() {
        WalletDataManager.createWallet()

        delegate.didCreateWallet()
    }

}