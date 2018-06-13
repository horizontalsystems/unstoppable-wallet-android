package org.grouvi.wallet.modules.settings

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_settings.*
import org.grouvi.wallet.LauncherActivity
import org.grouvi.wallet.R
import org.grouvi.wallet.lib.WalletDataManager
import org.grouvi.wallet.modules.backupWords.BackupWordsModule

class SettingsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonRemoveWallet.setOnClickListener {
            WalletDataManager.mnemonicWords = listOf()

            val intent = Intent(context, LauncherActivity::class.java)
            startActivity(intent)

            activity?.finish()
        }

        buttonBackup.setOnClickListener {
            context?.let { BackupWordsModule.start(it) }
        }
    }
}