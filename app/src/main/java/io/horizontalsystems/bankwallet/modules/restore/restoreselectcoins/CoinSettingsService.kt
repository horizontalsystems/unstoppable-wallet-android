package io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.coinkit.models.Coin
import io.reactivex.subjects.PublishSubject

class CoinSettingsService : Clearable {
    val approveSettingsObservable = PublishSubject.create<CoinWithSettings>()
    val rejectApproveSettingsObservable = PublishSubject.create<Coin>()
    val requestObservable = PublishSubject.create<Request>()

    fun approveSettings(coin: Coin, settings: List<CoinSettings>) {
        if (coin.type.coinSettingTypes.contains(CoinSettingType.derivation)) {
            val currentDerivations = settings.mapNotNull {
                it.settings[CoinSettingType.derivation]?.let {
                    AccountType.Derivation.fromString(it)
                }
            }

            val request = Request(coin, RequestType.Derivation(AccountType.Derivation.values().toList(), currentDerivations))

            requestObservable.onNext(request)
            return
        }

        if (coin.type.coinSettingTypes.contains(CoinSettingType.bitcoinCashCoinType)) {
            val currentTypes = settings.mapNotNull {
                it.settings[CoinSettingType.bitcoinCashCoinType]?.let {
                    BitcoinCashCoinType.fromString(it)
                }
            }

            val request = Request(coin, RequestType.BCHCoinType(BitcoinCashCoinType.values().toList(), currentTypes))

            requestObservable.onNext(request)
            return
        }

        approveSettingsObservable.onNext(CoinWithSettings(coin))
    }

    fun selectDerivations(derivations: List<AccountType.Derivation>, coin: Coin) {
        val settingsList: List<CoinSettings> = derivations.map {
            CoinSettings(mapOf(CoinSettingType.derivation to it.value))
        }
        val coinWithSettings = CoinWithSettings(coin, settingsList)
        approveSettingsObservable.onNext(coinWithSettings)
    }

    fun selectBchCoinTypes(bchCoinTypes: List<BitcoinCashCoinType>, coin: Coin) {
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

    data class CoinWithSettings(val coin: Coin, val settingsList: List<CoinSettings> = listOf())
    data class Request(val coin: Coin, val type: RequestType)
    sealed class RequestType {
        class Derivation(val allDerivations: List<AccountType.Derivation>, val current: List<AccountType.Derivation>) : RequestType()
        class BCHCoinType(val allTypes: List<BitcoinCashCoinType>, val current: List<BitcoinCashCoinType>) : RequestType()
    }

}
