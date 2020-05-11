package io.horizontalsystems.bankwallet.modules.addressformat

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation
import io.horizontalsystems.bankwallet.ui.extensions.ConfirmationDialog
import kotlinx.android.synthetic.main.activity_address_format_settings.*

class AddressFormatSettingsActivity : BaseActivity() {

    private lateinit var presenter: AddressFormatSettingsPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_address_format_settings)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val coinTypes: List<CoinType> = intent.getParcelableArrayListExtra(ModuleField.COIN_TYPES)
                ?: listOf()
        val showDoneButton: Boolean = intent.getBooleanExtra(ModuleField.SHOW_DONE_BUTTON, false)

        presenter = ViewModelProvider(this, AddressFormatSettingsModule.Factory(coinTypes, showDoneButton))
                .get(AddressFormatSettingsPresenter::class.java)

        presenter.onViewLoad()

        observeView(presenter.view as AddressFormatSettingsView)
        observeRouter(presenter.router as AddressFormatSettingsRouter)

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

    private fun observeView(view: AddressFormatSettingsView) {
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
            btcBip44.setChecked(derivation == Derivation.bip44)
            btcBip49.setChecked(derivation == Derivation.bip49)
            btcBip84.setChecked(derivation == Derivation.bip84)
        })
        view.ltcBipEnabled.observe(this, Observer { enabled ->
            ltcBip44.setEnabledState(enabled)
            ltcBip49.setEnabledState(enabled)
            ltcBip84.setEnabledState(enabled)
        })
        view.ltcBipDerivation.observe(this, Observer { derivation ->
            ltcBip44.setChecked(derivation == Derivation.bip44)
            ltcBip49.setChecked(derivation == Derivation.bip49)
            ltcBip84.setChecked(derivation == Derivation.bip84)
        })
        view.showDerivationChangeAlert.observe(this, Observer { (derivationSetting, coinTitle) ->
            val bipVersion = derivationSetting.derivation.title()
            ConfirmationDialog.show(
                    title = getString(R.string.BlockchainSettings_BipChangeAlert_Title),
                    subtitle = bipVersion,
                    contentText = getString(R.string.BlockchainSettings_BipChangeAlert_Content, coinTitle, coinTitle),
                    actionButtonTitle = getString(R.string.BlockchainSettings_ChangeAlert_ActionButtonText, bipVersion),
                    cancelButtonTitle = getString(R.string.Alert_Cancel),
                    activity = this,
                    listener = object : ConfirmationDialog.Listener {
                        override fun onActionButtonClick() {
                            presenter.proceedWithDerivationChange(derivationSetting)
                        }
                    }
            )
        })
    }

    private fun setBtcItems() {
        btcBip44.bind(
                Derivation.bip44.longTitle(),
                getString(Derivation.bip44.description()),
                { presenter.onSelect(DerivationSetting(CoinType.Bitcoin, Derivation.bip44)) }
        )
        btcBip49.bind(
                Derivation.bip49.longTitle(),
                getString(Derivation.bip49.description()),
                { presenter.onSelect(DerivationSetting(CoinType.Bitcoin, Derivation.bip49)) }
        )
        btcBip84.bind(
                Derivation.bip84.longTitle(),
                getString(Derivation.bip84.description()),
                { presenter.onSelect(DerivationSetting(CoinType.Bitcoin, Derivation.bip84)) },
                true
        )
    }

    private fun setLtcItems() {
        ltcBip44.bind(
                Derivation.bip44.longTitle(),
                getString(Derivation.bip44.description()),
                { presenter.onSelect(DerivationSetting(CoinType.Litecoin, Derivation.bip44)) }
        )
        ltcBip49.bind(
                Derivation.bip49.longTitle(),
                getString(Derivation.bip49.description()),
                { presenter.onSelect(DerivationSetting(CoinType.Litecoin, Derivation.bip49)) }
        )
        ltcBip84.bind(
                Derivation.bip84.longTitle(),
                getString(Derivation.bip84.description()),
                { presenter.onSelect(DerivationSetting(CoinType.Litecoin, Derivation.bip84)) },
                true
        )
    }

    private fun observeRouter(router: AddressFormatSettingsRouter) {
        router.closeWithResultOk.observe(this, Observer {
            setResult(RESULT_OK)
            finish()
        })

        router.close.observe(this, Observer {
            finish()
        })
    }

}
