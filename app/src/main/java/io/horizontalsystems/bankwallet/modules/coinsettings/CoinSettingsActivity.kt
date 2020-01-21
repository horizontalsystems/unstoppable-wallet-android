package io.horizontalsystems.bankwallet.modules.coinsettings

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.ui.dialogs.CoinSettingsAlertDialog
import kotlinx.android.synthetic.main.activity_coin_settings.*


class CoinSettingsActivity : BaseActivity() {

    private lateinit var presenter: CoinSettingsPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coin_settings)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val settingsMode = intent.getParcelableExtra(ModuleField.COIN_SETTINGS_CLOSE_MODE)
                ?: SettingsMode.StandAlone

        presenter = ViewModelProvider(this, CoinSettingsModule.Factory(settingsMode))
                .get(CoinSettingsPresenter::class.java)

        presenter.onLoad()

        observeView(presenter.view as CoinSettingsView)
        observeRouter(presenter.router as CoinSettingsRouter)

        bip44.setClick { presenter.onSelect(Derivation.bip44) }
        bip49.setClick { presenter.onSelect(Derivation.bip49) }
        bip84.setClick { presenter.onSelect(Derivation.bip84) }

        apiSource.setClick { presenter.onSelect(SyncMode.Fast) }
        blockchainSource.setClick { presenter.onSelect(SyncMode.Slow) }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.coin_settings_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menuDone -> {
                presenter.onDone()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.menuDone)?.apply {
            isVisible = presenter.showDoneButton
        }
        return true
    }

    private fun observeView(view: CoinSettingsView) {
        view.selection.observe(this, Observer { (derivation, syncMode) ->
            bip44.bindSelection(derivation == Derivation.bip44)
            bip49.bindSelection(derivation == Derivation.bip49)
            bip84.bindSelection(derivation == Derivation.bip84)

            apiSource.bindSelection(syncMode == SyncMode.Fast)
            blockchainSource.bindSelection(syncMode == SyncMode.Slow)
        })

        view.showDerivationChangeAlert.observe(this, Observer { bip ->
            val bipVersion = AccountType.getDerivationTitle(bip)
            CoinSettingsAlertDialog.show(
                    title = getString(R.string.BlockchainSettings_BipChangeAlert_Title),
                    subtitle = bipVersion,
                    contentText = getString(R.string.BlockchainSettings_BipChangeAlert_Content),
                    actionButtonTitle = getString(R.string.BlockchainSettings_ChangeAlert_ActionButtonText, bipVersion),
                    activity = this,
                    listener = object : CoinSettingsAlertDialog.Listener {
                        override fun onActionButtonClick() {
                            presenter.proceedWithDerivationChange(bip)
                        }
                    }
            )
        })

        view.showSyncModeChangeAlert.observe(this, Observer { syncMode ->
            val syncModeText = getSyncModeText(syncMode)

            CoinSettingsAlertDialog.show(
                    title = getString(R.string.BlockchainSettings_BipChangeAlert_Title),
                    subtitle = syncModeText,
                    contentText = getString(R.string.BlockchainSettings_SyncModeChangeAlert_Content),
                    actionButtonTitle = getString(R.string.BlockchainSettings_ChangeAlert_ActionButtonText, syncModeText),
                    activity = this,
                    listener = object : CoinSettingsAlertDialog.Listener {
                        override fun onActionButtonClick() {
                            presenter.proceedWithSyncModeChange(syncMode)
                        }
                    }
            )
        })
    }

    private fun getSyncModeText(syncMode: SyncMode?): String {
        return getString(if (syncMode == SyncMode.Slow) R.string.BlockchainSettings_SyncMode_Blockchain else R.string.BlockchainSettings_SyncMode_Api)
    }

    private fun observeRouter(router: CoinSettingsRouter) {
        router.closeWithResultOk.observe(this, Observer {
            setResult(RESULT_OK)
            finish()
        })

        router.close.observe(this, Observer {
            finish()
        })
    }

}
