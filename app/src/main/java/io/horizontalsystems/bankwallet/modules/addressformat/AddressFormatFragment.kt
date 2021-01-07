package io.horizontalsystems.bankwallet.modules.addressformat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.ui.extensions.ConfirmationDialog
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_address_format_settings.*

class AddressFormatFragment : BaseFragment(), AddressFormatAdapter.Listener {

    private val viewModel by viewModels<AddressFormatViewModel> { AddressFormatModule.Factory() }
    private lateinit var adapter: AddressFormatAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_address_format_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        adapter = AddressFormatAdapter(this)
        derivationSettingsRecyclerview.adapter = adapter

        observeView()
    }

    override fun onSettingClick(sectionIndex: Int, settingIndex: Int) {
        viewModel.onSelect(sectionIndex, settingIndex)
    }

    private fun observeView() {
        viewModel.sections.observe(viewLifecycleOwner, Observer { viewItems ->
            adapter.items = viewItems
            adapter.notifyDataSetChanged()
        })

        viewModel.showAddressFormatChangeAlert.observe(viewLifecycleOwner, Observer { (coinTypeTitle, settingTitle) ->
            activity?.let {
                ConfirmationDialog.show(
                        title = getString(R.string.BlockchainSettings_BipChangeAlert_Title),
                        subtitle = settingTitle,
                        contentText = getString(R.string.BlockchainSettings_BipChangeAlert_Content, coinTypeTitle, coinTypeTitle),
                        actionButtonTitle = getString(R.string.BlockchainSettings_ChangeAlert_ActionButtonText, settingTitle),
                        cancelButtonTitle = getString(R.string.Alert_Cancel),
                        activity = it,
                        listener = object : ConfirmationDialog.Listener {
                            override fun onActionButtonClick() {
                                viewModel.onConfirm()
                            }
                        }
                )
            }
        })
    }

}
