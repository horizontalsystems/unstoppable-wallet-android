package bitcoin.wallet.modules.backup

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.UserNotAuthenticatedException
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import bitcoin.wallet.R
import bitcoin.wallet.core.security.EncryptionManager
import bitcoin.wallet.viewHelpers.HudHelper
import kotlinx.android.synthetic.main.fragment_backup_words_confirm.*

class BackupConfirmFragment : Fragment(), BackupConfirmAlert.Listener {
    private lateinit var viewModel: BackupViewModel

    private var wordIndex1 = -1
    private var wordIndex2 = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup_words_confirm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(BackupViewModel::class.java)
        }

        viewModel.wordIndexesToConfirmLiveData.observe(this, Observer { list ->
            list?.let {
                textWordNumber1.text = "${it[0]}."
                textWordNumber2.text = "${it[1]}."

                wordIndex1 = it[0]
                wordIndex2 = it[1]
            }
        })

        viewModel.errorLiveData.observe(this, Observer {
            showError(it)
        })

        buttonBack.setOnClickListener {
            viewModel.delegate.hideConfirmationDidClick()
        }

        buttonSubmit.setOnClickListener {
            if (editWord1.text?.isEmpty() == true || editWord2.text?.isEmpty() == true) {
                showError(R.string.backup_words_error_enter_words)
            } else {
                activity?.let { BackupConfirmAlert.show(it, this) }
            }
        }
    }

    private fun showError(errorMsgId: Int?) {
        errorMsgId?.let { HudHelper.showErrorMessage(it, activity) }
    }

    override fun backupConfirmationSuccess() {
        try {
            validateWords()
        } catch (exception: UserNotAuthenticatedException) {
            EncryptionManager.showAuthenticationScreen(this, AUTHENTICATE_TO_VALIDATE_WORDS)
        } catch (exception: KeyPermanentlyInvalidatedException) {
            activity?.let { EncryptionManager.showKeysInvalidatedAlert(it) }
        }
    }

    private fun validateWords() {
        viewModel.delegate.validateDidClick(
                hashMapOf(wordIndex1 to editWord1.text.toString(),
                        wordIndex2 to editWord2.text.toString())
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == AUTHENTICATE_TO_VALIDATE_WORDS) {
                validateWords()
            }
        }
    }

    companion object {
        const val AUTHENTICATE_TO_VALIDATE_WORDS = 1
    }

}
