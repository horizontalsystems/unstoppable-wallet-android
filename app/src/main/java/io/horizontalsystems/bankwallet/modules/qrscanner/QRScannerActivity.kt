package io.horizontalsystems.bankwallet.modules.qrscanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.google.zxing.MultiFormatReader
import com.google.zxing.client.android.DecodeFormatManager
import com.google.zxing.client.android.DecodeHintManager
import com.google.zxing.client.android.Intents
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.journeyapps.barcodescanner.ScanOptions
import com.journeyapps.barcodescanner.camera.CameraSettings
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.databinding.ActivityQrScannerBinding
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Dark
import io.horizontalsystems.bankwallet.ui.compose.SteelLight
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefaults
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimary
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

class QRScannerActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private lateinit var binding: ActivityQrScannerBinding
    private var showPasteButton = false

    private val callback = BarcodeCallback {
        binding.barcodeView.pause()
        //slow down fast transition to new window
        Handler(Looper.getMainLooper()).postDelayed({
            onScan(it.text)
        }, 1000)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = ""

        val oldFlags = window.decorView.systemUiVisibility
        window.decorView.systemUiVisibility =
            oldFlags or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        binding.buttonsCompose.setContent {
            ComposeAppTheme {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
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
                        onClick = { onBackPressed() }
                    )
                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        binding.barcodeView.decodeSingle(callback)

        initializeFromIntent(intent)

        openCameraWithPermission()
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeView.pause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        if (perms.contains(Manifest.permission.CAMERA)) {
            binding.barcodeView.resume()
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        HudHelper.showErrorMessage(
            findViewById(android.R.id.content),
            R.string.ScanQr_NoCameraPermission
        )
    }

    @AfterPermissionGranted(REQUEST_CAMERA_PERMISSION)
    private fun openCameraWithPermission() {
        val perms = arrayOf(Manifest.permission.CAMERA)
        if (EasyPermissions.hasPermissions(this, *perms)) {
            binding.barcodeView.resume()
        } else {
            EasyPermissions.requestPermissions(
                this, getString(R.string.ScanQr_PleaseGrantCameraPermission),
                REQUEST_CAMERA_PERMISSION, *perms
            )
        }
    }

    private fun onScan(address: String?) {
        setResult(RESULT_OK, Intent().apply {
            putExtra(ModuleField.SCAN_ADDRESS, address)
        })
        finish()
    }

    private fun initializeFromIntent(intent: Intent) {
        // Scan the formats the intent requested, and return the result to the calling activity.
        val decodeFormats = DecodeFormatManager.parseDecodeFormats(intent)
        val decodeHints = DecodeHintManager.parseDecodeHints(intent)
        val settings = CameraSettings()
        if (intent.hasExtra(Intents.Scan.CAMERA_ID)) {
            val cameraId =
                intent.getIntExtra(Intents.Scan.CAMERA_ID, -1)
            if (cameraId >= 0) {
                settings.requestedCameraId = cameraId
            }
        }

        showPasteButton = intent.getBooleanExtra(SHOW_PASTE_BUTTON, false)

        // Check what type of scan. Default: normal scan
        val scanType = intent.getIntExtra(Intents.Scan.SCAN_TYPE, 0)
        val characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET)
        val reader = MultiFormatReader()
        reader.setHints(decodeHints)
        binding.barcodeView.cameraSettings = settings
        binding.barcodeView.decoderFactory = DefaultDecoderFactory(
            decodeFormats,
            decodeHints,
            characterSet,
            scanType
        )
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1
        private const val SHOW_PASTE_BUTTON = "show_paste_button_key"

        fun getIntentForFragment(fragment: Fragment, showPasteButton: Boolean = false): Intent {
            val intentIntegrator = IntentIntegrator.forSupportFragment(fragment)
            intentIntegrator.captureActivity = QRScannerActivity::class.java
            intentIntegrator.setOrientationLocked(true)
            intentIntegrator.setPrompt("")
            intentIntegrator.setBeepEnabled(false)
            intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            intentIntegrator.addExtra(Intents.Scan.SCAN_TYPE, Intents.Scan.MIXED_SCAN)
            val intent = intentIntegrator.createScanIntent()
            intent.putExtra(SHOW_PASTE_BUTTON, showPasteButton)
            return intent
        }

        fun getScanQrIntent(context: Context, showPasteButton: Boolean = false): Intent {
            val options = ScanOptions()
            options.setCaptureActivity(QRScannerActivity::class.java)
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
