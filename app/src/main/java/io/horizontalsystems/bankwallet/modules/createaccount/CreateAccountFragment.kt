package io.horizontalsystems.bankwallet.modules.createaccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.ui.selector.SelectorBottomSheetDialog
import io.horizontalsystems.bankwallet.ui.selector.SelectorItemViewHolderFactory
import io.horizontalsystems.bankwallet.ui.selector.ViewItemWrapper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_create_account.*

class CreateAccountFragment : BaseFragment() {
    private val viewModel by viewModels<CreateAccountViewModel> { CreateAccountModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_create_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.create -> {
                    viewModel.onClickCreate()
                    true
                }
                else -> false
            }
        }

        viewModel.kindLiveData.observe(viewLifecycleOwner) {
            kind.showValue(it)
        }

        viewModel.finishLiveEvent.observe(viewLifecycleOwner) {
            findNavController().popBackStack()
        }

        viewModel.showErrorLiveEvent.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(requireView(), it)
        }

        kind.setOnSingleClickListener {
            val dialog = SelectorBottomSheetDialog<ViewItemWrapper<CreateAccountModule.Kind>>()

            dialog.titleText = getString(R.string.CreateWallet_Mnemonic)
            dialog.items = viewModel.kindViewItems
            dialog.selectedItem = viewModel.selectedKindViewItem
            dialog.onSelectListener = {
                viewModel.selectedKindViewItem = it
            }
            dialog.itemViewHolderFactory = SelectorItemViewHolderFactory()

            dialog.show(childFragmentManager, "selector_dialog")
        }

    }
}
