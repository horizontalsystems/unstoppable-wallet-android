package io.horizontalsystems.bankwallet.modules.coinsettings

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.putParcelableExtra
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.SyncMode
import kotlinx.android.synthetic.main.activity_coin_settings.*

class CoinSettingsActivity : BaseActivity() {

    private lateinit var presenter: CoinSettingsPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coin_settings)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val coin = intent.getParcelableExtra<Coin>(ModuleField.COIN)
        val coinSettings = intent.getParcelableExtra<CoinSettingsWrapped>(ModuleField.COIN_SETTINGS)

        presenter = ViewModelProvider(this, CoinSettingsModule.Factory(coin, coinSettings.settings)).get(CoinSettingsPresenter::class.java)
        presenter.viewDidLoad()

        observeView(presenter.view as CoinSettingsView)
        observeRouter(presenter.router as CoinSettingsRouter)

        fastSync.setOnClickListener { presenter.onSelect(SyncMode.Fast) }
        slowSync.setOnClickListener { presenter.onSelect(SyncMode.Slow) }

        bip44.setOnClickListener { presenter.onSelect(Derivation.bip44) }
        bip49.setOnClickListener { presenter.onSelect(Derivation.bip49) }
        bip84.setOnClickListener { presenter.onSelect(Derivation.bip84) }
    }

    private fun observeView(view: CoinSettingsView) {
        view.titleData.observe(this, Observer { title ->
            collapsingToolbar.title = title
        })

        view.syncModeLiveEvent.observe(this, Observer {(syncMode, coinTitle) ->
            fastSync.checked = syncMode == SyncMode.Fast
            slowSync.checked = syncMode == SyncMode.Slow
            speedDescriptionOne.text = getString(R.string.CoinOption_Fast_Text, coinTitle)
            speedDescriptionTwo.text = getString(R.string.CoinOption_Slow_Text, coinTitle)
            speedGroup.visibility = View.VISIBLE
        })

        view.derivationLiveEvent.observe(this, Observer {
            bip44.checked = it == Derivation.bip44
            bip49.checked = it == Derivation.bip49
            bip84.checked = it == Derivation.bip84
            bipsGroup.visibility = View.VISIBLE
        })
    }

    private fun observeRouter(router: CoinSettingsRouter) {
        router.notifyOptionsLiveEvent.observe(this, Observer { (coin, coinSettings) ->
            setResult(RESULT_OK, Intent().apply {
                putParcelableExtra(ModuleField.COIN, coin)
                putParcelableExtra(ModuleField.COIN_SETTINGS, CoinSettingsWrapped(coinSettings))
            })

            finish()
        })

        router.onCancelClick.observe(this, Observer {
            setResult(RESULT_CANCELED, Intent())
            finish()
        })
    }

    override fun onBackPressed() {
        presenter.onCancel()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.coin_settings_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.menuEnable ->  {
                presenter.onDone()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}
