package io.horizontalsystems.bankwallet.core.security

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


import android.app.DialogFragment
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.core.widget.ImageViewCompat
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper


/**
 * A dialog which uses fingerprint APIs to authenticate the user, and falls back to password
 * authentication if fingerprint is not available.
 */
class FingerprintAuthenticationDialogFragment : DialogFragment(), FingerprintCallback {

    private lateinit var cancelButton: Button
    private lateinit var fingerprintWrapper: FrameLayout
    private lateinit var iconBackgroundImg: ImageView
    private lateinit var errorTextView: TextView

    private var callback: Callback? = null
    private var cryptoObject: FingerprintManagerCompat.CryptoObject? = null
    private var authCallbackHandler: FingerprintAuthenticationHandler? = null

     private val ERROR_TIMEOUT_MILLIS: Long = 1900
     private val SUCCESS_DELAY_MILLIS: Long = 100


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        retainInstance = true
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog.window?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.requestFeature(Window.FEATURE_NO_TITLE)
        }
        isCancelable = false
        return inflater.inflate(R.layout.fingerprint_dialog_container, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cancelButton = view.findViewById(R.id.cancel_button)
        fingerprintWrapper = view.findViewById(R.id.fingerprint_wrapper)
        iconBackgroundImg = view.findViewById(R.id.fingerprint_background)
        errorTextView = view.findViewById(R.id.fingerprint_status)

        cancelButton.setOnClickListener { dismiss() }

        authCallbackHandler = cryptoObject?.let { FingerprintAuthenticationHandler(this, it) }
    }

    override fun onPause() {
        super.onPause()
        authCallbackHandler?.stopListening()
    }

    override fun onResume() {
        super.onResume()
        authCallbackHandler?.startListening()

        dialog.setOnKeyListener { _, keyCode, _ ->

            if ((keyCode == KeyEvent.KEYCODE_BACK)) {
                // To dismiss the fragment when the back-button is pressed.
                dismiss()
                true
            }
            // Otherwise, do nothing else
            else false
        }
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        authCallbackHandler?.releaseFingerprintCallback()
    }

    override fun onAuthenticated() {
        errorTextView.run {
            removeCallbacks(resetErrorTextRunnable)
            setTextColor(errorTextView.resources.getColor(R.color.green_crypto, null))
            text = errorTextView.resources.getString(R.string.Fingerprint_Success)
        }
        iconBackgroundImg.run {
            setImageTintColor(iconBackgroundImg, R.color.green_crypto)
            postDelayed({ callback?.onFingerprintAuthSucceed() }, SUCCESS_DELAY_MILLIS)
        }
    }

    override fun onAuthenticationHelp(helpString: CharSequence?) { }

    override fun onAuthenticationFailed() {
        showError(getString(R.string.Fingerprint_NotRecognized))
    }

    override fun onAuthenticationError(errMsgId: Int, errString: CharSequence?) {
        if (errMsgId == FingerprintManager.FINGERPRINT_ERROR_LOCKOUT) {
            HudHelper.showErrorMessage(R.string.Unlock_Page_EnterYourPin)
            dismiss()
        }
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    fun setCryptoObject(cryptoObject: FingerprintManagerCompat.CryptoObject) {
        this.cryptoObject = cryptoObject
    }

    private val resetErrorTextRunnable = Runnable {
        setImageTintColor(iconBackgroundImg, R.color.dark)
        errorTextView.run {
            setTextColor(errorTextView.resources.getColor(R.color.dark, null))
            text = errorTextView.resources.getString(R.string.Fingerprint_Hint)
        }
    }

    private fun showError(error: CharSequence) {
        dialog?.let {
            errorTextView.run {
                text = error
                setTextColor(errorTextView.resources.getColor(R.color.red_warning, null))
                removeCallbacks(resetErrorTextRunnable)
                postDelayed(resetErrorTextRunnable, ERROR_TIMEOUT_MILLIS)
            }

            setImageTintColor(iconBackgroundImg, R.color.red_warning)
            val shake = AnimationUtils.loadAnimation(it.context, R.anim.shake)
            fingerprintWrapper.startAnimation(shake)
        }
    }

    private fun setImageTintColor(image: ImageView, colorResource: Int) {
        val color = ContextCompat.getColor(image.context, colorResource)
        ImageViewCompat.setImageTintList(image, ColorStateList.valueOf(color))
    }

    interface Callback {
        fun onFingerprintAuthSucceed()
    }
}
