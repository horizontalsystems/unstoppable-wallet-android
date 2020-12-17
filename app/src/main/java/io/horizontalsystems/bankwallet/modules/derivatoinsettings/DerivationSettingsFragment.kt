package io.horizontalsystems.bankwallet.modules.derivatoinsettings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.ui.extensions.ConfirmationDialog
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_address_format_settings.*

class DerivationSettingsFragment : BaseFragment(), DerivationSettingsAdapter.Listener {

    private lateinit var presenter: DerivationSettingsPresenter
    private lateinit var adapter: DerivationSettingsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_address_format_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        presenter = ViewModelProvider(this, DerivationSettingsModule.Factory())
                .get(DerivationSettingsPresenter::class.java)

        presenter.onViewLoad()

        adapter = DerivationSettingsAdapter(this)
        derivationSettingsRecyclerview.adapter = adapter

        observeView(presenter.view as DerivationSettingsView)
    }

    override fun onSettingClick(sectionIndex: Int, settingIndex: Int) {
        presenter.onSelect(sectionIndex, settingIndex)
    }

    private fun observeView(view: DerivationSettingsView) {
        view.derivationSettings.observe(viewLifecycleOwner, Observer { viewItems ->
            adapter.items = viewItems
            adapter.notifyDataSetChanged()
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
                                presenter.onConfirm(derivationSetting)
                            }
                        }
                )
            }
        })
    }

}
