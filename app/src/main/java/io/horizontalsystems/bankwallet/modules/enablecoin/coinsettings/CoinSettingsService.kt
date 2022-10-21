package io.horizontalsystems.bankwallet.modules.enablecoin.coinsettings

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.coinSettingType
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.BitcoinCashCoinType
import io.horizontalsystems.bankwallet.entities.CoinSettingType
import io.horizontalsystems.bankwallet.entities.CoinSettings
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.subjects.PublishSubject

class CoinSettingsService : Clearable {
    val approveSettingsObservable = PublishSubject.create<CoinWithSettings>()
    val rejectApproveSettingsObservable = PublishSubject.create<Token>()
    val requestObservable = PublishSubject.create<Request>()

    fun approveSettings(token: Token, accountType: AccountType, settings: List<CoinSettings>, allowEmpty: Boolean = false) {
        if (token.blockchainType.coinSettingType == CoinSettingType.derivation) {
            val currentDerivations = settings.mapNotNull {
                it.settings[CoinSettingType.derivation]?.let {
                    AccountType.Derivation.fromString(it)
                }
            }
            val type = RequestType.Derivation(accountType.supportedDerivations, currentDerivations)
            val request = Request(token, type, allowEmpty)
            requestObservable.onNext(request)
            return
        }

        if (token.blockchainType.coinSettingType == CoinSettingType.bitcoinCashCoinType) {
            val currentTypes = settings.mapNotNull {
                it.settings[CoinSettingType.bitcoinCashCoinType]?.let {
                    BitcoinCashCoinType.fromString(it)
                }
            }
            val type = RequestType.BCHCoinType(BitcoinCashCoinType.values().toList(), currentTypes)
            val request = Request(token, type, allowEmpty)
            requestObservable.onNext(request)
            return
        }

        approveSettingsObservable.onNext(CoinWithSettings(token))
    }

    fun selectDerivations(derivations: List<AccountType.Derivation>, token: Token) {
        val settingsList: List<CoinSettings> = derivations.map {
            CoinSettings(mapOf(CoinSettingType.derivation to it.value))
        }
        val coinWithSettings = CoinWithSettings(token, settingsList)
        approveSettingsObservable.onNext(coinWithSettings)
    }

    fun selectBchCoinTypes(bchCoinTypes: List<BitcoinCashCoinType>, token: Token) {
        val settingsList: List<CoinSettings> = bchCoinTypes.map {
            CoinSettings(mapOf(CoinSettingType.bitcoinCashCoinType to it.value))
        }
        val coinWithSettings = CoinWithSettings(token, settingsList)
        approveSettingsObservable.onNext(coinWithSettings)
    }

    fun cancel(token: Token) {
        rejectApproveSettingsObservable.onNext(token)
    }

    override fun clear() = Unit

    data class CoinWithSettings(val token: Token, val settingsList: List<CoinSettings> = listOf())
    data class Request(val token: Token, val type: RequestType, val allowEmpty: Boolean)
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
