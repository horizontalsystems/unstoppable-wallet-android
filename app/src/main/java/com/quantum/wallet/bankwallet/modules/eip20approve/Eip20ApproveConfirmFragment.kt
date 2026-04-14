package com.quantum.wallet.bankwallet.modules.eip20approve

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.badge
import com.quantum.wallet.bankwallet.core.setNavigationResultX
import com.quantum.wallet.bankwallet.core.slideFromBottom
import com.quantum.wallet.bankwallet.core.slideFromRight
import com.quantum.wallet.bankwallet.entities.CoinValue
import com.quantum.wallet.bankwallet.entities.Currency
import com.quantum.wallet.bankwallet.entities.CurrencyValue
import com.quantum.wallet.bankwallet.modules.confirm.ConfirmTransactionScreen
import com.quantum.wallet.bankwallet.modules.confirm.ErrorBottomSheet
import com.quantum.wallet.bankwallet.modules.eip20approve.AllowanceMode.OnlyRequired
import com.quantum.wallet.bankwallet.modules.eip20approve.AllowanceMode.Unlimited
import com.quantum.wallet.bankwallet.modules.evmfee.Cautions
import com.quantum.wallet.bankwallet.modules.multiswap.ui.DataFieldFeeTemplate
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.components.ButtonPrimaryYellow
import com.quantum.wallet.bankwallet.ui.compose.components.CoinImage
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.ui.helpers.TextHelper
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellMiddleInfo
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellPrimary
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellRightControlsButtonText
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellRightInfo
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellSecondary
import com.quantum.wallet.bankwallet.uiv3.components.cell.hs
import com.quantum.wallet.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

class Eip20ApproveConfirmFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        Eip20ApproveConfirmScreen(navController)
    }

    @Parcelize
    data class Result(val approved: Boolean) : Parcelable
}

@Composable
fun Eip20ApproveConfirmScreen(navController: NavController) {
    val viewModelStoreOwner = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry(R.id.eip20ApproveFragment)
    }
    val viewModel = viewModel<Eip20ApproveViewModel>(
        viewModelStoreOwner = viewModelStoreOwner,
    )

    val view = LocalView.current
    val uiState = viewModel.uiState

    ConfirmTransactionScreen(
        title = stringResource(R.string.Swap_ConfirmApprove_Title),
        initialLoading = uiState.initialLoading,
        onClickBack = navController::popBackStack,
        onClickFeeSettings = {
            navController.slideFromRight(R.id.eip20ApproveTransactionSettingsFragment)
        },
        buttonsSlot = {
            val coroutineScope = rememberCoroutineScope()
            var buttonEnabled by remember { mutableStateOf(true) }
            var buttonTitle by remember { mutableIntStateOf(R.string.Swap_Approve) }

            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(buttonTitle),
                onClick = {
                    coroutineScope.launch {
                        buttonEnabled = false
                        buttonTitle = R.string.Swap_Approving

                        try {
                            viewModel.approve()

                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                            delay(1200)
                            navController.setNavigationResultX(Eip20ApproveConfirmFragment.Result(true))
                            navController.popBackStack()
                        } catch (t: Throwable) {
                            navController.slideFromBottom(R.id.errorBottomSheet, ErrorBottomSheet.Input(t.message ?: t.javaClass.simpleName))
                        }

                        buttonTitle = R.string.Swap_Approve
                        buttonEnabled = true
                    }
                },
                enabled = uiState.approveEnabled && buttonEnabled
            )
        }
    ) {
        when (uiState.allowanceMode) {
            OnlyRequired -> {
                ConfirmTokenSection(
                    token = uiState.token,
                    amount = uiState.requiredAllowance,
                    fiatAmount = uiState.fiatAmount,
                    currency = uiState.currency,
                )
            }

            Unlimited -> {
                ConfirmTokenUnlimitedSection(uiState.token)
            }
        }

        VSpacer(16.dp)

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ComposeAppTheme.colors.lawrence)
                .padding(vertical = 8.dp)
        ) {
            SpenderCell(
                address = uiState.spenderAddress,
                contact = uiState.contact?.name,
                onCopyClick = {
                    TextHelper.copyText(uiState.spenderAddress)
                    HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                }
            )

            DataFieldFeeTemplate(
                navController = navController,
                primary = uiState.networkFee?.primary?.getFormattedPlain() ?: "---",
                secondary = uiState.networkFee?.secondary?.getFormattedPlain(),
                title = stringResource(id = R.string.FeeSettings_NetworkFee),
                infoText = stringResource(id = R.string.FeeSettings_NetworkFee_Info)
            )
        }

        if (uiState.cautions.isNotEmpty()) {
            Cautions(cautions = uiState.cautions)
        }
    }
}

@Composable
private fun ConfirmTokenUnlimitedSection(token: Token) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence)
    ) {
        CellPrimary(
            left = {
                CoinImage(
                    coin = token.coin,
                    modifier = Modifier.size(32.dp)
                )
            },
            middle = {
                CellMiddleInfo(
                    subtitle = token.coin.code.hs(color = ComposeAppTheme.colors.leah),
                    description = (token.badge ?: stringResource(id = R.string.CoinPlatforms_Native)).hs
                )
            },
            right = {
                CellRightInfo(
                    titleSubheadSb = stringResource(R.string.Swap_Approve_Unlimited).hs,
                )
            }
        )
    }
}

@Composable
fun ConfirmTokenSection(
    token: Token,
    amount: BigDecimal,
    fiatAmount: BigDecimal?,
    currency: Currency,
) {
    val coinAmount = CoinValue(token, amount).getFormatted()
    val fiatAmountFormatted =
        fiatAmount?.let { CurrencyValue(currency, fiatAmount).getFormattedFull() }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence)
    ) {
        CellPrimary(
            left = {
                CoinImage(
                    coin = token.coin,
                    modifier = Modifier.size(32.dp)
                )
            },
            middle = {
                CellMiddleInfo(
                    subtitle = token.coin.code.hs(color = ComposeAppTheme.colors.leah),
                    description = (token.badge ?: stringResource(id = R.string.CoinPlatforms_Native)).hs
                )
            },
            right = {
                CellRightInfo(
                    titleSubheadSb = coinAmount.hs,
                    description = fiatAmountFormatted?.hs
                )
            }
        )
    }
}

@Composable
fun SpenderCell(
    address: String,
    contact: String?,
    onCopyClick: () -> Unit
) {
    CellSecondary(
        middle = {
            CellMiddleInfo(
                eyebrow = stringResource(R.string.Approve_Spender).hs,
            )
        },
        right = {
            CellRightControlsButtonText(
                subtitle = (contact ?: address).hs,
                icon = painterResource(id = R.drawable.copy_filled_24),
                iconTint = ComposeAppTheme.colors.leah,
                onIconClick = onCopyClick
            )
        }
    )
}