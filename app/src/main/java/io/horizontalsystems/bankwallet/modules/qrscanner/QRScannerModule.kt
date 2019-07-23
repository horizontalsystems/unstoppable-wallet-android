package io.horizontalsystems.bankwallet.modules.qrscanner

import android.app.Activity
import com.google.zxing.integration.android.IntentIntegrator

object QRScannerModule {
    fun start(context: Activity) {
        val intentIntegrator = IntentIntegrator(context)
        intentIntegrator.captureActivity = QRScannerActivity::class.java
        intentIntegrator.setOrientationLocked(true)
        intentIntegrator.setPrompt("")
        intentIntegrator.setBeepEnabled(false)
        intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        intentIntegrator.initiateScan()
    }
}
