package io.horizontalsystems.bankwallet.modules.enablecoin.coinsettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.BitcoinCashCoinType
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorMultipleDialog
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorViewItem
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.disposables.CompositeDisposable

class CoinSettingsViewModel(
    private val service: CoinSettingsService,
    private val clearables: List<Clearable>
) : ViewModel() {

    private var disposables = CompositeDisposable()

    var showBottomSheetDialog by mutableStateOf(false)
        private set

    var config: BottomSheetSelectorMultipleDialog.Config? = null
        private set

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
                bitcoinCashCoinTypeConfig(request.token, request.type.allTypes, request.type.current, request.allowEmpty)
            }
        }

        currentRequest = request
        this.config = config
        showBottomSheetDialog = true
    }

    private fun derivationConfig(
        token: Token,
        allDerivations: List<AccountType.Derivation>,
        current: List<AccountType.Derivation>,
        allowEmpty: Boolean
    ): BottomSheetSelectorMultipleDialog.Config {
        return BottomSheetSelectorMultipleDialog.Config(
            icon = ImageSource.Remote(token.coin.imageUrl, token.iconPlaceholder),
            title = token.coin.code,
            selectedIndexes = current.map { allDerivations.indexOf(it) }.filter { it > -1 },
            viewItems = allDerivations.map { derivation ->
                BottomSheetSelectorViewItem(
                    title = derivation.rawName,
                    subtitle = derivation.addressType
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
            icon = ImageSource.Remote(token.coin.imageUrl, token.iconPlaceholder),
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

    fun bottomSheetDialogShown() {
        showBottomSheetDialog = false
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
