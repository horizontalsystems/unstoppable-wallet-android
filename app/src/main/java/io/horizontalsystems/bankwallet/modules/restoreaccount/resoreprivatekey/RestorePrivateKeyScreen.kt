package io.horizontalsystems.bankwallet.modules.restoreaccount.resoreprivatekey

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.restoreaccount.restore.RestoreByMenu
import io.horizontalsystems.bankwallet.modules.restoreaccount.restore.RestoreViewModel
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*

@Composable
fun ResorePrivateKey(
    navController: NavController,
    restoreViewModel: RestoreViewModel,
) {
    val viewModel =
        viewModel<RestorePrivateKeyViewModel>(factory = RestorePrivateKeyModule.Factory())
    val context = LocalContext.current

    var textState by rememberSaveable("", stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    val qrScannerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scannedText = result.data?.getStringExtra(ModuleField.SCAN_ADDRESS) ?: ""

                textState =
                    textState.copy(text = scannedText, selection = TextRange(scannedText.length))
                viewModel.onEnterPrivateKey(scannedText)
            }
        }

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = TranslatableString.ResString(R.string.Restore_Enter_Key_Title),
                navigationIcon = {
                    HsIconButton(onClick = navController::popBackStack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Next),
                        onClick = {
                            viewModel::onProceed
                        }
                    )
                )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))

            RestoreByMenu(restoreViewModel)

            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(8.dp))
                    .background(ComposeAppTheme.colors.lawrence),
            ) {

                val style = SpanStyle(
                    color = ComposeAppTheme.colors.lucian,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    letterSpacing = 0.sp
                )

                BasicTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 68.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    enabled = true,
                    value = textState,
                    onValueChange = {
                        textState = it
                        viewModel.onEnterPrivateKey(it.text)
                    },
                    textStyle = ColoredTextStyle(
                        color = ComposeAppTheme.colors.leah,
                        textStyle = ComposeAppTheme.typography.body
                    ),
                    maxLines = 6,
                    cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
                    visualTransformation = {
                        try {
                            val annotatedString = buildAnnotatedString {
                                append(it.text)

//                                    uiState.invalidWordRanges.forEach { range ->
//                                        addStyle(style = style, range.first, range.last + 1)
//                                    }
                            }
                            TransformedText(annotatedString, OffsetMapping.Identity)
                        } catch (error: Throwable) {
                            error.printStackTrace()
                            TransformedText(it, OffsetMapping.Identity)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    decorationBox = { innerTextField ->
                        if (textState.text.isEmpty()) {
                            body_grey50(
                                stringResource(R.string.Restore_PrivateKeyHint),
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    },
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (textState.text.isNotEmpty()) {
                        ButtonSecondaryCircle(
                            modifier = Modifier.padding(end = 16.dp),
                            icon = R.drawable.ic_delete_20,
                            onClick = {
                                textState = textState.copy(text = "", selection = TextRange(0))
                                viewModel.onEnterPrivateKey("")
                            }
                        )
                    } else {
                        ButtonSecondaryCircle(
                            modifier = Modifier.padding(end = 8.dp),
                            icon = R.drawable.ic_qr_scan_20,
                            onClick = {
                                qrScannerLauncher.launch(
                                    QRScannerActivity.getScanQrIntent(context)
                                )
                            }
                        )

                        val clipboardManager = LocalClipboardManager.current
                        ButtonSecondaryDefault(
                            modifier = Modifier.padding(end = 16.dp),
                            title = stringResource(id = R.string.Send_Button_Paste),
                            onClick = {
                                clipboardManager.getText()?.text?.let { textInClipboard ->
                                    textState = textState.copy(
                                        text = textInClipboard,
                                        selection = TextRange(textInClipboard.length)
                                    )
                                    viewModel.onEnterPrivateKey(textInClipboard)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
