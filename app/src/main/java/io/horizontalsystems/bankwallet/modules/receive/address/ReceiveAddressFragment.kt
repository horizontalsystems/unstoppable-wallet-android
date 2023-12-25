package io.horizontalsystems.bankwallet.modules.receive.address

import android.content.Intent
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsTextButton
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantError
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey50
import io.horizontalsystems.bankwallet.ui.compose.components.body_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.title3_leah
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.parcelable
import java.math.BigDecimal

class ReceiveAddressFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val wallet = arguments?.parcelable<Wallet>(WALLET_KEY)
        if (wallet == null) {
            Toast.makeText(App.instance, "Wallet parameter is missing", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
            return
        }

        val viewContent = LocalContext.current

        val viewModel by viewModels<ReceiveAddressViewModel> {
            ReceiveAddressModule.Factory(wallet)
        }

        ReceiveAddressScreen(
            title = stringResource(R.string.Deposit_Title, wallet.coin.code),
            uiState = viewModel.uiState,
            onErrorClick = { viewModel.onErrorClick() },
            setAmount = { amount -> viewModel.setAmount(amount) },
            navController = navController,
            onShareClick = { address ->
                viewContent.startActivity(Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, address)
                    type = "text/plain"
                })
            },
        )
    }

    companion object {
        const val WALLET_KEY = "wallet_key"

        fun params(wallet: Wallet): Bundle {
            return bundleOf(
                WALLET_KEY to wallet,
            )
        }
    }

}

@Composable
fun ReceiveAddressScreen(
    title: String,
    uiState: ReceiveAddressModule.UiState,
    navController: NavController,
    setAmount: (BigDecimal?) -> Unit,
    onErrorClick: () -> Unit = {},
    onShareClick: (String) -> Unit,
) {
    val localView = LocalView.current
    val openAmountDialog = remember { mutableStateOf(false) }

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = title,
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            Crossfade(uiState.viewState, label = "") { viewState ->
                Column {
                    when (viewState) {
                        is ViewState.Error -> {
                            ListErrorView(stringResource(R.string.SyncError), onErrorClick)
                        }

                        ViewState.Loading -> {
                            Loading()
                        }

                        ViewState.Success -> {
                            val qrCodeBitmap = TextHelper.getQrCodeBitmap(uiState.uri)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                VSpacer(12.dp)
                                WarningTextView(uiState.alertText)
                                VSpacer(12.dp)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(ComposeAppTheme.colors.lawrence),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                TextHelper.copyText(uiState.uri)
                                                HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
                                            },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        VSpacer(32.dp)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(ComposeAppTheme.colors.white)
                                                .size(224.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            qrCodeBitmap?.let { qrCode ->
                                                Image(
                                                    modifier = Modifier
                                                        .padding(8.dp)
                                                        .fillMaxSize(),
                                                    bitmap = qrCode.asImageBitmap(),
                                                    contentScale = ContentScale.FillWidth,
                                                    contentDescription = null
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(ComposeAppTheme.colors.white)
                                                        .size(64.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Image(
                                                        modifier = Modifier.size(48.dp),
                                                        painter = adaptiveIconPainterResource(
                                                            id = R.mipmap.launcher_main,
                                                            fallbackDrawable = R.drawable.launcher_main_preview
                                                        ),
                                                        contentDescription = null
                                                    )
                                                }
                                            }
                                        }
                                        VSpacer(12.dp)
                                        subhead2_leah(
                                            modifier = Modifier.padding(horizontal = 32.dp),
                                            text = uiState.address,
                                            textAlign = TextAlign.Center,
                                        )
                                        VSpacer(12.dp)
                                        subhead2_grey(
                                            modifier = Modifier.padding(horizontal = 32.dp),
                                            text = uiState.networkName,
                                            textAlign = TextAlign.Center,
                                        )
                                        VSpacer(24.dp)
                                    }
                                    if (uiState.additionalItems.isNotEmpty()) {
                                        AdditionalDataSection(
                                            items = uiState.additionalItems,
                                            onClearAmount = { setAmount(null) },
                                            showAccountNotActiveWarningDialog = {
                                                val args = NotActiveWarningDialog.prepareParams(
                                                    Translator.getString(R.string.Tron_AddressNotActive_Title),
                                                    Translator.getString(R.string.Tron_AddressNotActive_Info),
                                                )
                                                navController.slideFromBottom(R.id.notActiveAccountDialog, args)
                                            }
                                        )
                                    }
                                }

                                VSpacer(52.dp)

                                ActionButtonsRow(
                                    uri = uiState.uri,
                                    watchAccount = uiState.watchAccount,
                                    openAmountDialog = openAmountDialog,
                                    onShareClick = onShareClick,
                                )

                                VSpacer(32.dp)
                            }
                        }
                    }
                }
            }
            if (openAmountDialog.value) {
                AmountInputDialog(
                    initialAmount = uiState.amount,
                    onDismissRequest = { openAmountDialog.value = false },
                    onAmountConfirm = { amount ->
                        setAmount(amount)
                        openAmountDialog.value = false
                    }
                )
            }
        }
    }
}

@Composable
private fun WarningTextView(
    alertText: ReceiveAddressModule.AlertText
) {
    when (alertText) {
        is ReceiveAddressModule.AlertText.Critical -> {
            TextImportantError(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = alertText.content,
            )
        }

        is ReceiveAddressModule.AlertText.Normal -> {
            TextImportantWarning(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = alertText.content,
            )
        }
    }
}

@Composable
private fun ActionButtonsRow(
    uri: String,
    watchAccount: Boolean,
    openAmountDialog: MutableState<Boolean>,
    onShareClick: (String) -> Unit,
) {
    val localView = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp),
        horizontalArrangement = if (watchAccount) Arrangement.Center else Arrangement.SpaceBetween,
    ) {
        val itemModifier = if(watchAccount) Modifier else Modifier.weight(1f)
        if (!watchAccount) {
            ReceiveActionButton(
                modifier = itemModifier,
                icon = R.drawable.ic_edit_24px,
                buttonText = stringResource(R.string.Button_SetAmount),
                onClick = {
                    openAmountDialog.value = true
                },
            )
        }
        ReceiveActionButton(
            modifier = itemModifier,
            icon = R.drawable.ic_share_24px,
            buttonText = stringResource(R.string.Button_Share),
            onClick = {
                onShareClick.invoke(uri)
            },
        )
        if (watchAccount) {
            HSpacer(64.dp)
        }
        ReceiveActionButton(
            modifier = itemModifier,
            icon = R.drawable.ic_copy_24px,
            buttonText = stringResource(R.string.Button_Copy),
            onClick = {
                TextHelper.copyText(uri)
                HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
            },
        )
    }
}

@Composable
private fun AdditionalDataSection(
    items: List<ReceiveAddressModule.AdditionalData>,
    onClearAmount: () -> Unit,
    showAccountNotActiveWarningDialog: () -> Unit,
) {
    val localView = LocalView.current

    items.forEach { item ->
        Divider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel20
        )
        RowUniversal(
            modifier = Modifier.height(48.dp),
        ) {
            when (item) {
                is ReceiveAddressModule.AdditionalData.Amount -> {
                    subhead2_grey(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .weight(1f),
                        text = stringResource(R.string.Balance_Receive_Amount),
                    )
                    subhead1_leah(
                        text = item.value,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    ButtonSecondaryCircle(
                        modifier = Modifier.padding(end = 16.dp),
                        icon = R.drawable.ic_delete_20,
                        onClick = onClearAmount
                    )
                }

                is ReceiveAddressModule.AdditionalData.Memo -> {
                    subhead2_grey(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .weight(1f),
                        text = stringResource(R.string.Balance_Receive_Memo),
                    )
                    subhead1_leah(
                        text = item.value,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    ButtonSecondaryCircle(
                        modifier = Modifier.height(28.dp).padding(end = 16.dp),
                        icon = R.drawable.ic_copy_20,
                        onClick = {
                            TextHelper.copyText(item.value)
                            HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
                        }
                    )
                }

                is ReceiveAddressModule.AdditionalData.AccountNotActive -> {
                    subhead2_grey(
                        modifier = Modifier.padding(start = 16.dp),
                        text = stringResource(R.string.Balance_Receive_Account),
                    )
                    HSpacer(8.dp)
                    HsIconButton(
                        modifier = Modifier.size(20.dp),
                        onClick = showAccountNotActiveWarningDialog
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_info_20),
                            contentDescription = null
                        )
                    }
                    subhead1_jacob(
                        text = stringResource(R.string.Balance_Receive_NotActive),
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .weight(1f)
                    )
                }
            }
        }
    }
}


@Composable
private fun ReceiveActionButton(
    modifier: Modifier,
    icon: Int,
    buttonText: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ButtonPrimaryCircle(
            icon = icon,
            onClick = onClick,
        )
        caption_grey(
            modifier = Modifier.padding(top = 8.dp),
            text = buttonText,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun AmountInputDialog(
    initialAmount: BigDecimal? = null,
    onDismissRequest: () -> Unit,
    onAmountConfirm: (BigDecimal?) -> Unit
) {
    val textState = remember { mutableStateOf(TextFieldValue(text = initialAmount?.toString() ?: "")) }
    val focusRequester = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(ComposeAppTheme.colors.lawrence)
        ) {
            VSpacer(24.dp)
            title3_leah(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                text = stringResource(R.string.Balance_Receive_SetAmount),
            )
            VSpacer(16.dp)

            BasicTextField(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .focusRequester(focusRequester),
                value = textState.value,
                onValueChange = { value ->
                    textState.value = value
                },
                singleLine = true,
                textStyle = ColoredTextStyle(
                    color = ComposeAppTheme.colors.leah,
                    textStyle = ComposeAppTheme.typography.body
                ),
                decorationBox = { innerTextField ->
                    if (textState.value.text.isEmpty()) {
                        body_grey50("0")
                    }
                    innerTextField()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
            )
            SideEffect {
                focusRequester.requestFocus()
                textState.value = textState.value.copy(
                    selection = TextRange(textState.value.text.length)
                )
            }

            Divider(
                modifier = Modifier.padding(horizontal = 24.dp),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.jacob
            )

            Row(
                modifier = Modifier
                    .padding(all = 24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                HsTextButton(
                    onClick = onDismissRequest
                ) {
                    body_jacob(stringResource(R.string.Button_Cancel).uppercase())
                }
                HSpacer(8.dp)
                HsTextButton(
                    onClick = { onAmountConfirm.invoke(textState.value.text.toBigDecimalOrNull()) }
                ) {
                    body_jacob(stringResource(R.string.Button_Confirm).uppercase())
                }
            }
        }
    }
}

@Composable
fun adaptiveIconPainterResource(@DrawableRes id: Int, @DrawableRes fallbackDrawable: Int): Painter {
    val res = LocalContext.current.resources
    val theme = LocalContext.current.theme

    val adaptiveIcon = ResourcesCompat.getDrawable(res, id, theme) as? AdaptiveIconDrawable
    return if (adaptiveIcon != null) {
        BitmapPainter(adaptiveIcon.toBitmap().asImageBitmap())
    } else {
        painterResource(fallbackDrawable)
    }
}