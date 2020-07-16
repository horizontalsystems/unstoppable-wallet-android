package io.horizontalsystems.bankwallet.modules.createwallet.view

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.entities.PresentationMode
import io.horizontalsystems.bankwallet.modules.createwallet.CreateWalletModule
import io.horizontalsystems.bankwallet.modules.createwallet.CreateWalletPresenter
import io.horizontalsystems.bankwallet.modules.createwallet.CreateWalletRouter
import io.horizontalsystems.bankwallet.modules.createwallet.CreateWalletView
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.views.AlertDialogFragment
import io.horizontalsystems.views.TopMenuItem
import kotlinx.android.synthetic.main.select_coins.*

class CreateWalletActivity : BaseActivity(), CoinItemsAdapter.Listener {
    private lateinit var presenter: CreateWalletPresenter
    private lateinit var coinItemsAdapter: CoinItemsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.select_coins)

        shadowlessToolbar.bind(
                getString(R.string.ManageCoins_title),
                leftBtnItem = TopMenuItem(R.drawable.ic_back) { onBackPressed() },
                rightBtnItem = TopMenuItem(text = R.string.Button_Create, onClick = { presenter.onCreateButtonClick() })
        )
        shadowlessToolbar.setRightButtonEnabled(false)

        val presentationMode: PresentationMode = intent.getParcelableExtra(ModuleField.PRESENTATION_MODE)
                ?: PresentationMode.Initial
        val predefinedAccountType: PredefinedAccountType? = intent.getParcelableExtra(ModuleField.PREDEFINED_ACCOUNT_TYPE)

        presenter = ViewModelProvider(this, CreateWalletModule.Factory(presentationMode, predefinedAccountType)).get(CreateWalletPresenter::class.java)

        observeView(presenter.view as CreateWalletView)
        observeRouter(presenter.router as CreateWalletRouter)

        coinItemsAdapter = CoinItemsAdapter(this)
        coins.adapter = coinItemsAdapter

        presenter.onLoad()
    }

    // CoinItemsAdapter.Listener

    override fun enable(coin: Coin) {
        presenter.onEnable(coin)
    }

    override fun disable(coin: Coin) {
        presenter.onDisable(coin)
    }

    override fun select(coin: Coin) {
        presenter.onSelect(coin)
    }

    private fun observeView(view: CreateWalletView) {
        view.coinsLiveData.observe(this, Observer { coins ->
            coinItemsAdapter.viewItems = coins
            coinItemsAdapter.notifyDataSetChanged()
        })

        view.createButtonEnabled.observe(this, Observer { enabled ->
            shadowlessToolbar.setRightButtonEnabled(enabled)
        })

        view.showNotSupported.observe(this, Observer { predefinedAccountType ->
            AlertDialogFragment.newInstance(
                    titleString = getString(R.string.ManageCoins_Alert_CantCreateTitle, getString(predefinedAccountType.title)),
                    descriptionString = getString(R.string.ManageCoins_Alert_CantCreateDescription, getString(predefinedAccountType.title)),
                    buttonText = R.string.Alert_Ok
            ).show(supportFragmentManager, "alert_dialog")
        })
    }

    private fun observeRouter(router: CreateWalletRouter) {
        router.startMainModuleLiveEvent.observe(this, Observer {
            MainModule.start(this)
            finishAffinity()
        })
        router.showSuccessAndClose.observe(this, Observer {
            HudHelper.showSuccessMessage(findViewById(android.R.id.content), R.string.Hud_Text_Done, HudHelper.SnackbarDuration.LONG)
            finish()
        })
    }
}
