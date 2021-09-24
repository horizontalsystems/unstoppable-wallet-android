package io.horizontalsystems.bankwallet.modules.blockchainsettings

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins.CoinSettingsService
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorViewItem
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.disposables.CompositeDisposable

class CoinSettingsViewModel(
        private val service: CoinSettingsService,
        private val clearables: List<Clearable>
) : ViewModel() {

    private var disposables = CompositeDisposable()

    val openBottomSelectorLiveEvent = SingleLiveEvent<BlockchainSettingsModule.Config>()

    private var currentRequest: CoinSettingsService.Request? = null

    init {
        service.requestObservable
                .subscribeIO {
                    handle(it)
                }
                .let { disposables.add(it) }
    }

    private fun handle(request: CoinSettingsService.Request) {
        val config = when (request.type) {
            is CoinSettingsService.RequestType.Derivation -> {
                derivationConfig(request.coin, request.type.allDerivations, request.type.current)
            }
            is CoinSettingsService.RequestType.BCHCoinType -> {
                bitcoinCashCoinTypeConfig(request.coin, request.type.allTypes, request.type.current)
            }
        }

        currentRequest = request
        openBottomSelectorLiveEvent.postValue(config)
    }

    private fun derivationConfig(platformCoin: PlatformCoin, allDerivations: List<AccountType.Derivation>, current: List<AccountType.Derivation>): BlockchainSettingsModule.Config {
        return BlockchainSettingsModule.Config(
                platformCoin = platformCoin,
                title = Translator.getString(R.string.AddressFormatSettings_Title),
                subtitle = platformCoin.name,
                selectedIndexes = current.map { allDerivations.indexOf(it) }.filter { it > -1 },
                viewItems = allDerivations.map { derivation ->
                    BottomSheetSelectorViewItem(
                            title = derivation.longTitle(),
                            subtitle = Translator.getString(derivation.description(), (derivation.addressPrefix(platformCoin.coinType) ?: ""))
                    )
                },
                description = Translator.getString(R.string.AddressFormatSettings_Description, platformCoin.name)
        )
    }

    private fun bitcoinCashCoinTypeConfig(platformCoin: PlatformCoin, types: List<BitcoinCashCoinType>, current: List<BitcoinCashCoinType>): BlockchainSettingsModule.Config {
        return BlockchainSettingsModule.Config(
                platformCoin = platformCoin,
                title = Translator.getString(R.string.AddressFormatSettings_Title),
                subtitle = platformCoin.name,
                selectedIndexes = current.map { types.indexOf(it) }.filter { it > -1 },
                viewItems = types.map { type ->
                    BottomSheetSelectorViewItem(
                            title = Translator.getString(type.title),
                            subtitle = Translator.getString(type.description)
                    )
                },
                description = Translator.getString(R.string.AddressFormatSettings_Description, platformCoin.name)
        )
    }

    fun onSelect(indexes: List<Int>) {
        val request = currentRequest ?: return

        when (request.type) {
            is CoinSettingsService.RequestType.Derivation -> {
                service.selectDerivations(indexes.map { request.type.allDerivations[it] }, request.coin)
            }
            is CoinSettingsService.RequestType.BCHCoinType -> {
                service.selectBchCoinTypes(indexes.map { request.type.allTypes[it] }, request.coin)
            }
        }
    }

    fun onCancelSelect() {
        val request = currentRequest ?: return

        service.cancel(request.coin)
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposables.clear()
    }

}
