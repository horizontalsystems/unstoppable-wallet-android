package io.horizontalsystems.bankwallet.modules.qrscanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.CompoundBarcodeView
import com.journeyapps.barcodescanner.ScanOptions
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Dark
import io.horizontalsystems.bankwallet.ui.compose.DisposableLifecycleCallbacks
import io.horizontalsystems.bankwallet.ui.compose.SteelLight
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper

class QRScannerActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            QRScannerScreen(
                showPasteButton = intent.getBooleanExtra(SHOW_PASTE_BUTTON, false),
                onScan = { onScan(it) },
                onCloseClick = { finish() },
                onCameraPermissionSettingsClick = { openCameraPermissionSettings() }
            )
        }
        setStatusBarTransparent()
    }

    private fun onScan(address: String?) {
        setResult(RESULT_OK, Intent().apply {
            putExtra(ModuleField.SCAN_ADDRESS, address)
        })
        //slow down fast transition to new window
        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 1000)
    }

    private fun openCameraPermissionSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    companion object {
        private const val SHOW_PASTE_BUTTON = "show_paste_button_key"

        fun getScanQrIntent(context: Context, showPasteButton: Boolean = false): Intent {
            val options = ScanOptions()
            options.captureActivity = QRScannerActivity::class.java
            options.setOrientationLocked(true)
            options.setPrompt("")
            options.setBeepEnabled(false)
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            val intent = options.createScanIntent(context)
            intent.putExtra(SHOW_PASTE_BUTTON, showPasteButton)
            intent.putExtra(Intents.Scan.SCAN_TYPE, Intents.Scan.MIXED_SCAN)
            return intent
        }
    }

}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun QRScannerScreen(
    showPasteButton: Boolean,
    onScan: (String) -> Unit,
    onCloseClick: () -> Unit,
    onCameraPermissionSettingsClick: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var showPermissionNeededDialog by remember { mutableStateOf(cameraPermissionState.status != PermissionStatus.Granted) }

    if (showPermissionNeededDialog) {
        PermissionNeededDialog(
            onOkClick = {
                cameraPermissionState.launchPermissionRequest()
                showPermissionNeededDialog = false
            },
            onCancelClick = {
                showPermissionNeededDialog = false
            }
        )
    }

    ComposeAppTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(color = ComposeAppTheme.colors.tyler),
            contentAlignment = Alignment.Center
        ) {
            if (cameraPermissionState.status == PermissionStatus.Granted) {
                ScannerView(onScan)
            } else {
                Spacer(
                    Modifier
                        .fillMaxSize()
                        .background(color = ComposeAppTheme.colors.dark)
                )
                GoToSettingsBox(onCameraPermissionSettingsClick)
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))
                if (showPasteButton) {
                    ButtonPrimaryYellow(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.Send_Button_Paste),
                        onClick = { onScan(TextHelper.getCopiedText()) }
                    )
                    Spacer(Modifier.height(16.dp))
                }
                ButtonPrimary(
                    modifier = Modifier.fillMaxWidth(),
                    content = {
                        Text(
                            text = stringResource(R.string.Button_Cancel),
                            maxLines = 1,
                            color = Dark,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    buttonColors = ButtonPrimaryDefaults.textButtonColors(
                        backgroundColor = SteelLight,
                        contentColor = ComposeAppTheme.colors.dark,
                        disabledBackgroundColor = ComposeAppTheme.colors.steel20,
                        disabledContentColor = ComposeAppTheme.colors.grey50,
                    ),
                    onClick = onCloseClick
                )
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun ScannerView(onScan: (String) -> Unit) {
    val context = LocalContext.current
    val barcodeView = remember {
        CompoundBarcodeView(context).apply {
            this.initializeFromIntent((context as Activity).intent)
            this.setStatusText("")
            this.decodeSingle { result ->
                result.text?.let { barCodeOrQr ->
                    onScan.invoke(barCodeOrQr)
                }
            }
        }
    }
    AndroidView(factory = { barcodeView })
    DisposableLifecycleCallbacks(
        onResume = barcodeView::resume,
        onPause = barcodeView::pause
    )
}

@Composable
private fun GoToSettingsBox(onCameraPermissionSettingsClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        subhead2_grey(
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp),
            text = stringResource(R.string.ScanQr_CameraPermissionDeniedText)
        )
        Spacer(Modifier.height(24.dp))
        TextPrimaryButton(
            onClick = onCameraPermissionSettingsClick,
            title = stringResource(R.string.ScanQr_GoToSettings)
        )
    }
}

@Composable
private fun TextPrimaryButton(
    title: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        color = ComposeAppTheme.colors.transparent,
        contentColor = SteelLight,
    ) {
        Row(
            Modifier
                .defaultMinSize(
                    minWidth = ButtonPrimaryDefaults.MinWidth,
                    minHeight = ButtonPrimaryDefaults.MinHeight
                )
                .padding(ButtonPrimaryDefaults.ContentPadding)
                .clickable(
                    onClick = onClick,
                    interactionSource = interactionSource,
                    indication = null
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = ComposeAppTheme.typography.headline2
                )
            }
        )
    }
}

@Composable
private fun PermissionNeededDialog(
    onOkClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    ComposeAppTheme {
        Dialog(onDismissRequest = onCancelClick) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(color = ComposeAppTheme.colors.lawrence)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                title3_leah(text = stringResource(R.string.ScanQr_CameraPermission_Title))
                Spacer(Modifier.height(12.dp))
                body_leah(text = stringResource(R.string.ScanQr_PleaseGrantCameraPermission))
                Spacer(Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    ButtonPrimaryTransparent(
                        onClick = onCancelClick,
                        title = stringResource(R.string.Button_Cancel)
                    )
                    Spacer(Modifier.width(8.dp))
                    ButtonPrimaryYellow(
                        onClick = onOkClick,
                        title = stringResource(R.string.Button_Ok)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview_PermissionNeededDialog() {
    ComposeAppTheme {
        PermissionNeededDialog({}, {})
    }
}
