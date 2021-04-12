package io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.factories.AccountFactory
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.CoinSettings
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.blockchainsettings.BlockchainSettingsService
import io.horizontalsystems.bankwallet.modules.blockchainsettings.BlockchainSettingsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoins.*
import io.horizontalsystems.coinkit.models.Coin
import io.reactivex.Observable

object RestoreSelectCoinsModule {

    class Factory(private val predefinedAccountType: PredefinedAccountType, private val accountType: AccountType)
        : ViewModelProvider.Factory {

        private val enableCoinsService by lazy {
            EnableCoinsService(
                    App.buildConfigProvider,
                    EnableCoinsErc20Provider(App.networkManager),
                    EnableCoinsBep2Provider(App.buildConfigProvider),
                    EnableCoinsBep20Provider(App.networkManager, App.appConfigProvider.bscscanApiKey),
                    App.coinManager
            )
        }

        private val blockchainSettingsService by lazy {
            BlockchainSettingsService(App.derivationSettingsManager, App.bitcoinCashCoinTypeManager)
        }
        private val restoreSettingsService by lazy {
            RestoreSettingsService()
        }
        private val coinSettingsService by lazy {
            CoinSettingsService()
        }

        private val restoreSelectCoinsService by lazy {
            RestoreSelectCoinsService(
                    accountType,
                    AccountFactory(),
                    App.accountManager,
                    App.walletManager,
                    App.coinManager,
                    enableCoinsService,
                    restoreSettingsService,
                    coinSettingsService)
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                RestoreSelectCoinsViewModel::class.java -> {
                    RestoreSelectCoinsViewModel(restoreSelectCoinsService, listOf(restoreSelectCoinsService)) as T
                }
                BlockchainSettingsViewModel::class.java -> {
                    BlockchainSettingsViewModel(blockchainSettingsService) as T
                }
                EnableCoinsViewModel::class.java -> {
                    EnableCoinsViewModel(enableCoinsService) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }
}

enum class RestoreSettingType {
    birthdayHeight
}

class RestoreSettings {
    private val settings = mapOf<RestoreSettingType, String>()

    val birthdayHeight: Int?
        get() = settings[RestoreSettingType.birthdayHeight]?.toInt()

    fun isNotEmpty() = settings.isNotEmpty()

}

class RestoreSettingsService {

    val approveSettingsObservable: Observable<CoinWithSettings> = TODO()
    val rejectApproveSettingsObservable: Observable<Coin> = TODO()

    data class CoinWithSettings(val coin: Coin, val settings: RestoreSettings)

    fun approveSettings(coin: Coin) {
        TODO("Not yet implemented")
    }

    fun save(settings: RestoreSettings, account: Account, coin: Coin) {
        TODO("Not yet implemented")
    }
}

class CoinSettingsService {
    val rejectApproveSettingsObservable: Observable<Coin> = TODO()
    val approveSettingsObservable: Observable<CoinWithSettings> = TODO()

    fun approveSettings(coin: Coin, settings: List<CoinSettings>) {

    }

    data class CoinWithSettings(val coin: Coin, val settingsList: List<CoinSettings> = listOf())
}
