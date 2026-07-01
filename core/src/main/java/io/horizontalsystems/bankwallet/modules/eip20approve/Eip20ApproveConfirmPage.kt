package io.horizontalsystems.bankwallet.modules.eip20approve

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.core.R
import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.confirm.ConfirmTransactionScreen
import io.horizontalsystems.bankwallet.modules.confirm.ErrorSheet
import io.horizontalsystems.bankwallet.modules.eip20approve.AllowanceMode.OnlyRequired
import io.horizontalsystems.bankwallet.modules.eip20approve.AllowanceMode.Unlimited
import io.horizontalsystems.bankwallet.modules.evmfee.Cautions
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldFeeTemplate
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.rememberAsyncAction
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightControlsButtonText
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellSecondary
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.delay
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data object Eip20ApproveConfirmPage : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        Eip20ApproveConfirmScreen(navigation)
    }

    @Parcelize
    data class Result(val approved: Boolean) : Parcelable
}

@Composable
fun Eip20ApproveConfirmScreen(navigation: HSNavigation) {
    val resultEventBus = LocalResultEventBus.current
    val viewModel = navigation.viewModelForScreen<Eip20ApproveViewModel>(Eip20ApprovePage::class)

    val view = LocalView.current
    val uiState = viewModel.uiState

    ConfirmTransactionScreen(
        title = stringResource(R.string.Swap_ConfirmApprove_Title),
        initialLoading = uiState.initialLoading,
        onClickBack = navigation::removeLastOrNull,
        onClickFeeSettings = {
            navigation.slideFromRight(Eip20ApproveTransactionSettingsPage)
        },
        buttonsSlot = {
            val approveAction = rememberAsyncAction()

            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(if (approveAction.inProgress) R.string.Swap_Approving else R.string.Swap_Approve),
                onClick = {
                    approveAction.run {
                        try {
                            viewModel.approve()

                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                            delay(1200)
                            resultEventBus.sendResult(Eip20ApproveConfirmPage.Result(true))
                            navigation.removeLastOrNull()
                        } catch (t: Throwable) {
                            navigation.slideFromBottom(ErrorSheet(
                                ErrorSheet.Input(t.message ?: t.javaClass.simpleName)
                            ))
                        }
                    }
                },
                enabled = !approveAction.inProgress && uiState.approveEnabled
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
                navigation = navigation,
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