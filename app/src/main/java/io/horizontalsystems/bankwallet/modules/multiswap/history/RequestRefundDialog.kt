package io.horizontalsystems.bankwallet.modules.multiswap.history

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.modules.multiswap.providers.SwapProviderInfoManager
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
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
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class RequestRefundDialog : BaseComposableBottomSheetFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                val navController = findNavController()
                val recordId = navController.getInput<Input>()?.recordId

                ComposeAppTheme {
                    RequestRefundScreen(navController, recordId)
                }
            }
        }
    }

    @Parcelize
    data class Input(val recordId: Int) : Parcelable
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestRefundScreen(navController: NavController, recordId: Int?) {
    val viewModel = viewModel<RequestRefundViewModel>(
        key = recordId?.toString() ?: "preview",
        factory = RequestRefundViewModel.Factory(recordId),
    )
    val uiState = viewModel.uiState
    val context = LocalContext.current
    val view = LocalView.current

    BottomSheetContent(
        onDismissRequest = {
            navController.popBackStack()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        buttons = {
            HSButton(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.SwapInfo_CopyDetails),
                variant = ButtonVariant.Primary,
                enabled = uiState.emailBody.isNotEmpty(),
                onClick = {
                    TextHelper.copyText(uiState.emailBody)
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
                        CellRightInfo(titleSubheadSb = uiState.swapIdShort.hs)
                    },
                )
                CellSecondary(
                    middle = {
                        CellMiddleInfo(subtitle = stringResource(R.string.SwapInfo_Amount).hs)
                    },
                    right = {
                        CellRightInfo(titleSubheadSb = uiState.amount.hs)
                    },
                )
                CellSecondary(
                    middle = {
                        CellMiddleInfo(subtitle = stringResource(R.string.SwapInfo_RefundAddress).hs)
                    },
                    right = {
                        CellRightInfo(titleSubheadSb = uiState.refundAddressShort.hs)
                    },
                )
            }
            if (uiState.contactLinks.isNotEmpty()) {
                VSpacer(8.dp)
                CellGroup(paddingValues = PaddingValues(horizontal = 16.dp)) {
                    uiState.contactLinks.forEachIndexed { index, link ->
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
                                handleContactClick(context, view, link, uiState.emailSubject, uiState.emailBody)
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

private fun openTelegram(context: Context, telegramUrl: String, body: String, view: View) {
    // Pre-copy the body so the user can paste it into the provider's chat,
    // then open the provider's Telegram link.
    TextHelper.copyText(body)
    HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
    LinkHelper.openLinkInAppBrowser(context, telegramUrl)
}

enum class ContactType { Email, Telegram, Twitter, Website }

data class ContactLink(
    val type: ContactType,
    val label: String,
    val value: String,
    val rawValue: String,
    val iconRes: Int,
)

data class RequestRefundUiState(
    val swapIdShort: String,
    val amount: String,
    val refundAddressShort: String,
    val emailSubject: String,
    val emailBody: String,
    val contactLinks: List<ContactLink>,
)

class RequestRefundViewModel(
    private val recordId: Int?,
    private val swapRecordManager: SwapRecordManager,
    private val swapProviderInfoManager: SwapProviderInfoManager,
) : ViewModelUiState<RequestRefundUiState>() {
    private var swapIdShort: String = ""
    private var amount: String = ""
    private var refundAddressShort: String = ""
    private var emailSubject: String = ""
    private var emailBody: String = ""
    private var contactLinks: List<ContactLink> = emptyList()

    override fun createState() = RequestRefundUiState(
        swapIdShort = swapIdShort,
        amount = amount,
        refundAddressShort = refundAddressShort,
        emailSubject = emailSubject,
        emailBody = emailBody,
        contactLinks = contactLinks,
    )

    init {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                load()
            } catch (e: Throwable) {
                Log.e("RequestRefundViewModel", "Failed to load refund details", e)
            }
        }
    }

    private suspend fun load() {
        val id = recordId ?: return
        val record = swapRecordManager.getById(id) ?: return

        val swapId = record.providerSwapId ?: record.transactionHash.orEmpty()
        val fromAsset = record.tokenInCoinCode
        val toAsset = record.tokenOutCoinCode
        val amountValue = "${record.amountIn} ${record.tokenInCoinCode}"
        val txHash = record.transactionHash.orEmpty()
        val refundAddress = record.sourceAddress.orEmpty()

        swapIdShort = swapId.shorten()
        amount = amountValue
        refundAddressShort = refundAddress.shorten()
        emailSubject = "Refund Request - $swapId"
        emailBody = buildEmailBody(
            swapId = swapId,
            fromAsset = fromAsset,
            toAsset = toAsset,
            amount = amountValue,
            txHash = txHash,
        )

        val providerName = record.providerId.removePrefix("u_")
        val contacts = swapProviderInfoManager.getInfo(providerName)?.contacts
        contactLinks = buildList {
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

        emitState()
    }

    class Factory(private val recordId: Int?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RequestRefundViewModel(
                recordId = recordId,
                swapRecordManager = App.swapRecordManager,
                swapProviderInfoManager = App.swapProviderInfoManager,
            ) as T
        }
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
