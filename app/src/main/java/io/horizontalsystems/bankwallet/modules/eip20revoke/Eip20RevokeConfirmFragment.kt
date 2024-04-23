package io.horizontalsystems.bankwallet.modules.eip20revoke

import android.os.Parcelable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.evmfee.Cautions
import io.horizontalsystems.bankwallet.modules.evmfee.FeeSettingsInfoDialog
import io.horizontalsystems.bankwallet.modules.multiswap.QuoteInfoRow
import io.horizontalsystems.bankwallet.modules.multiswap.TokenRow
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.TransactionInfoAddressCell
import io.horizontalsystems.bankwallet.ui.compose.components.TransactionInfoContactCell
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.cell.BoxBorderedTop
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

class Eip20RevokeConfirmFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        Eip20RevokeScreen(navController, navController.requireInput())
    }

    @Parcelize
    data class Input(
        val token: Token,
        val spenderAddress: String,
        val allowance: BigDecimal,
    ) : Parcelable

    @Parcelize
    data class Result(val revoked: Boolean) : Parcelable
}

@Composable
fun Eip20RevokeScreen(navController: NavController, input: Eip20RevokeConfirmFragment.Input) {
    val currentBackStackEntry = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry(R.id.eip20RevokeConfirmFragment)
    }
    val viewModel = viewModel<Eip20RevokeConfirmViewModel>(
        viewModelStoreOwner = currentBackStackEntry,
        factory = Eip20RevokeConfirmViewModel.Factory(input.token, input.spenderAddress, input.allowance)
    )

    val uiState = viewModel.uiState

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(R.string.Swap_Confirm_Title),
                navigationIcon = {
                    HsBackButton(onClick = navController::popBackStack)
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Settings_Title),
                        icon = R.drawable.ic_manage_2_24,
                        onClick = {
                            navController.slideFromRight(R.id.eip20RevokeTransactionSettingsFragment)
                        }
                    ),
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = {
                            navController.popBackStack()
                        }
                    )
                ),
            )
        },
        bottomBar = {
            val coroutineScope = rememberCoroutineScope()
            var buttonEnabled by remember { mutableStateOf(true) }
            val view = LocalView.current

            ButtonsGroupWithShade {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ButtonPrimaryYellow(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.Swap_Revoke),
                        onClick = {
                            coroutineScope.launch {
                                buttonEnabled = false
                                HudHelper.showInProcessMessage(view, R.string.Swap_Revoking, SnackbarDuration.INDEFINITE)

                                val result = try {
                                    viewModel.revoke()

                                    HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                                    delay(1200)
                                    Eip20RevokeConfirmFragment.Result(true)
                                } catch (t: Throwable) {
                                    HudHelper.showErrorMessage(view, t.javaClass.simpleName)
                                    Eip20RevokeConfirmFragment.Result(false)
                                }

                                buttonEnabled = true
                                navController.setNavigationResultX(result)
                                navController.popBackStack()
                            }
                        },
                        enabled = uiState.approveEnabled && buttonEnabled
                    )
                    VSpacer(16.dp)
                    ButtonPrimaryDefault(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.Button_Cancel),
                        onClick = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        },
        backgroundColor = ComposeAppTheme.colors.tyler,
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
        ) {
            VSpacer(height = 12.dp)

            SectionUniversalLawrence {
                TokenRow(
                    token = uiState.token,
                    amount = uiState.allowance,
                    fiatAmount = uiState.fiatAmount,
                    currency = uiState.currency,
                    borderTop = false,
                    title = stringResource(R.string.Approve_YouRevoke),
                    amountColor = ComposeAppTheme.colors.leah
                )

                BoxBorderedTop {
                    TransactionInfoAddressCell(
                        title = stringResource(R.string.Approve_Spender),
                        value = uiState.spenderAddress,
                        showAdd = uiState.contact == null,
                        blockchainType = uiState.token.blockchainType,
                        navController = navController
                    )
                }

                uiState.contact?.let {
                    BoxBorderedTop {
                        TransactionInfoContactCell(it.name)
                    }
                }
            }

            VSpacer(height = 16.dp)
            SectionUniversalLawrence {
                QuoteInfoRow(
                    title = {
                        val title = stringResource(id = R.string.FeeSettings_NetworkFee)
                        val infoText = stringResource(id = R.string.FeeSettings_NetworkFee_Info)

                        subhead2_grey(text = title)

                        Image(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .clickable(
                                    onClick = {
                                        navController.slideFromBottom(
                                            R.id.feeSettingsInfoDialog,
                                            FeeSettingsInfoDialog.Input(title, infoText)
                                        )
                                    },
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                )
                            ,
                            painter = painterResource(id = R.drawable.ic_info_20),
                            contentDescription = ""
                        )

                    },
                    value = {
                        val primary = uiState.networkFee?.primary?.getFormattedPlain() ?: "---"
                        val secondary = uiState.networkFee?.secondary?.getFormattedPlain() ?: "---"

                        Column(horizontalAlignment = Alignment.End) {
                            subhead2_leah(text = primary)
                            VSpacer(height = 1.dp)
                            subhead2_grey(text = secondary)
                        }
                    }
                )
            }

            if (uiState.cautions.isNotEmpty()) {
                Cautions(cautions = uiState.cautions)
            }

            VSpacer(height = 32.dp)
        }
    }
}
