package io.horizontalsystems.bankwallet.modules.qrscanner

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.zxing.MultiFormatReader
import com.google.zxing.client.android.DecodeFormatManager
import com.google.zxing.client.android.DecodeHintManager
import com.google.zxing.client.android.Intents
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.journeyapps.barcodescanner.camera.CameraSettings
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import kotlinx.android.synthetic.main.activity_qr_scanner.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

class QRScannerActivity : AppCompatActivity() {

    private val callback = BarcodeCallback {
        barcodeView.pause()
        //slow down fast transition to new window
        Handler(Looper.getMainLooper()).postDelayed({
            onScan(it.text)
        }, 1000)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_qr_scanner)

        setSupportActionBar(toolbar)
        supportActionBar?.title = ""

        val oldFlags = window.decorView.systemUiVisibility
        window.decorView.systemUiVisibility = oldFlags or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        cancelButton.setOnClickListener {
            onBackPressed()
        }

        barcodeView.decodeSingle(callback)

        initializeFromIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        openCameraWithPermission()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    @AfterPermissionGranted(REQUEST_CAMERA_PERMISSION)
    private fun openCameraWithPermission() {
        val perms = arrayOf(Manifest.permission.CAMERA)
        if (EasyPermissions.hasPermissions(this, *perms)) {
            barcodeView.resume()
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.ScanQr_PleaseGrantCameraPermission),
                    REQUEST_CAMERA_PERMISSION, *perms)
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

        // Check what type of scan. Default: normal scan
        val scanType = intent.getIntExtra(Intents.Scan.SCAN_TYPE, 0)
        val characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET)
        val reader = MultiFormatReader()
        reader.setHints(decodeHints)
        barcodeView.cameraSettings = settings
        barcodeView.decoderFactory = DefaultDecoderFactory(
                decodeFormats,
                decodeHints,
                characterSet,
                scanType
        )
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1

        fun getIntentForFragment(fragment: Fragment): Intent {
            val intentIntegrator = IntentIntegrator.forSupportFragment(fragment)
            intentIntegrator.captureActivity = QRScannerActivity::class.java
            intentIntegrator.setOrientationLocked(true)
            intentIntegrator.setPrompt("")
            intentIntegrator.setBeepEnabled(false)
            intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            return intentIntegrator.createScanIntent()
        }
    }

}
