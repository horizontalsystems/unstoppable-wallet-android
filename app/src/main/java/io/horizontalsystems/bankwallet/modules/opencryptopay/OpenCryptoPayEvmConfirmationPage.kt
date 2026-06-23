package io.horizontalsystems.bankwallet.modules.opencryptopay

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.confirm.ConfirmTransactionScreen
import io.horizontalsystems.bankwallet.modules.confirm.ErrorSheet
import io.horizontalsystems.bankwallet.modules.evmfee.Cautions
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldFee
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
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
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class OpenCryptoPayEvmConfirmationPage(val input: Input) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        OpenCryptoPayEvmConfirmationScreen(navigation, input)
    }

    @Serializable
    data class Input(
        val wallet: Wallet,
        val callbackUrl: String,
        val quoteId: String,
        val paymentId: String,
        val method: String,
        val asset: String,
        val assetAmount: String,
        val blockchainType: BlockchainType,
        val merchant: String?,
        val expirationIso: String,
        val minFee: Double?,
        val sendEntryPointDestId: KClass<out HSPage>,
    )
}

@Composable
private fun OpenCryptoPayEvmConfirmationScreen(
    navigation: HSNavigation,
    input: OpenCryptoPayEvmConfirmationPage.Input,
) {
    val viewModel = viewModel<OpenCryptoPayEvmConfirmationViewModel>(
        factory = OpenCryptoPayEvmConfirmationViewModel.Factory(
            wallet = input.wallet,
            callbackUrl = input.callbackUrl,
            quoteId = input.quoteId,
            paymentId = input.paymentId,
            method = input.method,
            asset = input.asset,
            assetAmount = input.assetAmount,
            blockchainType = input.blockchainType,
            merchant = input.merchant,
            expirationIso = input.expirationIso,
            minFee = input.minFee,
        )
    )
    val uiState = viewModel.uiState

    ConfirmTransactionScreen(
        title = stringResource(R.string.Send_Confirmation_Title),
        initialLoading = uiState.initialLoading,
        onClickBack = { navigation.removeLastOrNull() },
        onClickFeeSettings = {
            navigation.slideFromBottom(OpenCryptoPayEvmSettingsPage)
        },
        onClickNonceSettings = {
            navigation.slideFromBottom(OpenCryptoPayEvmNonceSettingsPage)
        },
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
                            navigation.removeLastUntil(input.sendEntryPointDestId, true)
                        } catch (t: Throwable) {
                            navigation.slideFromBottom(
                                ErrorSheet(ErrorSheet.Input(t.message ?: t.javaClass.simpleName))
                            )
                        }
                    }
                },
                enabled = !isExpired && !payAction.inProgress && uiState.payEnabled,
            )
        }
    ) {
        // Section 1: Token + address
        uiState.sectionViewItems.forEach { section ->
            SectionView(section.viewItems, navigation, StatPage.SendConfirmation)
            VSpacer(16.dp)
        }

        // Section 2: Merchant + URL
        val merchantRows = buildList<@Composable () -> Unit> {
            uiState.merchant?.let { name ->
                add {
                    RowUniversal(modifier = Modifier.padding(horizontal = 16.dp)) {
                        subhead2_grey(text = stringResource(R.string.OpenCryptoPay_Merchant))
                        Spacer(Modifier.weight(1f))
                        subhead1_leah(
                            text = name,
                            textAlign = TextAlign.End,
                        )
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

        // Section 3: Expires In + Fee
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
                        navigation,
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
