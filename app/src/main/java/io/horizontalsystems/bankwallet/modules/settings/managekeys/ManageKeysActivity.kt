package io.horizontalsystems.bankwallet.modules.settings.managekeys

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AccountType
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.entities.EosAccountType
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.entities.Words12AccountType
import io.horizontalsystems.bankwallet.entities.Words24AccountType
import io.horizontalsystems.bankwallet.modules.backup.BackupModule
import io.horizontalsystems.bankwallet.modules.restorewords.RestoreWordsModule
import io.horizontalsystems.bankwallet.ui.dialogs.BottomButtonColor
import io.horizontalsystems.bankwallet.ui.dialogs.BottomConfirmAlert
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_manage_keys.*
import kotlinx.android.synthetic.main.view_holder_account.*

class ManageKeysActivity : BaseActivity() {

    private lateinit var viewModel: ManageKeysViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(ManageKeysViewModel::class.java)
        viewModel.init()

        setContentView(R.layout.activity_manage_keys)
        shadowlessToolbar.bind(getString(R.string.ManageKeys_title), TopMenuItem(R.drawable.back) { onBackPressed() })

        val adapter = ManageKeysAdapter(viewModel)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.confirmUnlinkEvent.observe(this, Observer { account ->
            account?.let {
                val confirmationList = mutableListOf(
                        R.string.SettingsSecurity_ImportWalletConfirmation_1,
                        R.string.SettingsSecurity_ImportWalletConfirmation_2
                )

                val confirmListener = object : BottomConfirmAlert.Listener {
                    override fun onConfirmationSuccess() {
                        viewModel.delegate.onClickUnlink(account.id)
                    }
                }

                BottomConfirmAlert.show(this, confirmationList, confirmListener, BottomButtonColor.RED)
            }
        })

        viewModel.startBackupModuleLiveEvent.observe(this, Observer { account ->
            account?.let { BackupModule.start(this, account) }
        })

        viewModel.startRestoreWordsLiveEvent.observe(this, Observer {
            RestoreWordsModule.startForResult(this, ModuleCode.RESTORE_WORDS)
        })

        viewModel.closeLiveEvent.observe(this, Observer {
            finish()
        })

        viewModel.showItemsEvent.observe(this, Observer { list ->
            list?.let {
                adapter.items = it
                adapter.notifyDataSetChanged()
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data == null || resultCode != RESULT_OK) return

        when (requestCode) {
            ModuleCode.RESTORE_WORDS -> {
                val syncMode = data.getParcelableExtra<SyncMode>("syncMode")
                val accountType = data.getParcelableExtra<AccountType>("accountType")

                viewModel.delegate.onClickRestore(accountType, syncMode)
            }
        }
    }
}

class ManageKeysAdapter(private val viewModel: ManageKeysViewModel)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items = listOf<ManageAccountItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return KeysViewHolder(viewModel, LayoutInflater.from(parent.context).inflate(R.layout.view_holder_account, parent, false))
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is KeysViewHolder) {
            holder.bind(items[position])
        }
    }
}

class KeysViewHolder(private val viewModel: ManageKeysViewModel, override val containerView: View)
    : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: ManageAccountItem) {
        hideButtons()

        val pAccountType = item.predefinedAccountType
        accountName.text = pAccountType.title
        accountCoin.text = pAccountType.coinCodes

        if (item.account == null) {
            when (pAccountType) {
                is EosAccountType -> {
                    buttonImport.visibility = View.VISIBLE
                }
                is Words12AccountType,
                is Words24AccountType -> {
                    buttonImport.visibility = View.VISIBLE
                    buttonNew.visibility = View.VISIBLE
                    buttonNew.setOnClickListener {
                        viewModel.delegate.onClickNew(pAccountType)
                    }
                }
            }

            buttonImport.setOnClickListener { viewModel.delegate.onClickRestore(pAccountType) }

            keyIcon.setColorFilter(ContextCompat.getColor(containerView.context, R.color.grey))
            accountName.setTextColor(containerView.resources.getColor(R.color.grey))
            accountCoin.setTextColor(containerView.resources.getColor(R.color.grey))

            return
        }

        val account = item.account
        if (account.isBackedUp) {
            buttonShow.visibility = View.VISIBLE
        } else {
            buttonBackup.visibility = View.VISIBLE
        }

        buttonUnlink.visibility = View.VISIBLE
        buttonUnlink.setOnClickListener { viewModel.confirmUnlink(account) }
        buttonBackup.setOnClickListener { viewModel.delegate.onClickBackup(account) }
    }

    private fun hideButtons() {
        buttonNew.visibility = View.GONE
        buttonImport.visibility = View.GONE
        buttonUnlink.visibility = View.GONE
        buttonShow.visibility = View.GONE
        buttonBackup.visibility = View.GONE
    }
}
