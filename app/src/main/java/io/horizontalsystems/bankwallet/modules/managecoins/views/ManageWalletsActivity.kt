package io.horizontalsystems.bankwallet.modules.managecoins.views

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.coinsettings.CoinSettingsModule
import io.horizontalsystems.bankwallet.modules.coinsettings.CoinSettingsWrapped
import io.horizontalsystems.bankwallet.modules.managecoins.CoinToggleViewItem
import io.horizontalsystems.bankwallet.modules.managecoins.ManageWalletsViewModel
import io.horizontalsystems.bankwallet.modules.restore.RestoreModule
import io.horizontalsystems.bankwallet.ui.dialogs.ManageWalletsDialog
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import kotlinx.android.synthetic.main.activity_manage_coins.*

class ManageWalletsActivity : BaseActivity(), ManageWalletsDialog.Listener, ManageWalletsAdapter.Listener {

    private lateinit var viewModel: ManageWalletsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_coins)

        viewModel = ViewModelProvider(this).get(ManageWalletsViewModel::class.java)
        viewModel.init()

        val adapter = ManageWalletsAdapter(this)
        recyclerView.adapter = adapter

        shadowlessToolbar.bind(
                title = getString(R.string.ManageCoins_title),
                leftBtnItem = TopMenuItem(R.drawable.ic_back, onClick = { onBackPressed() })
        )

        viewModel.setViewItems.observe(this, Observer { (featuredViewItems, coinViewItems) ->
            adapter.featuredViewItems = featuredViewItems
            adapter.coinsViewItems = coinViewItems
            adapter.notifyDataSetChanged()
        })

        viewModel.coinsLoadedLiveEvent.observe(this, Observer {
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

        viewModel.showCoinSettings.observe(this, Observer { (coin, coinSettings) ->
            CoinSettingsModule.startForResult(coin, coinSettings, this)
        })
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

    //  ManageWalletsDialog listener

    override fun onClickCreateKey(predefinedAccountType: PredefinedAccountType) {
        viewModel.delegate.onSelectNewAccount(predefinedAccountType)
    }

    override fun onClickRestoreKey(predefinedAccountType: PredefinedAccountType) {
        viewModel.delegate.onSelectRestoreAccount(predefinedAccountType)
    }

    override fun onCancel() {
        viewModel.delegate.onClickCancel()
    }

    //ManageWalletsAdapter listener

    override fun enable(item: CoinToggleViewItem) {
        viewModel.delegate.onEnable(item)
    }

    override fun disable(item: CoinToggleViewItem) {
        viewModel.delegate.onDisable(item)
    }

    override fun select(item: CoinToggleViewItem) {
        viewModel.delegate.onSelect(item)
    }
}
