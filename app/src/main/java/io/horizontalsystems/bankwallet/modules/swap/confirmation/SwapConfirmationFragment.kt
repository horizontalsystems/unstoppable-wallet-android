package io.horizontalsystems.bankwallet.modules.swap.confirmation

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.swap.view.SwapViewModel
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_confirmation_swap.*

class SwapConfirmationFragment : BaseFragment() {

    val viewModel by navGraphViewModels<SwapViewModel>(R.id.swapFragment)

    private lateinit var presenter: ConfirmationPresenter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_confirmation_swap, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.swap_settings_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuCancel -> {
                presenter.onCancelConfirmation()
                findNavController().popBackStack()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        setSupportActionBar(toolbar)

        presenter = viewModel.confirmationPresenter

        swapButton.setOnSingleClickListener {
            swapButton.isEnabled = false
            presenter.onSwap()
        }

        presenter.confirmationViewItem()?.let {
            payTitle.text = it.sendingTitle
            payValue.text = it.sendingValue
            getTitle.text = it.receivingTitle
            getValue.text = it.receivingValue
            minMaxTitle.text = it.minMaxTitle
            minMaxValue.text = it.minMaxValue
            price.text = it.price
            priceImpact.text = it.priceImpact
            swapFee.text = it.swapFee
            txSpeed.text = it.transactionSpeed
            txFee.text = it.transactionFee
            slippageValue.text = it.slippage
            deadlineValue.text = it.deadline
            recipientAddressValue.text = it.recipientAddress

            slippageTitle.isVisible = it.slippage != null
            slippageValue.isVisible = it.slippage != null
            deadlineTitle.isVisible = it.deadline != null
            deadlineValue.isVisible = it.deadline != null
            recipientAddressTitle.isVisible = it.recipientAddress != null
            recipientAddressValue.isVisible = it.recipientAddress != null
        }

        presenter.swapButtonEnabled.observe(viewLifecycleOwner, Observer { isEnabled ->
            swapButton.isEnabled = isEnabled
        })

        presenter.swapButtonTitle.observe(viewLifecycleOwner, Observer { title ->
            swapButton.text = title
        })

        presenter.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let { HudHelper.showErrorMessage(requireActivity().findViewById(android.R.id.content), it) }
        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(
                this,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        presenter.onCancelConfirmation()

                        if (isEnabled) {
                            isEnabled = false
                            findNavController().popBackStack()
                        }
                    }
                }
        )
    }
}
