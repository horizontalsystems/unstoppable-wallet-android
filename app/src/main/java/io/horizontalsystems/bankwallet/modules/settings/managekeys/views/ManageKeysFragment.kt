package io.horizontalsystems.bankwallet.modules.settings.managekeys.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.restore.RestoreFragment
import io.horizontalsystems.bankwallet.modules.settings.managekeys.*
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_manage_keys.*

class ManageKeysFragment : BaseFragment(), ManageKeysDialog.Listener, ManageKeysAdapter.Listener {

    private val presenter by viewModels<ManageKeysPresenter> { ManageKeysModule.Factory() }
    private lateinit var adapter: ManageKeysAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_manage_keys, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
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
            ManageKeysDialog.show(childFragmentManager, title, subtitle, description)
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
            val arguments = Bundle(3).apply {
                putParcelable(RestoreFragment.PREDEFINED_ACCOUNT_TYPE_KEY, predefinedAccountType)
                putBoolean(RestoreFragment.SELECT_COINS_KEY, true)
            }

            findNavController().navigate(R.id.manageKeysFragment_to_restoreFragment, arguments, navOptions())
        })

        router.showCreateWalletLiveEvent.observe(this, Observer { predefinedAccountType ->
            val arguments = Bundle(1).apply {
                putParcelable("predefinedAccountType", predefinedAccountType)
            }

            findNavController().navigate(R.id.manageKeysFragment_to_createWalletFragment, arguments, navOptions())
        })

        router.showBackupModule.observe(this, Observer { (account, predefinedAccountType) ->
            val arguments = Bundle(2).apply {
                putParcelable(ModuleField.ACCOUNT, account)
                putString(ModuleField.ACCOUNT_COINS, getString(predefinedAccountType.coinCodes))
            }

            findNavController().navigate(R.id.manageKeysFragment_to_backupFragment, arguments, navOptions())
        })

        router.showAddressFormat.observe(this, Observer {
            findNavController().navigate(R.id.manageKeysFragment_to_addressFormatFragment, null, navOptions())
        })

        router.closeEvent.observe(this, Observer {
            findNavController().popBackStack()
        })
    }

    //  ManageKeysAdapter Listener

    override fun onClickAddressFormat(item: ManageAccountItem) {
        presenter.onClickAddressFormat(item)
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
