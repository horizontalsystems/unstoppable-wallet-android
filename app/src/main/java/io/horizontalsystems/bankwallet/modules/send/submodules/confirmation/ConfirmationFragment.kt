package io.horizontalsystems.bankwallet.modules.send.submodules.confirmation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.LocalizedException
import io.horizontalsystems.bankwallet.core.stringResId
import io.horizontalsystems.bankwallet.modules.send.SendPresenter
import io.horizontalsystems.bankwallet.modules.send.SendView
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.snackbar.CustomSnackbar
import io.horizontalsystems.snackbar.SnackbarDuration
import java.net.UnknownHostException

class ConfirmationFragment(sendPresenter: SendPresenter) : BaseFragment() {

    private var sendView: SendView = sendPresenter.view as SendView
    private val viewModel by viewModels<SendConfirmationViewModel> {
        SendConfirmationModule.Factory(sendView.confirmationViewItems.value)
    }
    private val logger = AppLogger("send")
    private var snackbarInProcess: CustomSnackbar? = null

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
                ComposeAppTheme {
                    SendConfirmScreen(
                        viewModel,
                        { parentFragmentManager.popBackStack() },
                        { requireActivity().finish() },
                        { onAddressCopy(it) },
                        { onSendClick() }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sendView.error.observe(viewLifecycleOwner, { errorMsgTextRes ->
            snackbarInProcess?.dismiss()
            errorMsgTextRes?.let {
                HudHelper.showErrorMessage(requireView(), getErrorText(it))
            }
            viewModel.onSendError()
        })
    }

    override fun onDestroyView() {
        snackbarInProcess?.dismiss()
        super.onDestroyView()
    }

    private fun onSendClick() {
        viewModel.onSendClick()
        val logger = logger.getScopedUnique().apply { info("click") }
        sendView.delegate.onSendConfirmed(logger)
        snackbarInProcess = HudHelper.showInProcessMessage(
            requireView(),
            R.string.Send_Sending,
            SnackbarDuration.INDEFINITE
        )
    }

    private fun onAddressCopy(it: String) {
        TextHelper.copyText(it)
        HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Copied)
    }

    private fun getErrorText(error: Throwable): String {
        return when (error) {
            is UnknownHostException -> getString(R.string.Hud_Text_NoInternet)
            is LocalizedException -> getString(error.errorTextRes)
            else -> error.message ?: ""
        }
    }

}

@Composable
fun SendConfirmScreen(
    viewModel: SendConfirmationViewModel,
    onBackButtonClick: () -> Unit,
    onCloseButtonClick: () -> Unit,
    onClickCopy: (String) -> Unit,
    onSendButtonClick: () -> Unit,
) {
    val viewDataState by viewModel.viewDataLiveData.observeAsState()
    val sendButtonState by viewModel.sendButtonLiveData.observeAsState()

    val primarySectionItems = mutableListOf<@Composable () -> Unit>()
    val secondarySectionItems = mutableListOf<@Composable () -> Unit>()

    viewDataState?.let { data ->
        val locked = data.lockTimeInterval != null
        primarySectionItems.add {
            SectionTitleCell(R.string.Send_Confirmation_YouSend, data.coinName)
        }
        primarySectionItems.add {
            ConfirmAmountCell(data.currencyAmount, data.coinAmount, locked)
        }
        primarySectionItems.add {
            AddressCell(data.toAddress, onClickCopy)
        }

        data.memo?.let {
            secondarySectionItems.add {
                MemoCell(it)
            }
        }
        data.lockTimeInterval?.let {
            secondarySectionItems.add {
                ValueCell(
                    stringResource(R.string.Send_Confirmation_LockTime),
                    stringResource(it.stringResId())
                )
            }
        }
    }

    Column(Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = TranslatableString.ResString(R.string.Send_Confirmation_Title),
            navigationIcon = {
                IconButton(onClick = onBackButtonClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "back button",
                        tint = ComposeAppTheme.colors.jacob
                    )
                }
            },
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Close),
                    icon = R.drawable.ic_close,
                    onClick = onCloseButtonClick
                )
            )
        )
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 106.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                CellSingleLineLawrenceSection(primarySectionItems)

                if (secondarySectionItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CellSingleLineLawrenceSection(secondarySectionItems)
                }

                viewDataState?.feeAmount?.let{
                    Spacer(modifier = Modifier.height(20.dp))
                    FeeCell(it)
                }
            }
            sendButtonState?.let{
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    title = stringResource(it.title),
                    onClick = onSendButtonClick,
                    enabled = it.enabled
                )
            }
        }
    }
}

@Composable
private fun SectionTitleCell(title: Int, value: String) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(title),
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah
        )

        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            text = value,
            style = ComposeAppTheme.typography.subhead1,
            color = ComposeAppTheme.colors.grey,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ConfirmAmountCell(fiatAmount: String?, coinAmount: String, locked: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = fiatAmount ?: "",
            style = ComposeAppTheme.typography.subhead2,
            color = ComposeAppTheme.colors.grey
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            text = coinAmount,
            style = ComposeAppTheme.typography.subhead1,
            color = ComposeAppTheme.colors.jacob,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (locked) {
            Icon(
                modifier = Modifier.padding(start = 8.dp),
                painter = painterResource(id = R.drawable.ic_lock_20),
                tint = ComposeAppTheme.colors.grey,
                contentDescription = "lock icon",
            )
        }
    }
}

@Composable
private fun AddressCell(address: String, onClickCopy: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.Send_Confirmation_To),
            style = ComposeAppTheme.typography.subhead2,
            color = ComposeAppTheme.colors.grey
        )
        Spacer(Modifier.weight(1f))
        ButtonSecondaryDefault(
            modifier = Modifier
                .padding(start = 8.dp),
            title = address,
            onClick = { onClickCopy.invoke(address) },
            ellipsis = Ellipsis.Middle(10)
        )
    }
}

@Composable
private fun MemoCell(value: String) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.Send_Confirmation_HintMemo),
            style = ComposeAppTheme.typography.subhead2,
            color = ComposeAppTheme.colors.grey
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = ComposeAppTheme.typography.subheadItalic,
            color = ComposeAppTheme.colors.leah
        )
    }
}

@Composable
private fun ValueCell(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = ComposeAppTheme.typography.subhead2,
            color = ComposeAppTheme.colors.grey
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = ComposeAppTheme.typography.subhead1,
            color = ComposeAppTheme.colors.leah
        )
    }
}

@Composable
private fun FeeCell(value: String) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.Send_Fee),
            style = ComposeAppTheme.typography.subhead2,
            color = ComposeAppTheme.colors.grey
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = ComposeAppTheme.typography.subhead2,
            color = ComposeAppTheme.colors.grey
        )
    }
}

@Composable
private fun TransactionSpeedCell(
    value: String,
    onInfoClick: () -> Unit,
    onSpeedMenuClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            modifier = Modifier.padding(start = 2.dp),
            onClick = onInfoClick
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_info_20),
                tint = ComposeAppTheme.colors.jacob,
                contentDescription = "info icon",
            )
        }
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = stringResource(R.string.Send_DialogSpeed),
            style = ComposeAppTheme.typography.subhead2,
            color = ComposeAppTheme.colors.grey
        )
        Spacer(Modifier.weight(1f))
        ButtonSecondaryTransparent(
            title = value,
            iconRight = R.drawable.ic_down_arrow_20,
            onClick = onSpeedMenuClick
        )
    }
}
