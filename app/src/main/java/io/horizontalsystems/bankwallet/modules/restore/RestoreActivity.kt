package io.horizontalsystems.bankwallet.modules.restore

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.restore.eos.RestoreEosModule
import io.horizontalsystems.bankwallet.modules.restore.restorecoins.RestoreCoinsModule
import io.horizontalsystems.bankwallet.modules.restore.words.RestoreWordsModule
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
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val predefinedAccountType: PredefinedAccountType? = intent.getParcelableExtra(ModuleField.PREDEFINED_ACCOUNT_TYPE)
        val mode: RestoreMode = intent.getParcelableExtra(ModuleField.RESTORE_MODE) ?: RestoreMode.FromWelcome

        presenter = ViewModelProvider(this, RestoreModule.Factory(predefinedAccountType, mode)).get(RestorePresenter::class.java)

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
        router.showKeyInputEvent.observe(this, Observer { predefinedAccountType ->
            when(predefinedAccountType){
                PredefinedAccountType.Standard -> RestoreWordsModule.startForResult(this, 12, predefinedAccountType.title, ModuleCode.RESTORE_KEY_INPUT)
                PredefinedAccountType.Binance -> RestoreWordsModule.startForResult(this, 24, predefinedAccountType.title, ModuleCode.RESTORE_KEY_INPUT)
                PredefinedAccountType.Eos -> RestoreEosModule.startForResult(this, ModuleCode.RESTORE_KEY_INPUT)
            }
        })

        router.showRestoreCoins.observe(this, Observer { predefinedAccountType ->
            RestoreCoinsModule.startForResult(this, predefinedAccountType)
        })

        router.startMainModuleLiveEvent.observe(this, Observer {
            MainModule.start(this)
            finishAffinity()
        })

        router.closeEvent.observe(this, Observer {
            finish()
        })

        router.closeWithSuccessEvent.observe(this, Observer {
            setResult(Activity.RESULT_OK)
            finish()
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            presenter.onReturnWithCancel()
            return
        }

        when (requestCode) {
            ModuleCode.RESTORE_KEY_INPUT -> {
                if (resultCode == Activity.RESULT_OK) {
                    val accountType = data?.getParcelableExtra<AccountType>(ModuleField.ACCOUNT_TYPE) ?: return
                    presenter.didEnterValidAccount(accountType)
                }
            }
            ModuleCode.RESTORE_COINS -> {
                if (resultCode == Activity.RESULT_OK) {
                    val enabledCoins = data?.getParcelableArrayListExtra<Coin>(ModuleField.COINS)
                    presenter.didReturnFromRestoreCoins(enabledCoins)
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
