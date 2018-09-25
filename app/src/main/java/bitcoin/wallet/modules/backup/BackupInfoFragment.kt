package bitcoin.wallet.modules.backup

import android.app.Activity
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
import kotlinx.android.synthetic.main.fragment_backup_words_info.*

class BackupInfoFragment : Fragment() {
    private lateinit var viewModel: BackupViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup_words_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(BackupViewModel::class.java)
        }

        buttonBackup.setOnClickListener {
            try {
                viewModel.delegate.showWordsDidClick()
            } catch (exception: UserNotAuthenticatedException) {
                EncryptionManager.showAuthenticationScreen(this, AUTHENTICATE_TO_BACKUP_WORDS)
            } catch (exception: KeyPermanentlyInvalidatedException) {
                activity?.let { EncryptionManager.showKeysInvalidatedAlert(it) }
            }
        }

        buttonLater.setOnClickListener {
            viewModel.delegate.laterDidClick()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == AUTHENTICATE_TO_BACKUP_WORDS) {
                viewModel.delegate.showWordsDidClick()
            }
        }
    }

    companion object {
        const val AUTHENTICATE_TO_BACKUP_WORDS = 1
    }

}
