package com.quantum.wallet.bankwallet.modules.activatetoken

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
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.alternativeImageUrl
import com.quantum.wallet.bankwallet.core.badge
import com.quantum.wallet.bankwallet.core.iconPlaceholder
import com.quantum.wallet.bankwallet.core.imageUrl
import com.quantum.wallet.bankwallet.core.setNavigationResultX
import com.quantum.wallet.bankwallet.core.slideFromBottom
import com.quantum.wallet.bankwallet.entities.Wallet
import com.quantum.wallet.bankwallet.modules.confirm.ConfirmTransactionScreen
import com.quantum.wallet.bankwallet.modules.confirm.ErrorBottomSheet
import com.quantum.wallet.bankwallet.modules.multiswap.ui.DataFieldFee
import com.quantum.wallet.bankwallet.modules.receive.ActivateTokenError
import com.quantum.wallet.bankwallet.modules.receive.ActivateTokenViewModel
import com.quantum.wallet.bankwallet.ui.compose.components.ButtonPrimaryYellow
import com.quantum.wallet.bankwallet.ui.compose.components.HFillSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.HSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.HsImageCircle
import com.quantum.wallet.bankwallet.ui.compose.components.TextImportantError
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.caption_grey
import com.quantum.wallet.bankwallet.ui.compose.components.cell.CellUniversal
import com.quantum.wallet.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import com.quantum.wallet.bankwallet.ui.compose.components.subhead1_leah
import com.quantum.wallet.bankwallet.ui.compose.components.subhead2_leah
import com.quantum.wallet.core.helpers.HudHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class ActivateTokenFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Wallet>(navController) {
            ActivateTokenScreen(navController, it)
        }
    }

    @Parcelize
    data class Result(val activated: Boolean) : Parcelable
}


@Composable
fun ActivateTokenScreen(
    navController: NavController,
    wallet: Wallet,
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
                            navController.setNavigationResultX(ActivateTokenFragment.Result(true))
                            navController.popBackStack()
                        } catch (t: Throwable) {
                            navController.slideFromBottom(R.id.errorBottomSheet, ErrorBottomSheet.Input(t.message ?: t.javaClass.simpleName))
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
            DataFieldFee(
                navController,
                uiState.feeCoinValue?.getFormattedFull() ?: "---",
                uiState.feeFiatValue?.getFormattedFull() ?: "---"
            )
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