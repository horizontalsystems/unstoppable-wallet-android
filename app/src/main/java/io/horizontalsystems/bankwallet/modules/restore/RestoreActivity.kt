package io.horizontalsystems.bankwallet.modules.restore

import android.content.Context
import android.content.Intent
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
import io.horizontalsystems.bankwallet.core.AccountType
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.modules.restorewords.RestoreWordsModule
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_restore.*
import kotlinx.android.synthetic.main.view_holder_account_restore.*

class RestoreActivity : BaseActivity() {

    private lateinit var viewModel: RestoreViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_restore)
        shadowlessToolbar.bind(getString(R.string.Restore_Title), TopMenuItem(R.drawable.back) { onBackPressed() })

        viewModel = ViewModelProviders.of(this).get(RestoreViewModel::class.java)
        viewModel.init()

        recyclerView.adapter = RestoreNavigationAdapter(viewModel)
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.goToRestoreWordsLiveEvent.observe(this, Observer {
            RestoreWordsModule.startForResult(this, ModuleCode.RESTORE_WORDS)
        })

        viewModel.closeLiveEvent.observe(this, Observer {
            finish()
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ModuleCode.RESTORE_WORDS && data != null && resultCode == RESULT_OK) {
            val syncMode = data.getParcelableExtra<SyncMode>("syncMode")
            val accountType = data.getParcelableExtra<AccountType>("accountType")

            viewModel.delegate.didRestore(accountType, syncMode)
        }
    }
}

class RestoreNavigationAdapter(private val viewModel: RestoreViewModel)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = PredefinedAccountType.values()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return KeysViewHolder(parent.context, LayoutInflater.from(parent.context).inflate(R.layout.view_holder_account_restore, parent, false))
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

class KeysViewHolder(private val context: Context, override val containerView: View)
    : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(accountType: PredefinedAccountType) {
        accountName.text = context.getString(accountType.title)
        accountCoin.text = accountType.coinCodes
    }
}
