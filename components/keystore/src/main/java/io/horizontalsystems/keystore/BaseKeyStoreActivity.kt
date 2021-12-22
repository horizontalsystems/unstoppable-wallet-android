package io.horizontalsystems.keystore

import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.*
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import io.horizontalsystems.keystore.databinding.ActivityKeystoreBinding
import io.horizontalsystems.views.AlertDialogFragment

abstract class BaseKeyStoreActivity : AppCompatActivity() {

    abstract var viewModel: KeyStoreViewModel
    protected lateinit var binding: ActivityKeystoreBinding

    abstract fun openMainModule()

    fun observeEvents() {
        viewModel.showNoSystemLockWarning.observe(this, Observer {
            binding.warningView.isVisible = true
            binding.warningText.setText(R.string.OSPin_Confirm_Desciption)
        })

        viewModel.showInvalidKeyWarning.observe(this, Observer {
            AlertDialogFragment.newInstance(
                titleString = getString(R.string.Alert_KeysInvalidatedTitle),
                descriptionString = getString(R.string.Alert_KeysInvalidatedDescription),
                buttonText = R.string.Alert_Ok,
                cancelable = false,
                listener = object : AlertDialogFragment.Listener {
                    override fun onButtonClick() {
                        viewModel.delegate.onCloseInvalidKeyWarning()
                    }

                    override fun onCancel() {
                        finishAffinity()
                    }
                }).show(supportFragmentManager, "keys_invalidated_alert")
        })

        viewModel.promptUserAuthentication.observe(this, Observer {
            showBiometricPrompt()
        })

        viewModel.openLaunchModule.observe(this, Observer {
            openMainModule()
        })

        viewModel.closeApplication.observe(this, Observer {
            finishAffinity()
        })
    }

    override fun onPause() {
        super.onPause()
        if (binding.warningView.isVisible) {
            finishAffinity()
        }
    }

    private fun showBiometricPrompt() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.OSPin_Confirm_Title))
            .setDescription(getString(R.string.OSPin_Prompt_Desciption))
            .setDeviceCredentialAllowed(true)
            .build()

        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt =
            BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.delegate.onAuthenticationSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)

                    if (errorCode == ERROR_USER_CANCELED
                        || errorCode == ERROR_NEGATIVE_BUTTON
                        || errorCode == ERROR_CANCELED
                    ) {
                        viewModel.delegate.onAuthenticationCanceled()
                    }
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }

}
