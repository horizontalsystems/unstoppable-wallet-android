package io.horizontalsystems.bankwallet.modules.multiswap.history

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.modules.multiswap.providers.SwapProviderInfoManager
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.extensions.HSBottomSheet
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellGroup
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellLeftImage
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellSecondary
import io.horizontalsystems.bankwallet.uiv3.components.cell.ImageType
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
class RequestRefundSheet(val input: Input) : HSBottomSheet() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        RequestRefundScreen(navController, input.data)
    }

    @Serializable
    data class Input(val data: RequestRefundData)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestRefundScreen(navController: HSNavigation, data: RequestRefundData) {
    val context = LocalContext.current
    val view = LocalView.current

    BottomSheetContent(
        onDismissRequest = {
            navController.removeLastOrNull()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        buttons = {
            HSButton(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.SwapInfo_CopyDetails),
                variant = ButtonVariant.Primary,
                enabled = data.emailBody.isNotEmpty(),
                onClick = {
                    TextHelper.copyText(data.emailBody)
                    HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                },
            )
        },
        content = {
            BottomSheetHeaderV3(
                title = stringResource(R.string.SwapInfo_RequestRefund),
            )
            TextBlock(stringResource(R.string.SwapInfo_RequestRefundDescription))
            VSpacer(8.dp)
            CellGroup(paddingValues = PaddingValues(horizontal = 16.dp)) {
                CellSecondary(
                    middle = {
                        CellMiddleInfo(subtitle = stringResource(R.string.SwapInfo_SwapId).hs)
                    },
                    right = {
                        CellRightInfo(titleSubheadSb = data.swapIdShort.hs)
                    },
                )
                CellSecondary(
                    middle = {
                        CellMiddleInfo(subtitle = stringResource(R.string.SwapInfo_Amount).hs)
                    },
                    right = {
                        CellRightInfo(titleSubheadSb = data.amount.hs)
                    },
                )
                CellSecondary(
                    middle = {
                        CellMiddleInfo(subtitle = stringResource(R.string.SwapInfo_RefundAddress).hs)
                    },
                    right = {
                        CellRightInfo(titleSubheadSb = data.refundAddressShort.hs)
                    },
                )
            }
            if (data.contactLinks.isNotEmpty()) {
                VSpacer(8.dp)
                CellGroup(paddingValues = PaddingValues(horizontal = 16.dp)) {
                    data.contactLinks.forEachIndexed { index, link ->
                        if (index > 0) HsDivider()
                        CellPrimary(
                            left = {
                                CellLeftImage(
                                    painter = painterResource(link.iconRes),
                                    type = ImageType.Rectangle,
                                    size = 24,
                                )
                            },
                            middle = {
                                CellMiddleInfo(
                                    subtitle = link.label.hs(color = ComposeAppTheme.colors.leah),
                                    description = link.value.hs,
                                )
                            },
                            right = {
                                CellRightNavigation()
                            },
                            onClick = {
                                handleContactClick(context, view, link, data.emailSubject, data.emailBody)
                            },
                        )
                    }
                }
            }
        },
    )
}

private fun handleContactClick(
    context: Context,
    view: View,
    link: ContactLink,
    subject: String,
    body: String,
) {
    when (link.type) {
        ContactType.Email -> openEmail(context, link.rawValue, subject, body)
        ContactType.Telegram -> openTelegram(context, link.rawValue, body, view)
        ContactType.Twitter,
        ContactType.Website -> LinkHelper.openLinkInAppBrowser(context, link.rawValue)
    }
}

private fun openEmail(context: Context, email: String, subject: String, body: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No email app installed — silently ignore
    }
}

private fun openTelegram(context: Context, telegramValue: String, body: String, view: View) {
    // Pre-copy the body so the user can paste it into the provider's chat.
    TextHelper.copyText(body)
    LinkHelper.openTelegram(context, telegramValue)
}

enum class ContactType { Email, Telegram, Twitter, Website }

@Parcelize
data class ContactLink(
    val type: ContactType,
    val label: String,
    val value: String,
    val rawValue: String,
    val iconRes: Int,
) : Parcelable

@Parcelize
data class RequestRefundData(
    val swapIdShort: String,
    val amount: String,
    val refundAddressShort: String,
    val emailSubject: String,
    val emailBody: String,
    val contactLinks: List<ContactLink>,
) : Parcelable

object RequestRefundDataLoader {
    // Loads everything the refund bottom sheet needs (including the provider contacts network
    // call) BEFORE the sheet is shown, so it opens at its final height with no flicker.
    suspend fun load(
        recordId: Int,
        swapRecordManager: SwapRecordManager = App.swapRecordManager,
        swapProviderInfoManager: SwapProviderInfoManager = App.swapProviderInfoManager,
    ): RequestRefundData? {
        val record = swapRecordManager.getById(recordId) ?: return null

        val swapId = record.providerSwapId ?: record.transactionHash.orEmpty()
        val amountValue = "${record.amountIn} ${record.tokenInCoinCode}"
        val refundAddress = record.sourceAddress.orEmpty()

        val emailBody = buildEmailBody(
            swapId = swapId,
            fromAsset = record.tokenInCoinCode,
            toAsset = record.tokenOutCoinCode,
            amount = amountValue,
            txHash = record.transactionHash.orEmpty(),
        )

        val providerName = record.providerId.removePrefix("u_")
        val contacts = swapProviderInfoManager.getInfo(providerName)?.contacts
        val contactLinks = buildList {
            contacts?.telegram?.takeIf { it.isNotBlank() }?.let {
                add(ContactLink(ContactType.Telegram, "Telegram", it, it, R.drawable.ic_telegram_24))
            }
            contacts?.twitter?.takeIf { it.isNotBlank() }?.let {
                add(ContactLink(ContactType.Twitter, "Twitter", it, it, R.drawable.ic_twitter_24))
            }
            contacts?.email?.takeIf { it.isNotBlank() }?.let {
                add(ContactLink(ContactType.Email, "Email", it, it, R.drawable.ic_mail_24))
            }
            contacts?.website?.takeIf { it.isNotBlank() }?.let {
                add(ContactLink(ContactType.Website, "Website", it, it, R.drawable.ic_globe))
            }
        }

        return RequestRefundData(
            swapIdShort = swapId.shorten(),
            amount = amountValue,
            refundAddressShort = refundAddress.shorten(),
            emailSubject = "Refund Request - $swapId",
            emailBody = emailBody,
            contactLinks = contactLinks,
        )
    }
}

private fun buildEmailBody(
    swapId: String,
    fromAsset: String,
    toAsset: String,
    amount: String,
    txHash: String,
): String = """
    Hello,
    My swap was stopped after the deposit was received. I would like to request a refund.

    Swap ID: $swapId
    From: $fromAsset
    To: $toAsset
    Amount: $amount
    Deposit transaction: $txHash

    Please let me know if any additional information is required.
    Thank you.
""".trimIndent()
