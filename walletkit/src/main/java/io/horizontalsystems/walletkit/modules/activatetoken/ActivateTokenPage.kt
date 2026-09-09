package io.horizontalsystems.walletkit.modules.activatetoken

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IActivatableTokenAdapter
import io.horizontalsystems.walletkit.core.IAdapterManager
import io.horizontalsystems.walletkit.core.TokenActivationError
import io.horizontalsystems.walletkit.core.TokenActivationInfo
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.alternativeImageUrl
import io.horizontalsystems.walletkit.core.badge
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.iconPlaceholder
import io.horizontalsystems.walletkit.core.imageUrl
import io.horizontalsystems.walletkit.entities.CoinValue
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.walletkit.entities.CurrencyValue
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.confirm.ConfirmTransactionScreen
import io.horizontalsystems.walletkit.modules.confirm.ErrorSheet
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataFieldFee
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.nav3.LocalResultEventBus
import io.horizontalsystems.walletkit.modules.xrate.XRateService
import io.horizontalsystems.walletkit.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.walletkit.ui.compose.components.HFillSpacer
import io.horizontalsystems.walletkit.ui.compose.components.HSpacer
import io.horizontalsystems.walletkit.ui.compose.components.HsImageCircle
import io.horizontalsystems.walletkit.ui.compose.components.TextImportantError
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.caption_grey
import io.horizontalsystems.walletkit.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.walletkit.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.walletkit.ui.compose.components.rememberAsyncAction
import io.horizontalsystems.walletkit.ui.compose.components.subhead1_leah
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_leah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/**
 * Confirms and submits the transaction that lets the account hold [wallet]'s token: a Stellar
 * ChangeTrust or an XRPL TrustSet. The chain's adapter does the work through
 * [IActivatableTokenAdapter]; its plugin supplies the wording.
 */
@Serializable
data class ActivateTokenPage(val wallet: Wallet) : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        ActivateTokenScreen(navigation, wallet)
    }

    data class Result(val activated: Boolean)
}

@Composable
fun ActivateTokenScreen(
    navigation: HSNavigation,
    wallet: Wallet,
) {
    val resultEventBus = LocalResultEventBus.current
    val viewModel = viewModel<ActivateTokenViewModel>(factory = ActivateTokenViewModel.Factory(wallet))

    val uiState = viewModel.uiState
    val token = uiState.token

    ConfirmTransactionScreen(
        title = stringResource(R.string.Send_Confirmation_Title),
        onClickBack = null,
        onClickFeeSettings = null,
        buttonsSlot = {
            val activateAction = rememberAsyncAction()
            val view = LocalView.current

            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(if (activateAction.inProgress) R.string.Activate_Activating else R.string.Button_Activate),
                onClick = {
                    activateAction.run {
                        try {
                            viewModel.activate()

                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                            delay(1200)
                            resultEventBus.sendResult(ActivateTokenPage.Result(true))
                            navigation.removeLastOrNull()
                        } catch (t: Throwable) {
                            navigation.slideFromBottom(ErrorSheet(ErrorSheet.Input(t.message ?: t.javaClass.simpleName)))
                        }
                    }
                },
                enabled = !activateAction.inProgress && uiState.activateEnabled
            )
        }
    ) {
        SectionUniversalLawrence {
            CellUniversal(borderTop = false) {
                HsImageCircle(
                    modifier = Modifier.size(32.dp),
                    url = token.coin.imageUrl,
                    alternativeUrl = token.coin.alternativeImageUrl,
                    placeholder = token.iconPlaceholder
                )
                HSpacer(width = 16.dp)
                Column {
                    subhead2_leah(text = stringResource(R.string.Activate_YouActivate))
                    VSpacer(height = 1.dp)
                    caption_grey(text = token.badge ?: stringResource(id = R.string.CoinPlatforms_Native))
                }
                HFillSpacer(minWidth = 16.dp)
                Column(horizontalAlignment = Alignment.End) {
                    subhead1_leah(text = token.coin.code)
                }
            }
        }

        VSpacer(height = 16.dp)
        SectionUniversalLawrence {
            DataFieldFee(
                navigation,
                uiState.feeCoinValue?.getFormattedFull() ?: "---",
                uiState.feeFiatValue?.getFormattedFull() ?: "---"
            )
        }

        uiState.activationInfo?.reserveNote?.let { note ->
            VSpacer(height = 12.dp)
            caption_grey(
                modifier = Modifier.padding(horizontal = 32.dp),
                text = note,
            )
        }

        uiState.error?.let { error ->
            VSpacer(16.dp)
            val modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)

            when (error) {
                is ActivateTokenError.AlreadyActive -> TextImportantError(
                    modifier = modifier,
                    text = stringResource(R.string.Activate_AlreadyActive_Description),
                    title = stringResource(R.string.Activate_AlreadyActive_Title),
                    icon = R.drawable.ic_attention_20
                )

                is ActivateTokenError.NullAdapter -> TextImportantError(
                    modifier = modifier,
                    text = stringResource(R.string.Error_ParameterNotSet),
                    title = null,
                    icon = null
                )

                is ActivateTokenError.InsufficientBalance -> TextImportantError(
                    modifier = modifier,
                    title = stringResource(R.string.Activate_InsufficientBalance_Title),
                    text = stringResource(
                        uiState.activationInfo?.insufficientBalanceDescriptionRes ?: R.string.Activate_InsufficientBalance_Description
                    ),
                    icon = R.drawable.ic_attention_20
                )
            }
        }
    }
}

class ActivateTokenViewModel(
    wallet: Wallet,
    feeToken: Token,
    adapterManager: IAdapterManager,
    xRateService: XRateService,
) : ViewModelUiState<ActivateTokenUiState>() {
    private val token = wallet.token
    private val adapter = adapterManager.getAdapterForWallet<IActivatableTokenAdapter>(wallet)
    private val activationInfo = ChainRegistry[wallet.token.blockchainType]?.tokenActivationInfo(wallet)
    private var activateEnabled = false
    private var error: ActivateTokenError? = null
    private var feeCoinValue: CoinValue? = null
    private var feeFiatValue: CurrencyValue? = null

    init {
        viewModelScope.launch(Dispatchers.Default) {
            val tmpAdapter = adapter

            if (tmpAdapter == null) {
                activateEnabled = false
                error = ActivateTokenError.NullAdapter()
            } else if (tmpAdapter.isActivated()) {
                activateEnabled = false
                error = ActivateTokenError.AlreadyActive()
            } else try {
                tmpAdapter.validateActivation()
                activateEnabled = true
                error = null
            } catch (e: TokenActivationError.InsufficientBalance) {
                activateEnabled = false
                error = ActivateTokenError.InsufficientBalance()
            }

            tmpAdapter?.activationFee?.let { feeAmount ->
                feeCoinValue = CoinValue(feeToken, feeAmount)
                feeFiatValue = xRateService.getRate(feeToken.coin.uid)?.let { rate ->
                    rate.copy(value = rate.value * feeAmount)
                }
            }

            emitState()
        }
    }

    override fun createState() = ActivateTokenUiState(
        token = token,
        currency = App.currencyManager.baseCurrency,
        activateEnabled = activateEnabled,
        error = error,
        feeCoinValue = feeCoinValue,
        feeFiatValue = feeFiatValue,
        activationInfo = activationInfo,
    )

    suspend fun activate() = withContext(Dispatchers.Default) {
        adapter?.activate()
    }

    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val feeToken = App.coinManager.getToken(TokenQuery(wallet.token.blockchainType, TokenType.Native))
                ?: throw IllegalArgumentException()
            val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)
            return ActivateTokenViewModel(wallet, feeToken, App.adapterManager, xRateService) as T
        }
    }
}

sealed class ActivateTokenError : Throwable() {
    class NullAdapter : ActivateTokenError()
    class AlreadyActive : ActivateTokenError()
    class InsufficientBalance : ActivateTokenError()
}

data class ActivateTokenUiState(
    val token: Token,
    val currency: Currency,
    val activateEnabled: Boolean,
    val error: ActivateTokenError?,
    val feeCoinValue: CoinValue?,
    val feeFiatValue: CurrencyValue?,
    val activationInfo: TokenActivationInfo?,
)
