package io.horizontalsystems.bankwallet.modules.restore

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.entities.PresentationMode
import io.horizontalsystems.bankwallet.modules.blockchainsettings.CoinSettingsModule
import io.horizontalsystems.bankwallet.modules.blockchainsettings.SettingsMode
import io.horizontalsystems.bankwallet.modules.restore.restorecoins.RestoreCoinsModule
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_restore.*
import kotlinx.android.synthetic.main.view_holder_account_restore.*

class RestoreActivity : BaseActivity(), RestoreNavigationAdapter.Listener {

    private lateinit var presenter: RestorePresenter
    private lateinit var adapter: RestoreNavigationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_restore)
        setSupportActionBar(toolbar)

        presenter = ViewModelProvider(this, RestoreModule.Factory()).get(RestorePresenter::class.java)

        adapter = RestoreNavigationAdapter(this)
        recyclerView.adapter = adapter

        observeView(presenter.view as RestoreView)
        observeRouter(presenter.router as RestoreRouter)

        presenter.onLoad()
    }

    private fun observeView(view: RestoreView) {
        view.reloadLiveEvent.observe(this, Observer {
            adapter.items = it
            adapter.notifyDataSetChanged()
        })

        view.showErrorLiveEvent.observe(this, Observer {
            HudHelper.showErrorMessage(this, R.string.Restore_RestoreFailed)
        })
    }

    private fun observeRouter(router: RestoreRouter) {
        router.showRestoreCoins.observe(this, Observer { (predefinedAccountType, accountType) ->
            RestoreCoinsModule.start(this, predefinedAccountType, accountType, PresentationMode.Initial)
        })

        router.showKeyInputEvent.observe(this, Observer { predefinedAccountType ->
            RestoreModule.startForResult(this, predefinedAccountType, ModuleCode.RESTORE_KEY_INPUT)
        })

        router.showCoinSettingsEvent.observe(this, Observer {
            CoinSettingsModule.startForResult(this, SettingsMode.InsideRestore)
        })

        router.closeEvent.observe(this, Observer {
            finish()
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.restore_wallet_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuCancel -> {
                presenter.onClickClose()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            ModuleCode.RESTORE_KEY_INPUT -> {
                val accountType = data?.getParcelableExtra<AccountType>(ModuleField.ACCOUNT_TYPE)
                        ?: return
                presenter.didEnterValidAccount(accountType)
            }
            ModuleCode.COIN_SETTINGS -> {
                if (resultCode == Activity.RESULT_OK) {
                    presenter.didReturnFromCoinSettings()
                }
            }
        }
    }

    override fun onSelect(predefinedAccountType: PredefinedAccountType) {
        presenter.onSelect(predefinedAccountType)
    }

}


class RestoreNavigationAdapter(private val listener: Listener)
    : RecyclerView.Adapter<KeysViewHolder>() {

    interface Listener {
        fun onSelect(predefinedAccountType: PredefinedAccountType)
    }

    var items = listOf<PredefinedAccountType>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeysViewHolder {
        return KeysViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_account_restore, parent, false))
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: KeysViewHolder, position: Int) {
        val predefinedAccountType = items[position]
        holder.bind(predefinedAccountType)
        holder.viewHolderRoot.setOnClickListener {
            listener.onSelect(predefinedAccountType)
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
