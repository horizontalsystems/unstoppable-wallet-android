package io.horizontalsystems.bankwallet.modules.restore

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.entities.PresentationMode
import io.horizontalsystems.bankwallet.modules.restore.restorecoins.RestoreCoinsModule
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

        viewModel.startRestoreCoins.observe(this, Observer { predefinedAccountType ->
            RestoreCoinsModule.start(this, predefinedAccountType, PresentationMode.Initial)
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
            R.id.menuCancel -> {
                viewModel.delegate.onClickClose()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == RESULT_OK && requestCode == ModuleCode.RESTORE_COINS) {
            viewModel.delegate.onRestore()
        }
    }
}

class RestoreNavigationAdapter(private val viewModel: RestoreViewModel)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items = listOf<PredefinedAccountType>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return KeysViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_account_restore, parent, false))
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val predefinedAccountType = items[position]
        if (holder is KeysViewHolder) {
            holder.bind(predefinedAccountType)
            holder.viewHolderRoot.setOnClickListener {
                viewModel.delegate.onSelect(predefinedAccountType)
            }
        }
    }
}

class KeysViewHolder(override val containerView: View)
    : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(accountType: PredefinedAccountType) {
        val accountTypeTitle = containerView.resources.getString(accountType.title)
        accountName.text = containerView.resources.getString(R.string.Wallet, accountTypeTitle)
        accountCoin.text = containerView.resources.getString(accountType.coinCodes)
    }
}
