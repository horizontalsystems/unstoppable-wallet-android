package io.horizontalsystems.bankwallet.modules.managecoins.views

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.coinsettings.CoinSettingsModule
import io.horizontalsystems.bankwallet.modules.coinsettings.CoinSettingsWrapped
import io.horizontalsystems.bankwallet.modules.coinsettings.SettingsMode
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinItemsAdapter
import io.horizontalsystems.bankwallet.modules.managecoins.ManageWalletsViewModel
import io.horizontalsystems.bankwallet.modules.restore.RestoreModule
import io.horizontalsystems.bankwallet.ui.dialogs.ManageWalletsDialog
import kotlinx.android.synthetic.main.activity_manage_coins.*

class ManageWalletsActivity : BaseActivity(), ManageWalletsDialog.Listener, CoinItemsAdapter.Listener {

    private lateinit var viewModel: ManageWalletsViewModel
    private var showCloseButton: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_coins)

        showCloseButton = intent?.extras?.getBoolean(ModuleField.SHOW_CLOSE_BUTTON, false) ?: false

        setSupportActionBar(toolbar)
        if (!showCloseButton) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        viewModel = ViewModelProvider(this).get(ManageWalletsViewModel::class.java)
        viewModel.init()

        val adapter = CoinItemsAdapter(this)
        recyclerView.adapter = adapter

        viewModel.coinsLiveData.observe(this, Observer { viewItems ->
            adapter.viewItems = viewItems
            adapter.notifyDataSetChanged()
        })

        viewModel.showManageKeysDialog.observe(this, Observer { (coin, predefinedAccountType) ->
            ManageWalletsDialog.show(this, this, coin, predefinedAccountType)
        })

        viewModel.openRestoreModule.observe(this, Observer { predefinedAccountType ->
            RestoreModule.startForResult(this, predefinedAccountType)
        })

        viewModel.showErrorEvent.observe(this, Observer {
            onCancel() // will uncheck coin
        })

        viewModel.closeLiveDate.observe(this, Observer {
            finish()
        })

        viewModel.showCoinSettings.observe(this, Observer { (coin, coinSettings, origin) ->
            val settingMode = if(origin == AccountOrigin.Created) SettingsMode.Creating else SettingsMode.Restoring
            CoinSettingsModule.startForResult(coin, coinSettings, settingMode, this)
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.manage_coins_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.menuClose)?.apply {
            isVisible = showCloseButton
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.menuClose ->  {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            ModuleCode.COIN_SETTINGS -> {
                if (resultCode == Activity.RESULT_CANCELED) {
                    viewModel.delegate.onClickCancel()
                } else if (resultCode == Activity.RESULT_OK) {
                    val coin = data?.getParcelableExtra<Coin>(ModuleField.COIN) ?: return
                    val coinSettings = data.getParcelableExtra<CoinSettingsWrapped>(ModuleField.COIN_SETTINGS) ?: return

                    viewModel.delegate.onSelect(coinSettings.settings, coin)
                }
            }
            ModuleCode.RESTORE-> {
                val accountType = data?.getParcelableExtra<AccountType>(ModuleField.ACCOUNT_TYPE) ?: return
                viewModel.delegate.didRestore(accountType)
            }
        }

    }

    // ManageWalletsDialog.Listener

    override fun onClickCreateKey(predefinedAccountType: PredefinedAccountType) {
        viewModel.delegate.onSelectNewAccount(predefinedAccountType)
    }

    override fun onClickRestoreKey(predefinedAccountType: PredefinedAccountType) {
        viewModel.delegate.onSelectRestoreAccount(predefinedAccountType)
    }

    override fun onCancel() {
        viewModel.delegate.onClickCancel()
    }

    // CoinItemsAdapter listener

    override fun enable(coin: Coin) {
        viewModel.delegate.onEnable(coin)
    }

    override fun disable(coin: Coin) {
        viewModel.delegate.onDisable(coin)
    }

    override fun select(coin: Coin) {
        viewModel.delegate.onSelect(coin)
    }
}
