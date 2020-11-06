package io.horizontalsystems.bankwallet.modules.addressformat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation
import io.horizontalsystems.bankwallet.ui.extensions.ConfirmationDialog
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_address_format_settings.*

class AddressFormatSettingsFragment : BaseFragment() {

    private lateinit var presenter: AddressFormatSettingsPresenter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_address_format_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setNavigationToolbar(toolbar, findNavController())

        val coinTypes = arguments?.getParcelableArrayList(ModuleField.COIN_TYPES)
                ?: listOf<CoinType>()

        presenter = ViewModelProvider(this, AddressFormatSettingsModule.Factory(coinTypes))
                .get(AddressFormatSettingsPresenter::class.java)

        presenter.onViewLoad()

        observeView(presenter.view as AddressFormatSettingsView)

        (presenter.router as AddressFormatSettingsRouter).close.observe(viewLifecycleOwner, Observer {
            findNavController().popBackStack()
        })

        setBtcItems()
        setLtcItems()
    }

    private fun observeView(view: AddressFormatSettingsView) {
        view.btcBipTitle.observe(viewLifecycleOwner, Observer { title ->
            btcHeader.text = title
        })
        view.ltcBipTitle.observe(viewLifecycleOwner, Observer { title ->
            ltcHeader.text = title
        })
        view.btcBipVisibility.observe(viewLifecycleOwner, Observer { isVisible ->
            btcHeader.isVisible = isVisible
            btcBip44.isVisible = isVisible
            btcBip49.isVisible = isVisible
            btcBip84.isVisible = isVisible
        })
        view.btcBipDerivation.observe(viewLifecycleOwner, Observer { derivation ->
            btcBip44.setChecked(derivation == Derivation.bip44)
            btcBip49.setChecked(derivation == Derivation.bip49)
            btcBip84.setChecked(derivation == Derivation.bip84)
        })
        view.ltcBipVisibility.observe(viewLifecycleOwner, Observer { isVisible ->
            ltcHeader.isVisible = isVisible
            ltcBip44.isVisible = isVisible
            ltcBip49.isVisible = isVisible
            ltcBip84.isVisible = isVisible
        })
        view.ltcBipDerivation.observe(viewLifecycleOwner, Observer { derivation ->
            ltcBip44.setChecked(derivation == Derivation.bip44)
            ltcBip49.setChecked(derivation == Derivation.bip49)
            ltcBip84.setChecked(derivation == Derivation.bip84)
        })
        view.showDerivationChangeAlert.observe(viewLifecycleOwner, Observer { (derivationSetting, coinTitle) ->
            activity?.let {
                val bipVersion = derivationSetting.derivation.title()
                ConfirmationDialog.show(
                        title = getString(R.string.BlockchainSettings_BipChangeAlert_Title),
                        subtitle = bipVersion,
                        contentText = getString(R.string.BlockchainSettings_BipChangeAlert_Content, coinTitle, coinTitle),
                        actionButtonTitle = getString(R.string.BlockchainSettings_ChangeAlert_ActionButtonText, bipVersion),
                        cancelButtonTitle = getString(R.string.Alert_Cancel),
                        activity = it,
                        listener = object : ConfirmationDialog.Listener {
                            override fun onActionButtonClick() {
                                presenter.proceedWithDerivationChange(derivationSetting)
                            }
                        }
                )
            }
        })
    }

    private fun setBtcItems() {
        val btcCoinType = CoinType.Bitcoin

        btcBip44.bind(
                Derivation.bip44.longTitle(),
                getString(Derivation.bip44.description(), Derivation.bip44.addressPrefix(btcCoinType)),
                { presenter.onSelect(DerivationSetting(CoinType.Bitcoin, Derivation.bip44)) }
        )
        btcBip49.bind(
                Derivation.bip49.longTitle(),
                getString(Derivation.bip49.description(), Derivation.bip49.addressPrefix(btcCoinType)),
                { presenter.onSelect(DerivationSetting(CoinType.Bitcoin, Derivation.bip49)) }
        )
        btcBip84.bind(
                Derivation.bip84.longTitle(),
                getString(Derivation.bip84.description(), Derivation.bip84.addressPrefix(btcCoinType)),
                { presenter.onSelect(DerivationSetting(CoinType.Bitcoin, Derivation.bip84)) },
                true
        )
    }

    private fun setLtcItems() {
        val ltcCoinType = CoinType.Litecoin

        ltcBip44.bind(
                Derivation.bip44.longTitle(),
                getString(Derivation.bip44.description(), Derivation.bip44.addressPrefix(ltcCoinType)),
                { presenter.onSelect(DerivationSetting(CoinType.Litecoin, Derivation.bip44)) }
        )
        ltcBip49.bind(
                Derivation.bip49.longTitle(),
                getString(Derivation.bip49.description(), Derivation.bip49.addressPrefix(ltcCoinType)),
                { presenter.onSelect(DerivationSetting(CoinType.Litecoin, Derivation.bip49)) }
        )
        ltcBip84.bind(
                Derivation.bip84.longTitle(),
                getString(Derivation.bip84.description(), Derivation.bip84.addressPrefix(ltcCoinType)),
                { presenter.onSelect(DerivationSetting(CoinType.Litecoin, Derivation.bip84)) },
                true
        )
    }

}
