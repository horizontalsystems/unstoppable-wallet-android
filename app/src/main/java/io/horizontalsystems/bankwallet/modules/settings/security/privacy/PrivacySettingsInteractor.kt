package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IBlockchainSettingsManager
import io.horizontalsystems.bankwallet.core.INetManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.core.IPinComponent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

class PrivacySettingsInteractor(
        private val pinComponent: IPinComponent,
        private val netManager: INetManager,
        private val blockchainSettingsManager: IBlockchainSettingsManager,
        private val appConfigProvider: IAppConfigProvider,
        private val walletManager: IWalletManager
) : PrivacySettingsModule.IPrivacySettingsInteractor {

    var delegate: PrivacySettingsModule.IPrivacySettingsInteractorDelegate? = null

    private var disposables: CompositeDisposable = CompositeDisposable()

    override var isTorEnabled: Boolean
        get() = netManager.isTorEnabled
        set(value) {
            pinComponent.updateLastExitDateBeforeRestart()
            if (value) {
                netManager.enableTor()
            } else {
                netManager.disableTor()
            }
        }

    override val isTorNotificationEnabled: Boolean
        get() = netManager.isTorNotificationEnabled

    override fun communicationSetting(coinType: CoinType): CommunicationSetting? {
        return blockchainSettingsManager.communicationSetting(coinType)
    }

    override fun saveCommunicationSetting(communicationSetting: CommunicationSetting) {
        blockchainSettingsManager.updateSetting(communicationSetting)
    }

    override fun syncModeSetting(coinType: CoinType): SyncModeSetting? {
        return blockchainSettingsManager.syncModeSetting(coinType)
    }

    override fun saveSyncModeSetting(syncModeSetting: SyncModeSetting) {
        blockchainSettingsManager.updateSetting(syncModeSetting)
    }

    override fun ether(): Coin {
        return appConfigProvider.coins.first { it.code == "ETH" }
    }

    override fun eos(): Coin {
        return appConfigProvider.coins.first { it.code == "EOS" }
    }

    override fun binance(): Coin {
        return appConfigProvider.coins.first { it.code == "BNB" }
    }

    override fun bitcoin(): Coin {
        return appConfigProvider.coins.first { it.code == "BTC" }
    }

    override fun litecoin(): Coin {
        return appConfigProvider.coins.first { it.code == "LTC" }
    }

    override fun bitcoinCash(): Coin {
        return appConfigProvider.coins.first { it.code == "BCH" }
    }

    override fun dash(): Coin {
        return appConfigProvider.coins.first { it.code == "DASH" }
    }

    override fun getWalletForUpdate(coinType: CoinType): Wallet? {
        return walletManager.wallets.firstOrNull { it.coin.type == coinType }
    }

    override fun reSyncWallet(wallet: Wallet) {
        walletManager.delete(listOf(wallet))

        //start wallet with updated settings
        walletManager.save(listOf(wallet))
    }

    override fun stopTor() {
        netManager.stop()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    delegate?.didStopTor()
                }, {

                }).let {
                    disposables.add(it)
                }
    }

    override fun clear() {
        disposables.clear()
    }

}
