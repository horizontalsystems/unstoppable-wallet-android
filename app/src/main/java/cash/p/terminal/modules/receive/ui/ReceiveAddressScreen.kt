package cash.p.terminal.modules.receive.ui

import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import cash.p.terminal.R
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.modules.receive.ReceiveModule
import cash.p.terminal.modules.receive.ReceiveModule.UiState
import cash.p.terminal.modules.receive.UsedAddressesRow
import cash.p.terminal.modules.receive.viewmodels.AddressBadge
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui.compose.components.HsTextButton
import cash.p.terminal.ui.compose.components.ListErrorView
import cash.p.terminal.ui.helpers.TextHelper
import cash.p.terminal.ui_compose.BottomSheetHeader
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryCircle
import cash.p.terminal.ui_compose.components.ButtonPrimaryTransparent
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.ButtonSecondaryCircle
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.HsDivider
import cash.p.terminal.ui_compose.components.HsIconButton
import cash.p.terminal.ui_compose.components.HudHelper
import cash.p.terminal.ui_compose.components.InfoText
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.TextImportantError
import cash.p.terminal.ui_compose.components.TextImportantWarning
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_grey50
import cash.p.terminal.ui_compose.components.body_jacob
import cash.p.terminal.ui_compose.components.caption_grey
import cash.p.terminal.ui_compose.components.subhead1_jacob
import cash.p.terminal.ui_compose.components.subhead1_leah
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui_compose.components.title3_leah
import cash.p.terminal.ui_compose.entities.ViewState
import cash.p.terminal.ui_compose.theme.ColoredTextStyle
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.entities.UsedAddress
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import io.github.alexzhirkevich.qrose.options.QrBallShape
import io.github.alexzhirkevich.qrose.options.QrErrorCorrectionLevel
import io.github.alexzhirkevich.qrose.options.QrFrameShape
import io.github.alexzhirkevich.qrose.options.QrLogoPadding
import io.github.alexzhirkevich.qrose.options.QrLogoShape
import io.github.alexzhirkevich.qrose.options.QrPixelShape
import io.github.alexzhirkevich.qrose.options.roundCorners
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveAddressScreen(
    title: String,
    uiState: ReceiveModule.AbstractUiState,
    setAmount: (BigDecimal?) -> Unit,
    onErrorClick: () -> Unit = {},
    topContent: @Composable () -> Unit = {},
    addressBadge: @Composable () -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
    onBackPress: () -> Unit,
    closeModule: () -> Unit,
) {
    val localView = LocalView.current
    val openAmountDialog = remember { mutableStateOf(false) }
    val tronAlertSheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tronInfoSheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var isTronAlertVisible by remember { mutableStateOf(false) }
    var isTronInfoVisible by remember { mutableStateOf(false) }

    if (uiState is UiState) {
        LaunchedEffect(uiState.showTronAlert) {
            if (uiState.showTronAlert) {
                isTronAlertVisible = true
                scope.launch {
                    delay(2000)
                    tronAlertSheetState.show()
                }
            }
        }
    }

    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = title,
                navigationIcon = {
                    HsBackButton(onClick = onBackPress)
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Done),
                        onClick = closeModule,
                        tint = ComposeAppTheme.colors.jacob
                    )
                )
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
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                VSpacer(12.dp)
                                uiState.alertText?.let {
                                    WarningTextView(it)
                                }

                                if (uiState.watchAccount) {
                                    TextImportantWarning(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        text = stringResource(R.string.Balance_Receive_WatchAddressAlert),
                                    )
                                }

                                VSpacer(16.dp)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(ComposeAppTheme.colors.lawrence),
                                ) {
                                    topContent()
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                TextHelper.copyText(uiState.uri)
                                                HudHelper.showSuccessMessage(
                                                    localView,
                                                    R.string.Hud_Text_Copied
                                                )
                                            },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        VSpacer(16.dp)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(ComposeAppTheme.colors.white)
                                                .size(QR_CODE_SIZE),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            QrCodeImage(uiState.uri)
                                        }
                                        VSpacer(12.dp)
                                        addressBadge()
                                        subhead2_leah(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 32.dp),
                                            text = uiState.address,
                                            textAlign = TextAlign.Start,
                                        )

                                        val testNetBadge =
                                            if (!uiState.mainNet) " (TestNet)" else ""
                                        uiState.blockchainName?.let { blockchainName ->
                                            VSpacer(12.dp)
                                            subhead2_grey(
                                                modifier = Modifier.padding(horizontal = 32.dp),
                                                text = stringResource(R.string.Balance_Network) + ": " + blockchainName + testNetBadge,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                        uiState.addressFormat?.let { addressFormat ->
                                            VSpacer(12.dp)
                                            subhead2_grey(
                                                modifier = Modifier.padding(horizontal = 32.dp),
                                                text = stringResource(R.string.Balance_Format) + ": " + addressFormat + testNetBadge,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                        VSpacer(16.dp)
                                    }
                                    val additionalItems = buildList {
                                        addAll(uiState.additionalItems)
                                        uiState.amount?.let {
                                            add(ReceiveModule.AdditionalData.Amount(it.toString()))
                                        }
                                    }

                                    if (additionalItems.isNotEmpty()) {
                                        AdditionalDataSection(
                                            items = additionalItems,
                                            onClearAmount = {
                                                setAmount(null)
                                            },
                                            showAccountNotActiveWarningDialog = {
                                                isTronInfoVisible = true
                                            }
                                        )
                                    }

                                    ActionButtonsRow(
                                        uri = uiState.uri,
                                        watchAccount = uiState.watchAccount,
                                        openAmountDialog = openAmountDialog,
                                    )
                                    VSpacer(16.dp)

                                    bottomContent()
                                }
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
    if (isTronInfoVisible) {
        TronInfoBottomSheet(
            title = stringResource(R.string.Tron_AddressNotActive_Title),
            text = stringResource(R.string.Tron_AddressNotActive_Info),
            hideBottomSheet = {
                scope.launch { tronInfoSheetState.hide() }
                isTronInfoVisible = false
            },
            bottomSheetState = tronInfoSheetState
        )
    }
    if (isTronAlertVisible) {
        TronAlertBottomSheet(
            title = stringResource(R.string.Tron_AddressNotActive_Title),
            text = stringResource(R.string.Tron_AddressNotActive_Info),
            hideBottomSheet = {
                scope.launch { tronAlertSheetState.hide() }
                isTronAlertVisible = false
            },
            bottomSheetState = tronAlertSheetState,
        )
    }
}

@Composable
private fun QrCodeImage(address: String) {
    val quietZone = remember(address) { calculateQuietZone(address) }
    val logoPainter: Painter =
        adaptiveIconPainterResource(
            id = R.mipmap.launcher_main,
            fallbackDrawable = R.drawable.launcher_main_preview
        )
    val qrcodePainter: Painter =
        rememberQrCodePainter(address) {
            errorCorrectionLevel = QrErrorCorrectionLevel.MediumHigh
            logo {
                painter = logoPainter
                padding = QrLogoPadding.Natural(.25f)
                shape = QrLogoShape.roundCorners(0.8f)
                size = 0.2f
            }

            shapes {
                ball = QrBallShape.roundCorners(.25f)
                darkPixel = QrPixelShape.roundCorners()
                frame = QrFrameShape.roundCorners(.25f)
            }
        }
    Image(
        painter = qrcodePainter,
        modifier = Modifier
            .padding(quietZone)
            .fillMaxSize(),
        contentScale = ContentScale.FillWidth,
        contentDescription = null
    )
}

private val QR_CODE_SIZE = 300.dp
private val DEFAULT_QUIET_ZONE = 8.dp

private fun calculateQuietZone(content: String): Dp {
    return runCatching {
        val modules = Encoder.encode(content, ErrorCorrectionLevel.Q).matrix?.width
            ?.takeIf { it > 0 } ?: return DEFAULT_QUIET_ZONE
        val moduleSize = QR_CODE_SIZE.value / modules
        (moduleSize * QUIET_ZONE_MODULES).dp
    }.getOrElse { DEFAULT_QUIET_ZONE }
        .coerceAtLeast(DEFAULT_QUIET_ZONE)
}

private const val QUIET_ZONE_MODULES = 4f

@Composable
private fun WarningTextView(
    alertText: ReceiveModule.AlertText
) {
    when (alertText) {
        is ReceiveModule.AlertText.Critical -> {
            TextImportantError(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = alertText.content,
            )
        }

        is ReceiveModule.AlertText.Normal -> Unit
    }
}

@Composable
private fun ActionButtonsRow(
    uri: String,
    watchAccount: Boolean,
    openAmountDialog: MutableState<Boolean>,
) {
    val localView = LocalView.current
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = if (watchAccount) Arrangement.Center else Arrangement.SpaceBetween,
    ) {
        val itemModifier = if (watchAccount) Modifier else Modifier.weight(1f)
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
            icon = R.drawable.ic_copy_24px,
            buttonText = stringResource(R.string.Button_Copy),
            onClick = {
                TextHelper.copyText(uri)
                HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
            },
        )

        if (watchAccount) {
            HSpacer(64.dp)
        }

        ReceiveActionButton(
            modifier = itemModifier,
            icon = R.drawable.ic_share_24px,
            buttonText = stringResource(R.string.Button_Share),
            onClick = {
                context.startActivity(Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, uri)
                    type = "text/plain"
                })
            },
        )
    }
}

@Composable
private fun AdditionalDataSection(
    items: List<ReceiveModule.AdditionalData>,
    onClearAmount: () -> Unit,
    showAccountNotActiveWarningDialog: () -> Unit,
) {
    val localView = LocalView.current

    items.forEach { item ->
        HsDivider(modifier = Modifier.fillMaxWidth())
        RowUniversal(
            modifier = Modifier.height(48.dp),
        ) {
            when (item) {
                is ReceiveModule.AdditionalData.Amount -> {
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

                is ReceiveModule.AdditionalData.Memo -> {
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
                        modifier = Modifier
                            .height(28.dp)
                            .padding(end = 16.dp),
                        icon = R.drawable.ic_copy_20,
                        onClick = {
                            TextHelper.copyText(item.value)
                            HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
                        }
                    )
                }

                is ReceiveModule.AdditionalData.AccountNotActive -> {
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
    val textState =
        remember { mutableStateOf(TextFieldValue(text = initialAmount?.toString() ?: "")) }
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

            HsDivider(modifier = Modifier.padding(horizontal = 24.dp))

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TronAlertBottomSheet(
    title: String,
    text: String,
    hideBottomSheet: () -> Unit,
    bottomSheetState: SheetState,
) {
    ModalBottomSheet(
        onDismissRequest = hideBottomSheet,
        sheetState = bottomSheetState,
        containerColor = ComposeAppTheme.colors.transparent
    ) {
        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.ic_attention_24),
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
            title = title,
            onCloseClick = hideBottomSheet
        ) {
            TextImportantWarning(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                text = text
            )

            VSpacer(12.dp)
            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(R.string.Button_Understand),
                onClick = hideBottomSheet
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TronInfoBottomSheet(
    title: String,
    text: String,
    hideBottomSheet: () -> Unit,
    bottomSheetState: SheetState,
) {
    ModalBottomSheet(
        onDismissRequest = hideBottomSheet,
        sheetState = bottomSheetState,
        containerColor = ComposeAppTheme.colors.transparent
    ) {
        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.ic_info_24),
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.grey),
            title = title,
            onCloseClick = hideBottomSheet
        ) {
            InfoText(text)

            Spacer(Modifier.height(64.dp))
        }
    }
}

@Suppress("UnusedPrivateMember", "LongMethod")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ReceiveAddressScreenPreview() {
    val addressBadge = AddressBadge.USED
    ComposeAppTheme {
        ReceiveAddressScreen(
            title = "Receive BTC",
            uiState = UiState(
                viewState = ViewState.Success,
                address = "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
                mainNet = true,
                usedAddresses = listOf(
                    UsedAddress(
                        0,
                        "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
                        "https:/some-url.com",
                    )
                ),
                usedChangeAddresses = listOf(
                    UsedAddress(
                        0,
                        "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
                        "https:/some-url.com",
                    )
                ),
                isAddressHistorySupported = true,
                showTronAlert = false,
                uri = "bitcoin:bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
                blockchainName = "Bitcoin",
                addressFormat = null,
                watchAccount = false,
                additionalItems = emptyList(),
                amount = null,
                alertText = null,
            ),
            topContent = {
                UsedAddressesRow(
                    enabled = true,
                    onClick = {},
                )
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = ComposeAppTheme.colors.steel20,
                )
            },
            addressBadge = {
                val badgeText = when (addressBadge) {
                    AddressBadge.NEW -> stringResource(R.string.receive_address_badge_new)
                    AddressBadge.USED -> stringResource(R.string.receive_address_badge_used)
                    AddressBadge.UNUSED -> stringResource(R.string.receive_address_badge_unused)
                }
                val badgeColor = when (addressBadge) {
                    AddressBadge.NEW -> ComposeAppTheme.colors.remus
                    AddressBadge.USED -> ComposeAppTheme.colors.jacob
                    AddressBadge.UNUSED -> ComposeAppTheme.colors.grey
                }
                VSpacer(12.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                ) {
                    AddressBadgeChip(text = badgeText, color = badgeColor)
                }
                VSpacer(4.dp)
            },
            bottomContent = {
                ButtonPrimaryTransparent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    title = stringResource(R.string.receive_create_new_address),
                    onClick = {},
                )
            },
            setAmount = {},
            onBackPress = {},
            closeModule = {},
        )
    }
}

@Composable
fun adaptiveIconPainterResource(@DrawableRes id: Int, @DrawableRes fallbackDrawable: Int): Painter {
    val context = LocalContext.current
    return remember(id, fallbackDrawable) {
        val adaptiveIcon = ResourcesCompat.getDrawable(context.resources, id, context.theme) as? AdaptiveIconDrawable
        if (adaptiveIcon != null) {
            BitmapPainter(adaptiveIcon.toBitmap().asImageBitmap())
        } else {
            null
        }
    } ?: painterResource(fallbackDrawable)
}
