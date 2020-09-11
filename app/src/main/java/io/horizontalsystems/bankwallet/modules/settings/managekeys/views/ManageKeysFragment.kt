package io.horizontalsystems.bankwallet.modules.settings.managekeys.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.addressformat.AddressFormatSettingsModule
import io.horizontalsystems.bankwallet.modules.backup.BackupModule
import io.horizontalsystems.bankwallet.modules.createwallet.CreateWalletModule
import io.horizontalsystems.bankwallet.modules.restore.RestoreMode
import io.horizontalsystems.bankwallet.modules.restore.RestoreModule
import io.horizontalsystems.bankwallet.modules.settings.managekeys.*
import kotlinx.android.synthetic.main.fragment_manage_keys.*

class ManageKeysFragment : BaseFragment(), ManageKeysDialog.Listener, ManageKeysAdapter.Listener {

    private val presenter by viewModels<ManageKeysPresenter> { ManageKeysModule.Factory() }
    private lateinit var adapter: ManageKeysAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_manage_keys, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.let {
            it.setSupportActionBar(toolbar)
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        adapter = ManageKeysAdapter(this)
        recyclerView.adapter = adapter

        observeView(presenter.view as ManageKeysView)
        observeRouter(presenter.router as ManageKeysRouter)

        (presenter.view as ManageKeysView).confirmUnlinkEvent.observe(this, Observer { item ->
            item.account?.let { account ->
                val confirmationList = listOf(
                        getString(R.string.ManageKeys_Delete_ConfirmationRemove, getString(item.predefinedAccountType.title)),
                        getString(R.string.ManageKeys_Delete_ConfirmationDisable, getString(item.predefinedAccountType.coinCodes)),
                        getString(R.string.ManageKeys_Delete_ConfirmationLose)
                )

                val confirmListener = object : ManageKeysDeleteAlert.Listener {
                    override fun onConfirmationSuccess() {
                        presenter.onConfirmUnlink(account)
                    }
                }

                activity?.let {
                    ManageKeysDeleteAlert.show(it, getString(item.predefinedAccountType.title), confirmationList, confirmListener)
                }
            }
        })

        (presenter.view as ManageKeysView).confirmBackupEvent.observe(this, Observer {
            val title = getString(R.string.ManageKeys_Delete_Alert_Title)
            val subtitle = getString(it.predefinedAccountType.title)
            val description = getString(R.string.ManageKeys_Delete_Alert)
            ManageKeysDialog.show(parentFragmentManager, title, subtitle, description)
        })

        presenter.onLoad()
    }

    override fun onAttachFragment(fragment: Fragment) {
        if (fragment is ManageKeysDialog) {
            fragment.setListener(this)
        }
    }

    private fun observeView(view: ManageKeysView) {
        view.showItemsEvent.observe(this, Observer { list ->
            adapter.items = list
            adapter.notifyDataSetChanged()
        })
    }

    private fun observeRouter(router: ManageKeysRouter) {
        router.showRestore.observe(this, Observer { predefinedAccountType ->
            context?.let {
                RestoreModule.start(it, predefinedAccountType, RestoreMode.FromManageKeys)
            }
        })

        router.showCreateWalletLiveEvent.observe(this, Observer { predefinedAccountType ->
            context?.let {
                CreateWalletModule.startInApp(it, predefinedAccountType)
            }
        })

        router.showBackupModule.observe(this, Observer { (account, predefinedAccountType) ->
            context?.let {
                BackupModule.start(it, account, getString(predefinedAccountType.coinCodes))
            }
        })

        router.showBlockchainSettings.observe(this, Observer { enabledCoinTypes ->
            activity?.let {
                AddressFormatSettingsModule.startForResult(it, enabledCoinTypes, false)
            }
        })

        router.closeEvent.observe(this, Observer {
            activity?.supportFragmentManager?.popBackStack()
        })
    }

    //  ManageKeysAdapter Listener

    override fun onClickAdvancedSettings(item: ManageAccountItem) {
        presenter.onClickAdvancedSettings(item)
    }

    override fun onClickCreate(item: ManageAccountItem) {
        presenter.onClickCreate(item)
    }

    override fun onClickRestore(item: ManageAccountItem) {
        presenter.onClickRestore(item)
    }

    override fun onClickBackup(item: ManageAccountItem) {
        presenter.onClickBackup(item)
    }

    override fun onClickUnlink(item: ManageAccountItem) {
        presenter.onClickUnlink(item)
    }

    //  ManageKeysDialog Listener

    override fun onClickBackupKey() {
        presenter.onConfirmBackup()
    }
}
