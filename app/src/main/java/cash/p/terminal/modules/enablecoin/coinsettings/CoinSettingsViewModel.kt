package cash.p.terminal.modules.enablecoin.coinsettings

import androidx.lifecycle.ViewModel
import cash.p.terminal.R
import cash.p.terminal.core.Clearable
import cash.p.terminal.core.iconPlaceholder
import cash.p.terminal.core.iconUrl
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.AccountType
import cash.p.terminal.entities.BitcoinCashCoinType
import cash.p.terminal.entities.description
import cash.p.terminal.entities.rawName
import cash.p.terminal.modules.market.ImageSource
import cash.p.terminal.ui.extensions.BottomSheetSelectorMultipleDialog
import cash.p.terminal.ui.extensions.BottomSheetSelectorViewItem
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.marketkit.models.Token
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
                derivationConfig(request.token, request.type.allDerivations, request.type.current, request.allowEmpty)
            }
            is CoinSettingsService.RequestType.BCHCoinType -> {
                bitcoinCashCoinTypeConfig(request.token, request.type.allTypes, request.type.current, request.allowEmpty,)
            }
        }

        currentRequest = request
        openBottomSelectorLiveEvent.postValue(config)
    }

    private fun derivationConfig(
        token: Token,
        allDerivations: List<AccountType.Derivation>,
        current: List<AccountType.Derivation>,
        allowEmpty: Boolean
    ): BottomSheetSelectorMultipleDialog.Config {
        return BottomSheetSelectorMultipleDialog.Config(
            icon = ImageSource.Remote(token.coin.iconUrl, token.iconPlaceholder),
            title = token.coin.code,
            selectedIndexes = current.map { allDerivations.indexOf(it) }.filter { it > -1 },
            viewItems = allDerivations.map { derivation ->
                BottomSheetSelectorViewItem(
                    title = derivation.rawName,
                    subtitle = derivation.description
                )
            },
            description = Translator.getString(R.string.AddressFormatSettings_Description, token.coin.name),
            allowEmpty = allowEmpty,
        )
    }

    private fun bitcoinCashCoinTypeConfig(
        token: Token,
        types: List<BitcoinCashCoinType>,
        current: List<BitcoinCashCoinType>,
        allowEmpty: Boolean
    ): BottomSheetSelectorMultipleDialog.Config {
        return BottomSheetSelectorMultipleDialog.Config(
            icon = ImageSource.Remote(token.coin.iconUrl, token.iconPlaceholder),
            title = token.coin.code,
            selectedIndexes = current.map { types.indexOf(it) }.filter { it > -1 },
            viewItems = types.map { type ->
                BottomSheetSelectorViewItem(
                    title = Translator.getString(type.title),
                    subtitle = Translator.getString(type.description)
                )
            },
            descriptionTitle = null,
            description = Translator.getString(R.string.AddressFormatSettings_Description, token.coin.name),
            allowEmpty = allowEmpty,
        )
    }

    fun onSelect(indexes: List<Int>) {
        val request = currentRequest ?: return

        when (request.type) {
            is CoinSettingsService.RequestType.Derivation -> {
                service.selectDerivations(indexes.map { request.type.allDerivations[it] }, request.token)
            }
            is CoinSettingsService.RequestType.BCHCoinType -> {
                service.selectBchCoinTypes(indexes.map { request.type.allTypes[it] }, request.token)
            }
        }
    }

    fun onCancelSelect() {
        val request = currentRequest ?: return

        service.cancel(request.token)
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposables.clear()
    }

}
