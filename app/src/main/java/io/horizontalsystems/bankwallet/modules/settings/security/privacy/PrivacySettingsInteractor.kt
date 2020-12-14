package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.TorStatus
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.core.IPinComponent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

class PrivacySettingsInteractor(
        private val pinComponent: IPinComponent,
        private val torManager: ITorManager,
        private val syncModeSettingsManager: ISyncModeSettingsManager,
        private val communicationSettingsManager: ICommunicationSettingsManager,
        private val coinManager: ICoinManager,
        private val walletManager: IWalletManager,
        private val localStorageManager: ILocalStorage,
        private val adapterManager: IAdapterManager,
        private val accountManager: IAccountManager
) : PrivacySettingsModule.IPrivacySettingsInteractor {

    var delegate: PrivacySettingsModule.IPrivacySettingsInteractorDelegate? = null

    private var disposables: CompositeDisposable = CompositeDisposable()

    override var transactionsSortingType: TransactionDataSortingType
        get() = localStorageManager.transactionSortingType
        set(value) {
            localStorageManager.transactionSortingType = value
        }

    override val walletsCount: Int
        get() = walletManager.wallets.count()

    override var isTorEnabled: Boolean
        get() = torManager.isTorEnabled
        set(value) {
            pinComponent.updateLastExitDateBeforeRestart()
            if (value) {
                torManager.enableTor()
            } else {
                torManager.disableTor()
            }
        }

    override val isTorNotificationEnabled: Boolean
        get() = torManager.isTorNotificationEnabled

    override fun subscribeToTorStatus() {
        delegate?.onTorConnectionStatusUpdated(TorStatus.Closed)
        torManager.torObservable
                .subscribe { connectionStatus ->
                    delegate?.onTorConnectionStatusUpdated(connectionStatus)
                }.let {
                    disposables.add(it)
                }
    }

    override fun reSyncWallets(wallets: List<Wallet>) {
        adapterManager.refreshAdapters(wallets)
    }

    private fun getStandartAccountOrigin(): AccountOrigin? {

        // Standart Wallet mnemonic size
        val STANDART_ACCOUNT_WORDS_COUNT = 12

        accountManager.accounts.forEach {
            if(it.type is AccountType.Mnemonic && it.type.words.size == STANDART_ACCOUNT_WORDS_COUNT){
                return it.origin
            }
        }

        return null
    }

    override fun isAccountOriginCreated(): Boolean {

        return getStandartAccountOrigin()?.let {
            it == AccountOrigin.Created
        }?:false
    }

    override fun stopTor() {
        torManager.stop()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    delegate?.didStopTor()
                }, {

                }).let {
                    disposables.add(it)
                }
    }

    override fun enableTor() {
        torManager.start()
    }

    override fun disableTor() {
        torManager.stop()
                .subscribe()
                .let {
                    disposables.add(it)
                }
    }

    override fun communicationSetting(coinType: CoinType): CommunicationSetting? {
        return communicationSettingsManager.communicationSetting(coinType)
    }

    override fun saveCommunicationSetting(communicationSetting: CommunicationSetting) {
        communicationSettingsManager.updateSetting(communicationSetting)
    }

    override fun syncModeSetting(coinType: CoinType): SyncModeSetting? {
        return syncModeSettingsManager.syncModeSetting(coinType)
    }

    override fun saveSyncModeSetting(syncModeSetting: SyncModeSetting) {
        syncModeSettingsManager.updateSetting(syncModeSetting)
    }

    override fun ether(): Coin {
        return coinManager.coins.first { it.code == "ETH" }
    }

    override fun eos(): Coin {
        return coinManager.coins.first { it.code == "EOS" }
    }

    override fun binance(): Coin {
        return coinManager.coins.first { it.code == "BNB" }
    }

    override fun bitcoin(): Coin {
        return coinManager.coins.first { it.code == "BTC" }
    }

    override fun litecoin(): Coin {
        return coinManager.coins.first { it.code == "LTC" }
    }

    override fun bitcoinCash(): Coin {
        return coinManager.coins.first { it.code == "BCH" }
    }

    override fun dash(): Coin {
        return coinManager.coins.first { it.code == "DASH" }
    }

    override fun getWalletsForUpdate(coinType: CoinType): List<Wallet> {
        // include Erc20 wallets for CoinType.Ethereum
        return walletManager.wallets.filter { it.coin.type == coinType || (coinType == CoinType.Ethereum && it.coin.type is CoinType.Erc20) }
    }

    override fun clear() {
        disposables.clear()
    }

}
