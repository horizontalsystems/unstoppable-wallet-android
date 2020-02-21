package io.horizontalsystems.keystore

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import io.horizontalsystems.views.AlertDialogFragment
import kotlinx.android.synthetic.main.activity_keystore.*

abstract class BaseKeyStoreActivity : AppCompatActivity() {

    abstract var viewModel: KeyStoreViewModel

    abstract fun openMainModule()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_AUTHENTICATION) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    viewModel.delegate.onAuthenticationSuccess()
                }
                Activity.RESULT_CANCELED -> {
                    viewModel.delegate.onAuthenticationCanceled()
                }
            }
        }
    }

    fun observeEvents() {
        viewModel.showNoSystemLockWarning.observe(this, Observer {
            noSystemLockWarning.visibility = View.VISIBLE
        })

        viewModel.showInvalidKeyWarning.observe(this, Observer {
            AlertDialogFragment.newInstance(
                    getString(R.string.Alert_KeysInvalidatedTitle),
                    getString(R.string.Alert_KeysInvalidatedDescription),
                    R.string.Alert_Ok,
                    false,
                    object : AlertDialogFragment.Listener {
                        override fun onButtonClick() {
                            viewModel.delegate.onCloseInvalidKeyWarning()
                        }
                    }).show(supportFragmentManager, "keys_invalidated_alert")
        })

        viewModel.promptUserAuthentication.observe(this, Observer {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            val intent: Intent? = keyguardManager.createConfirmDeviceCredentialIntent(
                    getString(R.string.OSPin_Confirm_Title),
                    getString(R.string.OSPin_Prompt_Desciption)
            )

            startActivityForResult(intent, REQUEST_CODE_AUTHENTICATION)
        })

        viewModel.openLaunchModule.observe(this, Observer {
            openMainModule()
        })

        viewModel.closeApplication.observe(this, Observer {
            finishAffinity()
        })
    }

    companion object {
        const val REQUEST_CODE_AUTHENTICATION = 1
    }

}
