package io.horizontalsystems.bankwallet.modules.blockchainsettings

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.SyncMode
import kotlinx.android.synthetic.main.activity_coin_settings.*

class BlockchainSettingsActivity : BaseActivity() {

    private lateinit var presenter: BlockchainSettingsPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coin_settings)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val coinType = intent.getParcelableExtra<CoinType>(ModuleField.COIN_TYPE) ?: run { finish(); return }

        presenter = ViewModelProvider(this, BlockchainSettingsModule.Factory(coinType))
                .get(BlockchainSettingsPresenter::class.java)

        presenter.onViewLoad()

        observeView(presenter.view as BlockchainSettingsView)
        observeRouter(presenter.router as BlockchainSettingsRouter)

        bip44.bind(AccountType.getDerivationLongTitle(Derivation.bip44), getString(R.string.CoinOption_bip44_Subtitle), { presenter.onSelect(Derivation.bip44) })
        bip49.bind(AccountType.getDerivationLongTitle(Derivation.bip49), getString(R.string.CoinOption_bip49_Subtitle), { presenter.onSelect(Derivation.bip49) })
        bip84.bind(AccountType.getDerivationLongTitle(Derivation.bip84), getString(R.string.CoinOption_bip84_Subtitle), { presenter.onSelect(Derivation.bip84) }, true)

        apiSource.bind(
                getString(R.string.CoinOption_Fast),
                getString(R.string.CoinOption_Fast_Subtitle),
                { presenter.onSelect(SyncMode.Fast) })
        blockchainSource.bind(
                getString(R.string.CoinOption_Slow),
                getString(R.string.CoinOption_Slow_Subtitle),
                { presenter.onSelect(SyncMode.Slow) },
                true)
    }

    private fun observeView(view: BlockchainSettingsView) {
        view.titleLiveEvent.observe(this, Observer { coinTitle ->
            toolbar.title = getString(R.string.BlockchainSettings_CoinSettings, coinTitle)
        })

        view.derivationLiveEvent.observe(this, Observer { derivation ->
            derivationWrapper.visibility = View.VISIBLE
            bip44.setChecked(derivation == Derivation.bip44)
            bip49.setChecked(derivation == Derivation.bip49)
            bip84.setChecked(derivation == Derivation.bip84)
        })

        view.syncModeLiveEvent.observe(this, Observer { syncMode ->
            syncModeWrapper.visibility = View.VISIBLE
            apiSource.setChecked(syncMode == SyncMode.Fast)
            blockchainSource.setChecked(syncMode == SyncMode.Slow)
        })

        view.sourceLinkLiveEvent.observe(this, Observer { coinType->
            sourceDescription.text = getString(R.string.CoinOption_RestoreSource, coinType.restoreUrl())
        })

        view.showDerivationChangeAlert.observe(this, Observer { (bip,coinTitle) ->
            val bipVersion = AccountType.getDerivationTitle(bip)
            BlockchainSettingsAlertDialog.show(
                    title = getString(R.string.BlockchainSettings_BipChangeAlert_Title),
                    subtitle = bipVersion,
                    contentText = getString(R.string.BlockchainSettings_BipChangeAlert_Content, coinTitle, coinTitle),
                    actionButtonTitle = getString(R.string.BlockchainSettings_ChangeAlert_ActionButtonText, bipVersion),
                    activity = this,
                    listener = object : BlockchainSettingsAlertDialog.Listener {
                        override fun onActionButtonClick() {
                            presenter.proceedWithDerivationChange(bip)
                        }
                    }
            )
        })

        view.showSyncModeChangeAlert.observe(this, Observer { (syncMode, coinTitle) ->
            val syncModeText = getSyncModeText(syncMode)

            BlockchainSettingsAlertDialog.show(
                    title = getString(R.string.BlockchainSettings_SyncModeChangeAlert_Title),
                    subtitle = syncModeText,
                    contentText = getString(R.string.BlockchainSettings_SyncModeChangeAlert_Content, coinTitle),
                    actionButtonTitle = getString(R.string.Button_Change),
                    activity = this,
                    listener = object : BlockchainSettingsAlertDialog.Listener {
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

    private fun observeRouter(router: BlockchainSettingsRouter) {
        router.closeWithResultOk.observe(this, Observer {
            setResult(RESULT_OK)
            finish()
        })

        router.close.observe(this, Observer {
            finish()
        })
    }

}
