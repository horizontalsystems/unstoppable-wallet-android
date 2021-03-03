package io.horizontalsystems.bankwallet.modules.blockchainsettings

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorViewItem
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class BlockchainSettingsViewModel(
        private val service: BlockchainSettingsService,
        private val stringProvider: StringProvider
) : ViewModel() {

    val openBottomSelectorLiveEvent = SingleLiveEvent<BlockchainSettingsModule.Config>()

    private var disposable: Disposable? = null
    private var currentRequest: BlockchainSettingsModule.Request? = null

    init {
        service.requestAsync
                .subscribeOn(Schedulers.io())
                .subscribe { handle(it) }
                .let { disposable = it }
    }

    override fun onCleared() {
        disposable?.dispose()
    }

    fun onSelect(index: Int) {
        val request = currentRequest ?: return

        when (request.type) {
            is BlockchainSettingsModule.RequestType.DerivationType -> {
                service.select(request.type.derivations[index], request.coin)
            }
            is BlockchainSettingsModule.RequestType.BitcoinCashType -> {
                service.select(request.type.types[index], request.coin)
            }
        }
    }

    fun onCancelSelect() {
        val request = currentRequest ?: return

        service.cancel(request.coin)
    }

    private fun handle(request: BlockchainSettingsModule.Request) {
        val config = when (request.type) {
            is BlockchainSettingsModule.RequestType.DerivationType -> {
                derivationConfig(request.coin, request.type.derivations, request.type.current)
            }
            is BlockchainSettingsModule.RequestType.BitcoinCashType -> {
                bitcoinCashCoinTypeConfig(request.coin, request.type.types, request.type.current)
            }
        }

        currentRequest = request
        openBottomSelectorLiveEvent.postValue(config)
    }

    private fun derivationConfig(coin: Coin, derivations: List<AccountType.Derivation>, current: AccountType.Derivation): BlockchainSettingsModule.Config {
        return BlockchainSettingsModule.Config(
                coin = coin,
                title = stringProvider.string(R.string.AddressFormatSettings_Title),
                subtitle = coin.title,
                selectedIndex = derivations.indexOfFirst { it == current },
                viewItems = derivations.map { derivation ->
                    BottomSheetSelectorViewItem(
                            title = derivation.longTitle(),
                            subtitle = stringProvider.string(derivation.description(), (derivation.addressPrefix(coin.type)
                                    ?: ""))
                    )
                }
        )
    }

    private fun bitcoinCashCoinTypeConfig(coin: Coin, types: List<BitcoinCashCoinType>, current: BitcoinCashCoinType): BlockchainSettingsModule.Config {
        return BlockchainSettingsModule.Config(
                coin = coin,
                title = stringProvider.string(R.string.AddressFormatSettings_Title),
                subtitle = coin.title,
                selectedIndex = types.indexOfFirst { it == current },
                viewItems = types.map { type ->
                    BottomSheetSelectorViewItem(
                            title = stringProvider.string(type.title),
                            subtitle = stringProvider.string(type.description)
                    )
                }
        )
    }

}
