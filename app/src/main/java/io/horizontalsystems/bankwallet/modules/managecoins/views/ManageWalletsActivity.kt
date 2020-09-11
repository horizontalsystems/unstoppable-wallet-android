package io.horizontalsystems.bankwallet.modules.managecoins.views

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.addErc20token.AddErc20TokenActivity
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinItemsAdapter
import io.horizontalsystems.bankwallet.modules.managecoins.ManageWalletsModule
import io.horizontalsystems.bankwallet.modules.managecoins.ManageWalletsPresenter
import io.horizontalsystems.bankwallet.modules.managecoins.ManageWalletsRouter
import io.horizontalsystems.bankwallet.modules.managecoins.ManageWalletsView
import io.horizontalsystems.bankwallet.modules.restore.RestoreMode
import io.horizontalsystems.bankwallet.modules.restore.RestoreModule
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorDialog
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.views.TopMenuItem
import kotlinx.android.synthetic.main.activity_manage_coins.*

class ManageWalletsActivity : BaseActivity(), ManageWalletsDialog.Listener, CoinItemsAdapter.Listener {

    private lateinit var presenter: ManageWalletsPresenter
    private lateinit var adapter: CoinItemsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_coins)

        shadowlessToolbar.bind(
                getString(R.string.ManageCoins_title),
                leftBtnItem = TopMenuItem(text = R.string.ManageCoins_AddToken, onClick = { showAddTokenDialog() }),
                rightBtnItem = TopMenuItem(text = R.string.Button_Done, onClick = { finish() })
        )

        presenter = ViewModelProvider(this, ManageWalletsModule.Factory())
                .get(ManageWalletsPresenter::class.java)

        presenter.onLoad()

        adapter = CoinItemsAdapter(this)
        recyclerView.adapter = adapter

        observe(presenter.view as ManageWalletsView)
        observe(presenter.router as ManageWalletsRouter)
    }

    private fun observe(view: ManageWalletsView) {
        view.coinsLiveData.observe(this, Observer { viewItems ->
            adapter.viewItems = viewItems
            adapter.notifyDataSetChanged()
        })

        view.showManageKeysDialog.observe(this, Observer { (coin, predefinedAccountType) ->
            ManageWalletsDialog.show(this, coin, predefinedAccountType)
        })

        view.showErrorEvent.observe(this, Observer {
            onCancel() // will uncheck coin
        })

        view.showSuccessEvent.observe(this, Observer {
            HudHelper.showSuccessMessage(findViewById(android.R.id.content), R.string.Hud_Text_Done)
        })

        view.showDerivationSelectorDialog.observe(this, Observer { (items, selected, coin) ->
            BottomSheetSelectorDialog.show(
                    supportFragmentManager,
                    getString(R.string.AddressFormatSettings_Title),
                    coin.title,
                    AppLayoutHelper.getCoinDrawable(this, coin.code, coin.type),
                    items.map { derivation -> Pair(derivation.longTitle(), getString(derivation.description(), derivation.addressPrefix(coin.type))) },
                    items.indexOf(selected),
                    notifyUnchanged = true,
                    onItemSelected = { position ->
                        presenter.onSelectDerivationSetting(coin, items[position])
                    },
                    onCancelled = {
                        presenter.onCancelDerivationSelectorDialog(coin)
                    }
            )
        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            ModuleCode.RESTORE -> {
                if (resultCode == Activity.RESULT_OK) {
                    presenter.onAccountRestored()
                }
            }
        }
    }

    override fun onAttachFragment(fragment: Fragment) {
        if (fragment is ManageWalletsDialog) {
            fragment.setListener(this)
        }
    }

    //  ManageWalletsDialog.Listener

    override fun onClickCreateKey(predefinedAccountType: PredefinedAccountType) {
        presenter.onSelectNewAccount(predefinedAccountType)
    }

    override fun onClickRestoreKey(predefinedAccountType: PredefinedAccountType) {
        presenter.onSelectRestoreAccount(predefinedAccountType)
    }

    override fun onCancel() {
        presenter.onClickCancel()
    }

    // CoinItemsAdapter listener

    override fun enable(coin: Coin) {
        presenter.onEnable(coin)
    }

    override fun disable(coin: Coin) {
        presenter.onDisable(coin)
    }

    override fun select(coin: Coin) {
        presenter.onSelect(coin)
    }

    private fun observe(router: ManageWalletsRouter) {
        router.openRestoreModule.observe(this, Observer { predefinedAccountType ->
            RestoreModule.startForResult(this, predefinedAccountType, RestoreMode.InApp)
        })

        router.closeLiveDate.observe(this, Observer {
            finish()
        })
    }

    private fun showAddTokenDialog() {
        AddTokenDialog.show(this, object : AddTokenDialog.Listener {
            override fun onClickAddErc20Token() {
                startActivity(Intent(this@ManageWalletsActivity, AddErc20TokenActivity::class.java))
            }
        })
    }
}
