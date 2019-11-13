package io.horizontalsystems.bankwallet.modules.restore

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IPredefinedAccountType
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.restore.eos.RestoreEosModule
import io.horizontalsystems.bankwallet.modules.restore.words.RestoreWordsModule
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_restore.*
import kotlinx.android.synthetic.main.view_holder_account_restore.*

class RestoreActivity : BaseActivity() {

    private lateinit var viewModel: RestoreViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_restore)
        setSupportActionBar(toolbar)

        viewModel = ViewModelProvider(this).get(RestoreViewModel::class.java)
        viewModel.init()

        val adapter = RestoreNavigationAdapter(viewModel)
        recyclerView.adapter = adapter

        viewModel.reloadLiveEvent.observe(this, Observer {
            adapter.items = it
            adapter.notifyDataSetChanged()
        })

        viewModel.showErrorLiveEvent.observe(this, Observer {
            HudHelper.showErrorMessage(R.string.Restore_RestoreFailed)
        })

        viewModel.startRestoreWordsLiveEvent.observe(this, Observer { (wordsCount, titleRes) ->
            RestoreWordsModule.startForResult(this, wordsCount, titleRes, ModuleCode.RESTORE_WORDS)
        })

        viewModel.startRestoreEosLiveEvent.observe(this, Observer {titleRes ->
            RestoreEosModule.startForResult(this, titleRes, ModuleCode.RESTORE_EOS)
        })

        viewModel.startMainModuleLiveEvent.observe(this, Observer {
            MainModule.startAsNewTask(this)
            finish()
        })

        viewModel.closeLiveEvent.observe(this, Observer {
            finish()
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.restore_wallet_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menuCopy -> {
                viewModel.delegate.onClickClose()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data == null || resultCode != RESULT_OK)
            return

        val accountType = data.getParcelableExtra<AccountType>(ModuleField.ACCOUNT_TYPE)

        when (requestCode) {
            ModuleCode.RESTORE_WORDS -> {
                viewModel.delegate.onRestore(accountType, data.getParcelableExtra(ModuleField.SYNCMODE))
            }
            ModuleCode.RESTORE_EOS -> {
                viewModel.delegate.onRestore(accountType)
            }
        }
    }
}

class RestoreNavigationAdapter(private val viewModel: RestoreViewModel)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items = listOf<IPredefinedAccountType>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return KeysViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_account_restore, parent, false))
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val accountType = items[position]
        if (holder is KeysViewHolder) {
            holder.bind(accountType)
            holder.viewHolderRoot.setOnClickListener {
                viewModel.delegate.onSelect(accountType)
            }
        }
    }
}

class KeysViewHolder(override val containerView: View)
    : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(accountType: IPredefinedAccountType) {
        val accountTypeTitle = containerView.resources.getString(accountType.title)
        accountName.text = containerView.resources.getString(R.string.Wallet, accountTypeTitle)
        accountCoin.text = containerView.resources.getString(accountType.coinCodes)
    }
}
