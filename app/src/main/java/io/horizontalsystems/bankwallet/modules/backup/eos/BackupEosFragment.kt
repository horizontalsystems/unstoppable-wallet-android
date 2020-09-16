package io.horizontalsystems.bankwallet.modules.backup.eos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.fragment.app.setFragmentResult
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.views.TopMenuItem
import kotlinx.android.synthetic.main.fragment_backup_eos.*

class BackupEosFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup_eos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        shadowlessToolbar.bind(getString(R.string.Backup_DisplayTitle), TopMenuItem(R.drawable.ic_back, onClick = {
            activity?.supportFragmentManager?.popBackStack()
        }))

        val account = arguments?.getString(ACCOUNT) ?: run {
            activity?.supportFragmentManager?.popBackStack()
            return
        }

        val activePrivateKey = arguments?.getString(ACTIVE_PRIVATE_KEY) ?: run {
            activity?.supportFragmentManager?.popBackStack()
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
            setFragmentResult(BackupEosModule.requestKey, bundleOf(
                    BackupEosModule.requestResult to BackupEosModule.RESULT_SHOW
            ))
            activity?.supportFragmentManager?.popBackStack()
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

        fun start(activity: FragmentActivity, account: String, activePrivateKey: String) {
            val fragment = BackupEosFragment().apply {
                arguments = Bundle(2).apply {
                    putString(ACCOUNT, account)
                    putString(ACTIVE_PRIVATE_KEY, activePrivateKey)
                }
            }

            activity.supportFragmentManager.commit {
                add(R.id.fragmentContainerView, fragment)
                addToBackStack(null)
            }
        }
    }
}
