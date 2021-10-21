package io.horizontalsystems.bankwallet.modules.enablecoin.coinsettings

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.BitcoinCashCoinType
import io.horizontalsystems.bankwallet.entities.description
import io.horizontalsystems.bankwallet.entities.title
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorMultipleDialog
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorViewItem
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.disposables.CompositeDisposable

class CoinSettingsViewModel(
    private val service: CoinSettingsService,
    private val clearables: List<Clearable>
) : ViewModel() {

    private var disposables = CompositeDisposable()

    val openBottomSelectorLiveEvent = SingleLiveEvent<BottomSheetSelectorMultipleDialog.Config>()

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
                derivationConfig(request.platformCoin, request.type.allDerivations, request.type.current)
            }
            is CoinSettingsService.RequestType.BCHCoinType -> {
                bitcoinCashCoinTypeConfig(request.platformCoin, request.type.allTypes, request.type.current)
            }
        }

        currentRequest = request
        openBottomSelectorLiveEvent.postValue(config)
    }

    private fun derivationConfig(
        platformCoin: PlatformCoin,
        allDerivations: List<AccountType.Derivation>,
        current: List<AccountType.Derivation>
    ): BottomSheetSelectorMultipleDialog.Config {
        return BottomSheetSelectorMultipleDialog.Config(
            icon = ImageSource.Remote(platformCoin.coin.iconUrl, platformCoin.coinType.iconPlaceholder),
            title = Translator.getString(R.string.AddressFormatSettings_Title),
            subtitle = platformCoin.name,
            selectedIndexes = current.map { allDerivations.indexOf(it) }.filter { it > -1 },
            viewItems = allDerivations.map { derivation ->
                BottomSheetSelectorViewItem(
                    title = derivation.title,
                    subtitle = derivation.description
                )
            },
            description = Translator.getString(R.string.AddressFormatSettings_Description, platformCoin.name)
        )
    }

    private fun bitcoinCashCoinTypeConfig(
        platformCoin: PlatformCoin,
        types: List<BitcoinCashCoinType>,
        current: List<BitcoinCashCoinType>
    ): BottomSheetSelectorMultipleDialog.Config {
        return BottomSheetSelectorMultipleDialog.Config(
            icon = ImageSource.Remote(platformCoin.coin.iconUrl, platformCoin.coinType.iconPlaceholder),
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
                service.selectDerivations(indexes.map { request.type.allDerivations[it] }, request.platformCoin)
            }
            is CoinSettingsService.RequestType.BCHCoinType -> {
                service.selectBchCoinTypes(indexes.map { request.type.allTypes[it] }, request.platformCoin)
            }
        }
    }

    fun onCancelSelect() {
        val request = currentRequest ?: return

        service.cancel(request.platformCoin.coin)
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposables.clear()
    }

}
