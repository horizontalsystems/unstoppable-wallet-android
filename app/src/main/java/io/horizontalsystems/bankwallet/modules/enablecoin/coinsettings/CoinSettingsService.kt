package io.horizontalsystems.bankwallet.modules.enablecoin.coinsettings

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.subjects.PublishSubject

class CoinSettingsService : Clearable {
    val approveSettingsObservable = PublishSubject.create<CoinWithSettings>()
    val rejectApproveSettingsObservable = PublishSubject.create<Coin>()
    val requestObservable = PublishSubject.create<Request>()

    fun approveSettings(platformCoin: PlatformCoin, settings: List<CoinSettings>) {
        if (platformCoin.coinType.coinSettingTypes.contains(CoinSettingType.derivation)) {
            val currentDerivations = settings.mapNotNull {
                it.settings[CoinSettingType.derivation]?.let {
                    AccountType.Derivation.fromString(it)
                }
            }
            val type = RequestType.Derivation(AccountType.Derivation.values().toList(), currentDerivations)
            val request = Request(platformCoin, type)
            requestObservable.onNext(request)
            return
        }

        if (platformCoin.coinType.coinSettingTypes.contains(CoinSettingType.bitcoinCashCoinType)) {
            val currentTypes = settings.mapNotNull {
                it.settings[CoinSettingType.bitcoinCashCoinType]?.let {
                    BitcoinCashCoinType.fromString(it)
                }
            }
            val type = RequestType.BCHCoinType(BitcoinCashCoinType.values().toList(), currentTypes)
            val request = Request(platformCoin, type)
            requestObservable.onNext(request)
            return
        }

        approveSettingsObservable.onNext(CoinWithSettings(platformCoin))
    }

    fun selectDerivations(derivations: List<AccountType.Derivation>, coin: PlatformCoin) {
        val settingsList: List<CoinSettings> = derivations.map {
            CoinSettings(mapOf(CoinSettingType.derivation to it.value))
        }
        val coinWithSettings = CoinWithSettings(coin, settingsList)
        approveSettingsObservable.onNext(coinWithSettings)
    }

    fun selectBchCoinTypes(bchCoinTypes: List<BitcoinCashCoinType>, coin: PlatformCoin) {
        val settingsList: List<CoinSettings> = bchCoinTypes.map {
            CoinSettings(mapOf(CoinSettingType.bitcoinCashCoinType to it.value))
        }
        val coinWithSettings = CoinWithSettings(coin, settingsList)
        approveSettingsObservable.onNext(coinWithSettings)
    }

    fun cancel(coin: Coin) {
        rejectApproveSettingsObservable.onNext(coin)
    }

    override fun clear() = Unit

    data class CoinWithSettings(val coin: PlatformCoin, val settingsList: List<CoinSettings> = listOf())
    data class Request(val platformCoin: PlatformCoin, val type: RequestType)
    sealed class RequestType {
        class Derivation(
            val allDerivations: List<AccountType.Derivation>,
            val current: List<AccountType.Derivation>
        ) : RequestType()

        class BCHCoinType(
            val allTypes: List<BitcoinCashCoinType>,
            val current: List<BitcoinCashCoinType>
        ) : RequestType()
    }

}
