package bitcoin.wallet.core.security

/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
//source https://github.com/googlesamples/android-FingerprintDialog


import android.content.res.ColorStateList
import android.hardware.fingerprint.FingerprintManager
import android.os.CancellationSignal
import android.support.v4.content.ContextCompat
import android.support.v4.widget.ImageViewCompat
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import bitcoin.wallet.R

/**
 * Small helper class to manage text/icon around fingerprint authentication UI.
 */
class FingerprintUiHelper

/**
 * Constructor for [FingerprintUiHelper].
 */
internal constructor(private val fingerprintMgr: FingerprintManager,
                     private val iconBackgroundImg: ImageView,
                     private val errorTextView: TextView,
                     private val fingerprintWrapper: FrameLayout,
                     private val callback: Callback
) : FingerprintManager.AuthenticationCallback() {

    private var cancellationSignal: CancellationSignal? = null
    private var selfCancelled = false

    private val isFingerprintAuthAvailable: Boolean
        get() = fingerprintMgr.isHardwareDetected && fingerprintMgr.hasEnrolledFingerprints()

    private val resetErrorTextRunnable = Runnable {
        setImageTintColor(iconBackgroundImg, R.color.dark)
        errorTextView.run {
            setTextColor(errorTextView.resources.getColor(R.color.dark, null))
            text = errorTextView.resources.getString(R.string.fingerprint_hint)
        }
    }

    private fun setImageTintColor(image: ImageView, colorResource: Int) {
        val color = ContextCompat.getColor(image.context, colorResource)
        ImageViewCompat.setImageTintList(image, ColorStateList.valueOf(color))
    }

    fun startListening(cryptoObject: FingerprintManager.CryptoObject) {
        if (!isFingerprintAuthAvailable) return
        cancellationSignal = CancellationSignal()
        selfCancelled = false
        fingerprintMgr.authenticate(cryptoObject, cancellationSignal, 0, this, null)
    }

    fun stopListening() {
        cancellationSignal?.also {
            selfCancelled = true
            it.cancel()
        }
        cancellationSignal = null
    }

    override fun onAuthenticationError(errMsgId: Int, errString: CharSequence) {
        if (!selfCancelled) {
            showError(errString)
        }
    }

    override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence) =
            showError(helpString)

    override fun onAuthenticationFailed() =
            showError(iconBackgroundImg.resources.getString(R.string.fingerprint_not_recognized))

    override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult) {
        errorTextView.run {
            removeCallbacks(resetErrorTextRunnable)
            setTextColor(errorTextView.resources.getColor(R.color.green_crypto, null))
            text = errorTextView.resources.getString(R.string.fingerprint_success)
        }
        iconBackgroundImg.run {
            setImageTintColor(iconBackgroundImg, R.color.green_crypto)
            postDelayed({ callback.onAuthenticated() }, SUCCESS_DELAY_MILLIS)
        }
    }

    private fun showError(error: CharSequence) {
        setImageTintColor(iconBackgroundImg, R.color.red_warning)
        errorTextView.run {
            text = error
            setTextColor(errorTextView.resources.getColor(R.color.red_warning, null))
            removeCallbacks(resetErrorTextRunnable)
            postDelayed(resetErrorTextRunnable, ERROR_TIMEOUT_MILLIS)
        }
        val shake = AnimationUtils.loadAnimation(iconBackgroundImg.context, R.anim.shake)
        fingerprintWrapper.startAnimation(shake)
    }

    interface Callback {
        fun onAuthenticated()
    }

    companion object {
        const val ERROR_TIMEOUT_MILLIS: Long = 1900
        const val SUCCESS_DELAY_MILLIS: Long = 1300
    }
}
