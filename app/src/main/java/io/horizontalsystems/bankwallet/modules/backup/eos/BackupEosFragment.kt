package io.horizontalsystems.bankwallet.modules.backup.eos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.setNavigationResult
import io.horizontalsystems.views.TopMenuItem
import kotlinx.android.synthetic.main.fragment_backup_eos.*

class BackupEosFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup_eos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        shadowlessToolbar.bind(getString(R.string.Backup_DisplayTitle), TopMenuItem(R.drawable.ic_back, onClick = {
            findNavController().popBackStack()
        }))

        val account = arguments?.getString(ACCOUNT) ?: run {
            findNavController().popBackStack()
            return
        }

        val activePrivateKey = arguments?.getString(ACTIVE_PRIVATE_KEY) ?: run {
            findNavController().popBackStack()
            return
        }

        bind(account, activePrivateKey)
    }

    private fun bind(account: String, privateKey: String) {
        eosAccount.text = account
        eosAccount.bind { onCopy(account) }

        eosActivePrivateKey.text = privateKey
        eosActivePrivateKey.bind { onCopy(privateKey) }

        btnClose.setOnClickListener {
            setNavigationResult(BackupEosModule.requestKey, bundleOf(BackupEosModule.requestResult to BackupEosModule.RESULT_SHOW))
            findNavController().popBackStack()
        }

        imgQrCode.setImageBitmap(TextHelper.getQrCodeBitmap(privateKey, 120F))
    }

    private fun onCopy(text: String) {
        TextHelper.copyText(text)
        activity?.let {
            HudHelper.showSuccessMessage(it.findViewById(android.R.id.content), R.string.Hud_Text_Copied)
        }
    }

    companion object {
        const val ACCOUNT = "account"
        const val ACTIVE_PRIVATE_KEY = "active_private_key"
    }
}
