package io.horizontalsystems.bankwallet.modules.opencryptopay

import android.os.Parcelable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.confirm.ConfirmTransactionScreen
import io.horizontalsystems.bankwallet.modules.confirm.ErrorBottomSheet
import io.horizontalsystems.bankwallet.modules.evmfee.Cautions
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldFee
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SectionView
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.rememberAsyncAction
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_lucian
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay
import kotlinx.parcelize.Parcelize

class OpenCryptoPayConfirmationFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            OpenCryptoPayConfirmationScreen(navController, input)
        }
    }

    @Parcelize
    data class Input(
        val wallet: Wallet,
        val callbackUrl: String,
        val quoteId: String,
        val paymentId: String,
        val method: String,
        val asset: String,
        val assetAmount: String,
        val merchant: String?,
        val expirationIso: String,
        val minFee: Double?,
        val sendEntryPointDestId: Int,
    ) : Parcelable
}

@Composable
private fun OpenCryptoPayConfirmationScreen(
    navController: NavController,
    input: OpenCryptoPayConfirmationFragment.Input,
) {
    val backStackEntry = remember {
        navController.getBackStackEntry(R.id.openCryptoPayConfirmationFragment)
    }
    val viewModel = viewModel<OpenCryptoPayConfirmationViewModel>(
        viewModelStoreOwner = backStackEntry,
        factory = OpenCryptoPayConfirmationViewModel.Factory(
            wallet = input.wallet,
            callbackUrl = input.callbackUrl,
            quoteId = input.quoteId,
            paymentId = input.paymentId,
            method = input.method,
            asset = input.asset,
            assetAmount = input.assetAmount,
            merchant = input.merchant,
            expirationIso = input.expirationIso,
            minFee = input.minFee,
        )
    )
    val uiState = viewModel.uiState

    ConfirmTransactionScreen(
        title = stringResource(R.string.Send_Confirmation_Title),
        initialLoading = uiState.initialLoading,
        onClickBack = { navController.popBackStack() },
        onClickFeeSettings = null,
        onClickNonceSettings = null,
        buttonsSlot = {
            val view = LocalView.current
            val payAction = rememberAsyncAction()
            val isExpired = uiState.secondsUntilExpiry == 0

            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp),
                title = when {
                    isExpired -> stringResource(R.string.OpenCryptoPay_Expired)
                    payAction.inProgress -> stringResource(R.string.Send_Sending)
                    else -> stringResource(R.string.Send_Confirmation_Send_Button)
                },
                onClick = {
                    payAction.run {
                        try {
                            viewModel.pay()
                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                            delay(1200)
                            navController.popBackStack(input.sendEntryPointDestId, true)
                        } catch (t: Throwable) {
                            navController.slideFromBottom(
                                R.id.errorBottomSheet,
                                ErrorBottomSheet.Input(t.message ?: t.javaClass.simpleName)
                            )
                        }
                    }
                },
                enabled = !isExpired && !payAction.inProgress && uiState.payEnabled,
            )
        }
    ) {
        uiState.sectionViewItems.forEach { section ->
            SectionView(section.viewItems, navController, StatPage.SendConfirmation)
            VSpacer(16.dp)
        }

        val merchantRows = buildList<@Composable () -> Unit> {
            uiState.merchant?.let { name ->
                add {
                    RowUniversal(modifier = Modifier.padding(horizontal = 16.dp)) {
                        subhead2_grey(text = stringResource(R.string.OpenCryptoPay_Merchant))
                        Spacer(Modifier.weight(1f))
                        subhead1_leah(text = name, textAlign = TextAlign.End)
                    }
                }
            }
            if (uiState.url.isNotBlank()) {
                add {
                    RowUniversal(modifier = Modifier.padding(horizontal = 16.dp)) {
                        subhead2_grey(text = stringResource(R.string.OpenCryptoPay_URL))
                        Spacer(Modifier.weight(1f))
                        subhead1_leah(
                            modifier = Modifier.weight(1f, fill = false),
                            text = uiState.url,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        if (merchantRows.isNotEmpty()) {
            CellUniversalLawrenceSection(merchantRows)
            VSpacer(16.dp)
        }

        CellUniversalLawrenceSection(
            buildList<@Composable () -> Unit> {
                uiState.secondsUntilExpiry?.let { secs ->
                    add {
                        RowUniversal(modifier = Modifier.padding(horizontal = 16.dp)) {
                            subhead2_grey(text = stringResource(R.string.OpenCryptoPay_ExpiresIn))
                            Spacer(Modifier.weight(1f))
                            if (secs == 0) {
                                subhead1_lucian(text = formatExpiry(secs))
                            } else {
                                subhead1_jacob(text = formatExpiry(secs))
                            }
                        }
                    }
                }
                add {
                    DataFieldFee(
                        navController,
                        uiState.networkFee?.primary?.getFormattedPlain() ?: "---",
                        uiState.networkFee?.secondary?.getFormattedPlain(),
                    )
                }
            }
        )

        if (uiState.cautions.isNotEmpty()) {
            VSpacer(16.dp)
            Cautions(uiState.cautions)
        }
    }
}

private fun formatExpiry(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${s}s"
        else -> "${s}s"
    }
}
