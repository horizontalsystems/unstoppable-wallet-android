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
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import io.horizontalsystems.bankwallet.R

/**
 * A dialog which uses fingerprint APIs to authenticate the user, and falls back to password
 * authentication if fingerprint is not available.
 */
class FingerprintAuthenticationDialogFragment : DialogFragment(), FingerprintUiHelper.Callback {

    private lateinit var cancelButton: Button

    private lateinit var callback: Callback
    private lateinit var cryptoObject: FingerprintManager.CryptoObject
    private lateinit var fingerprintUiHelper: FingerprintUiHelper
    private lateinit var inputMethodManager: InputMethodManager
    private lateinit var sharedPreferences: SharedPreferences


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

        cancelButton.setOnClickListener { dismiss() }

        fingerprintUiHelper = FingerprintUiHelper(
                activity.getSystemService(FingerprintManager::class.java),
                view.findViewById(R.id.fingerprint_background),
                view.findViewById(R.id.fingerprint_status),
                view.findViewById(R.id.fingerprint_wrapper),
                this
        )
    }

    override fun onResume() {
        super.onResume()
        fingerprintUiHelper.startListening(cryptoObject)

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

    override fun onPause() {
        super.onPause()
        fingerprintUiHelper.stopListening()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        inputMethodManager = context.getSystemService(InputMethodManager::class.java)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    fun setCryptoObject(cryptoObject: FingerprintManager.CryptoObject) {
        this.cryptoObject = cryptoObject
    }

    override fun onAuthenticated() {
        // Callback from FingerprintUiHelper. Let the activity know that authentication succeeded.
        callback.onFingerprintAuthSucceed(withFingerprint = true, crypto = cryptoObject)
        dismiss()
    }

    interface Callback {
        fun onFingerprintAuthSucceed(withFingerprint: Boolean, crypto: FingerprintManager.CryptoObject? = null)
    }
}
