package bitcoin.wallet.modules.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import bitcoin.wallet.LauncherActivity
import bitcoin.wallet.R
import bitcoin.wallet.lib.WalletDataManager
import bitcoin.wallet.modules.backup.BackupModule
import bitcoin.wallet.modules.backup.BackupPresenter
import bitcoin.wallet.modules.main.BaseTabFragment
import kotlinx.android.synthetic.main.fragment_settings.*

class SettingsFragment : BaseTabFragment() {

    override val title: Int
        get() = R.string.tab_title_settings

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
            context?.let { BackupModule.start(it, BackupPresenter.DismissMode.DISMISS_SELF) }
        }
    }
}