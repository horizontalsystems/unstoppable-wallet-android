package io.horizontalsystems.bankwallet.modules.activatetoken

import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.alternativeImageUrl
import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.confirm.ConfirmTransactionScreen
import io.horizontalsystems.bankwallet.modules.confirm.ErrorBottomSheetScreen
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.modules.receive.ActivateTokenError
import io.horizontalsystems.bankwallet.modules.receive.ActivateTokenViewModel
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HFillSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsImageCircle
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantError
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import io.horizontalsystems.bankwallet.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class ActivateTokenScreen(val wallet: Wallet) : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        ActivateTokenScreen(backStack, wallet, resultBus)
    }

    data class Result(val activated: Boolean)
}

class ActivateTokenFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
//        withInput<Wallet>(navController) {
//            ActivateTokenScreen(navController, it)
//        }
    }

    @Parcelize
    data class Result(val activated: Boolean) : Parcelable
}


@Composable
fun ActivateTokenScreen(
    backStack: NavBackStack<HSScreen>,
    wallet: Wallet,
    resultBus: ResultEventBus
) {
    val viewModel = viewModel<ActivateTokenViewModel>(factory = ActivateTokenViewModel.Factory(wallet))

    val uiState = viewModel.uiState
    val token = uiState.token

    ConfirmTransactionScreen(
        title = stringResource(R.string.Send_Confirmation_Title),
        onClickBack = null,
        onClickFeeSettings = null,
        buttonsSlot = {
            val coroutineScope = rememberCoroutineScope()
            var buttonEnabled by remember { mutableStateOf(true) }
            var buttonTitle by remember { mutableIntStateOf(R.string.Button_Activate) }
            val view = LocalView.current

            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(buttonTitle),
                onClick = {
                    coroutineScope.launch {
                        buttonEnabled = false
                        buttonTitle = R.string.Activate_Activating

                        try {
                            viewModel.activate()

                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                            delay(1200)
                            resultBus.sendResult(result = ActivateTokenScreen.Result(true))
                            backStack.removeLastOrNull()
                        } catch (t: Throwable) {
                            backStack.add(ErrorBottomSheetScreen(t.message ?: t.javaClass.simpleName))
                        }

                        buttonTitle = R.string.Button_Activate
                        buttonEnabled = true
                    }
                },
                enabled = uiState.activateEnabled && buttonEnabled
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
                    subhead1_leah(
                        text = token.coin.code,
                    )
                }
            }
        }

        VSpacer(height = 16.dp)
        SectionUniversalLawrence {
//            TODO("xxx nav3")
//            DataFieldFee(
//                backStack,
//                uiState.feeCoinValue?.getFormattedFull() ?: "---",
//                uiState.feeFiatValue?.getFormattedFull() ?: "---"
//            )
        }

        uiState.error?.let { error ->
            VSpacer(16.dp)
            val modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)

            when (error) {
                is ActivateTokenError.AlreadyActive -> {
                    TextImportantError(
                        modifier = modifier,
                        text = stringResource(R.string.Activate_AlreadyActive_Description),
                        title = stringResource(R.string.Activate_AlreadyActive_Title),
                        icon = R.drawable.ic_attention_20
                    )
                }

                is ActivateTokenError.NullAdapter -> {
                    TextImportantError(
                        modifier = modifier,
                        text = stringResource(R.string.Error_ParameterNotSet),
                        title = null,
                        icon = null
                    )
                }

                is ActivateTokenError.InsufficientBalance -> {
                    TextImportantError(
                        modifier = modifier,
                        title = stringResource(R.string.Activate_InsufficientBalance_Title),
                        text = stringResource(R.string.Activate_InsufficientBalance_Description),
                        icon = R.drawable.ic_attention_20
                    )
                }
            }
        }
    }
}