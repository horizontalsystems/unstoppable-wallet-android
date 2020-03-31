package io.horizontalsystems.bankwallet.modules.bipsettings

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.DerivationSetting
import io.horizontalsystems.feeratekit.model.Coin
import kotlinx.android.synthetic.main.activity_bip_settings.*

class BipSettingsActivity : BaseActivity() {

    private lateinit var presenter: BipSettingsPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bip_settings)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val coinTypes: List<CoinType> = intent.getParcelableArrayListExtra(ModuleField.COIN_TYPES) ?: listOf()
        val showDoneButton: Boolean = intent.getBooleanExtra(ModuleField.SHOW_DONE_BUTTON, false)

        presenter = ViewModelProvider(this, BipSettingsModule.Factory(coinTypes, showDoneButton))
                .get(BipSettingsPresenter::class.java)

        presenter.onViewLoad()

        observeView(presenter.view as BipSettingsView)
        observeRouter(presenter.router as BipSettingsRouter)

        setBtcItems()
        setLtcItems()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.coin_settings_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.menuDone)?.apply {
            isVisible = presenter.showDoneButton
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuDone -> {
                presenter.onDone()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun observeView(view: BipSettingsView) {
        view.btcBipTitle.observe(this, Observer { title ->
            btcHeader.text = title
        })
        view.ltcBipTitle.observe(this, Observer { title ->
            ltcHeader.text = title
        })
        view.btcBipEnabled.observe(this, Observer { enabled ->
            btcBip44.setEnabledState(enabled)
            btcBip49.setEnabledState(enabled)
            btcBip84.setEnabledState(enabled)
        })
        view.btcBipDerivation.observe(this, Observer { derivation ->
            btcBip44.setChecked(derivation == AccountType.Derivation.bip44)
            btcBip49.setChecked(derivation == AccountType.Derivation.bip49)
            btcBip84.setChecked(derivation == AccountType.Derivation.bip84)
        })
        view.ltcBipEnabled.observe(this, Observer { enabled ->
            ltcBip44.setEnabledState(enabled)
            ltcBip49.setEnabledState(enabled)
            ltcBip84.setEnabledState(enabled)
        })
        view.ltcBipDerivation.observe(this, Observer { derivation ->
            ltcBip44.setChecked(derivation == AccountType.Derivation.bip44)
            ltcBip49.setChecked(derivation == AccountType.Derivation.bip49)
            ltcBip84.setChecked(derivation == AccountType.Derivation.bip84)
        })
        view.showDerivationChangeAlert.observe(this, Observer { (derivationSetting, coinTitle) ->
            val bipVersion = AccountType.getDerivationTitle(derivationSetting.derivation)
            BipSettingsAlertDialog.show(
                    title = getString(R.string.BlockchainSettings_BipChangeAlert_Title),
                    subtitle = bipVersion,
                    contentText = getString(R.string.BlockchainSettings_BipChangeAlert_Content, coinTitle, coinTitle),
                    actionButtonTitle = getString(R.string.BlockchainSettings_ChangeAlert_ActionButtonText, bipVersion),
                    activity = this,
                    listener = object : BipSettingsAlertDialog.Listener {
                        override fun onActionButtonClick() {
                            presenter.proceedWithDerivationChange(derivationSetting)
                        }
                    }
            )
        })
    }

    private fun setBtcItems() {
        btcBip44.bind(
                AccountType.getDerivationLongTitle(AccountType.Derivation.bip44),
                getString(R.string.CoinOption_bip44_Subtitle),
                { presenter.onSelect(DerivationSetting(CoinType.Bitcoin, AccountType.Derivation.bip44)) }
        )
        btcBip49.bind(
                AccountType.getDerivationLongTitle(AccountType.Derivation.bip49),
                getString(R.string.CoinOption_bip49_Subtitle),
                { presenter.onSelect(DerivationSetting(CoinType.Bitcoin, AccountType.Derivation.bip49)) }
        )
        btcBip84.bind(
                AccountType.getDerivationLongTitle(AccountType.Derivation.bip84),
                getString(R.string.CoinOption_bip84_Subtitle),
                { presenter.onSelect(DerivationSetting(CoinType.Bitcoin, AccountType.Derivation.bip84)) },
                true
        )
    }

    private fun setLtcItems() {
        ltcBip44.bind(
                AccountType.getDerivationLongTitle(AccountType.Derivation.bip44),
                getString(R.string.CoinOption_bip44_Subtitle),
                { presenter.onSelect(DerivationSetting(CoinType.Litecoin, AccountType.Derivation.bip44)) }
        )
        ltcBip49.bind(
                AccountType.getDerivationLongTitle(AccountType.Derivation.bip49),
                getString(R.string.CoinOption_bip49_Subtitle),
                { presenter.onSelect(DerivationSetting(CoinType.Litecoin, AccountType.Derivation.bip49)) }
        )
        ltcBip84.bind(
                AccountType.getDerivationLongTitle(AccountType.Derivation.bip84),
                getString(R.string.CoinOption_bip84_Subtitle),
                { presenter.onSelect(DerivationSetting(CoinType.Litecoin, AccountType.Derivation.bip84)) },
                true
        )
    }

    private fun observeRouter(router: BipSettingsRouter) {
        router.closeWithResultOk.observe(this, Observer {
            setResult(RESULT_OK)
            finish()
        })

        router.close.observe(this, Observer {
            finish()
        })
    }

}
