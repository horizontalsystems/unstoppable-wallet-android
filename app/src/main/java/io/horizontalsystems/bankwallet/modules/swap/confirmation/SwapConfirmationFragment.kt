package io.horizontalsystems.bankwallet.modules.swap.confirmation

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.swap.view.SwapViewModel
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.views.TopMenuItem
import kotlinx.android.synthetic.main.fragment_confirmation.shadowlessToolbar
import kotlinx.android.synthetic.main.fragment_confirmation_swap.*

class SwapConfirmationFragment : BaseFragment() {

    private lateinit var presenter: ConfirmationPresenter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_confirmation_swap, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel = ViewModelProvider(requireActivity()).get(SwapViewModel::class.java)
        presenter = viewModel.confirmationPresenter

        shadowlessToolbar.bind(
                title = getString(R.string.Send_Confirmation_Title),
                leftBtnItem = TopMenuItem(R.drawable.ic_back, onClick = {
                    presenter.onCancelConfirmation()
                    requireActivity().onBackPressed()
                }),
                rightBtnItem = TopMenuItem(text = R.string.Button_Cancel, onClick = {
                    presenter.onCancelConfirmation()
                    requireActivity().onBackPressed()
                }))

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
                            requireActivity().onBackPressed()
                        }
                    }
                }
        )
    }

    companion object {
        fun start(activity: FragmentActivity) {
            activity.supportFragmentManager.commit {
                add(R.id.fragmentContainerView, SwapConfirmationFragment())
                addToBackStack(null)
            }
        }
    }

}
