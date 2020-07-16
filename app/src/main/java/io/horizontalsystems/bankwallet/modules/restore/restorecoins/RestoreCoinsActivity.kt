package io.horizontalsystems.bankwallet.modules.restore.restorecoins

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinItemsAdapter
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorDialog
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import io.horizontalsystems.views.TopMenuItem
import kotlinx.android.synthetic.main.select_coins.*

class RestoreCoinsActivity : BaseActivity(), CoinItemsAdapter.Listener {
    private lateinit var presenter: RestoreCoinsPresenter
    private lateinit var coinItemsAdapter: CoinItemsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.select_coins)

        shadowlessToolbar.bind(
                getString(R.string.ManageCoins_title),
                leftBtnItem = TopMenuItem(R.drawable.ic_back) { onBackPressed() },
                rightBtnItem = TopMenuItem(text = R.string.Button_Restore, onClick = { presenter.onProceedButtonClick() })
        )
        shadowlessToolbar.setRightButtonEnabled(false)

        val predefinedAccountType: PredefinedAccountType = intent.getParcelableExtra(ModuleField.PREDEFINED_ACCOUNT_TYPE)
                ?: run { finish(); return }

        presenter = ViewModelProvider(this, RestoreCoinsModule.Factory(predefinedAccountType)).get(RestoreCoinsPresenter::class.java)

        observeView(presenter.view as RestoreCoinsView)
        observeRouter(presenter.router as RestoreCoinsRouter)

        coinItemsAdapter = CoinItemsAdapter(this)
        coins.adapter = coinItemsAdapter

        presenter.onLoad()
    }

    //CoinItemsAdapter.Listener

    override fun enable(coin: Coin) {
        presenter.onEnable(coin)
    }

    override fun disable(coin: Coin) {
        presenter.onDisable(coin)
    }

    override fun select(coin: Coin) {
        //not used here
    }

    private fun observeView(view: RestoreCoinsView) {
        view.coinsLiveData.observe(this, Observer { viewItems ->
            coinItemsAdapter.viewItems = viewItems
            coinItemsAdapter.notifyDataSetChanged()
        })

        view.proceedButtonEnabled.observe(this, Observer { enabled ->
            shadowlessToolbar.setRightButtonEnabled(enabled)
        })

        view.showDerivationSelectorDialog.observe(this, Observer { (items, selected, coin) ->
            BottomSheetSelectorDialog.show(
                    supportFragmentManager,
                    getString(R.string.AddressFormatSettings_Title),
                    coin.title,
                    AppLayoutHelper.getCoinDrawable(this, coin.code, coin.type),
                    items.map { derivation -> Pair(derivation.longTitle(), getString(derivation.description(), derivation.addressPrefix(coin.type))) },
                    items.indexOf(selected),
                    onItemSelected = { position ->
                        presenter.onSelectDerivationSetting(coin, items[position])
                    },
                    onCancelled = {
                        presenter.onCancelDerivationSelectorDialog(coin)
                    }
            )
        })

    }

    private fun observeRouter(router: RestoreCoinsRouter) {
        router.closeWithSelectedCoins.observe(this, Observer { coins ->
            setResult(RESULT_OK, Intent().apply {
                putParcelableArrayListExtra(ModuleField.COINS, ArrayList(coins))
            })
            finish()
        })
    }

}
