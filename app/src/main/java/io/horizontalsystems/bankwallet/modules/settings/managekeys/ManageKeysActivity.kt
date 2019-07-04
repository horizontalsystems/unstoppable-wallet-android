package io.horizontalsystems.bankwallet.modules.settings.managekeys

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Account
import io.horizontalsystems.bankwallet.core.AccountType
import io.horizontalsystems.bankwallet.modules.backup.BackupModule
import io.horizontalsystems.bankwallet.modules.pin.PinModule
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

        viewModel.closeLiveEvent.observe(this, Observer {
            finish()
        })

        viewModel.unlinkAccountEvent.observe(this, Observer { account ->
            account?.let {
                val confirmationList = mutableListOf(
                        R.string.SettingsSecurity_ImportWalletConfirmation_1,
                        R.string.SettingsSecurity_ImportWalletConfirmation_2
                )

                val confirmListener = object : BottomConfirmAlert.Listener {
                    override fun onConfirmationSuccess() {
                        viewModel.delegate.unlinkAccount(account.id)
                    }
                }

                BottomConfirmAlert.show(this, confirmationList, confirmListener, BottomButtonColor.RED)
            }
        })

        viewModel.showPinUnlockLiveEvent.observe(this, Observer {
            PinModule.startForUnlock(true)
        })

        viewModel.openBackupWalletLiveEvent.observe(this, Observer { account ->
            account?.let {
                BackupModule.start(this, account)
            }
        })

        viewModel.showItemsEvent.observe(this, Observer { list ->
            list?.let {
                adapter.items = it
                adapter.notifyDataSetChanged()
            }
        })
    }
}

class ManageKeysAdapter(private val viewModel: ManageKeysViewModel)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items = listOf<Account>()

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

    fun bind(account: Account) {
        accountName.text = account.name
        accountCoin.text = when (account.type) {
            is AccountType.Eos -> "EOS"
            is AccountType.Mnemonic,
            is AccountType.HDMasterKey,
            is AccountType.PrivateKey -> "BTC, BCH, DASH, ETH, ERC20"
        }

        if (!account.isBackedUp) {
            backupBadge.visibility = View.VISIBLE
        }

        buttonUnlink.setOnClickListener {
            viewModel.onUnlink(account)
        }

        buttonBackup.setOnClickListener {
            viewModel.delegate.backupAccount(account)
        }
    }
}
