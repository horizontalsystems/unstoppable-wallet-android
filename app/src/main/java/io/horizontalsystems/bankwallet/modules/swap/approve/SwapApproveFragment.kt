package io.horizontalsystems.bankwallet.modules.swap.approve

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_swap_approve.*
import java.math.BigDecimal

class SwapApproveFragment : BaseBottomSheetDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setContentView(R.layout.fragment_swap_approve)

        val coin = requireArguments().getParcelable<Coin>(COIN_KEY)!!
        val amount = requireArguments().getSerializable(AMOUNT_KEY) as BigDecimal
        val spenderAddress = requireArguments().getString(SPENDER_ADDRESS_KEY)!!

        val viewModel by viewModels<SwapApproveViewModel> {
            SwapApproveModule.Factory(coin, amount, spenderAddress)
        }

        setTitle(getString(R.string.Approve_Title))
        setSubtitle(getString(R.string.Swap))
        setHeaderIcon(R.drawable.ic_swap)

        coinAmount.text = viewModel.coinAmount
        coinCode.text = viewModel.coinTitle
        txSpeedValue.text = viewModel.feePresenter.txSpeed

        btnApprove.setOnSingleClickListener {
            viewModel.onApprove()
        }

        viewModel.feePresenter.feeValue.observe(viewLifecycleOwner, Observer {
            feeValue.text = it
        })

        viewModel.feePresenter.feeLoading.observe(viewLifecycleOwner, Observer {
            txFeeLoading.isVisible = it
        })

        viewModel.feePresenter.error.observe(viewLifecycleOwner, Observer {
            feeDataGroup.isVisible = it == null
            feeError.isVisible = it != null

            feeError.text = it
        })

        viewModel.approveAllowed.observe(viewLifecycleOwner, Observer {
            btnApprove.isEnabled = it
        })

        viewModel.successLiveEvent.observe(viewLifecycleOwner, Observer {
            setFragmentResult(requestKey, bundleOf(resultKey to true))

            dismiss()
        })

        viewModel.error.observe(viewLifecycleOwner, Observer {
            HudHelper.showErrorMessage(requireView(), it)
        })
    }

    companion object {
        val requestKey = "approve"
        val resultKey = "result"

        const val COIN_KEY = "coin_key"
        const val AMOUNT_KEY = "amount_key"
        const val SPENDER_ADDRESS_KEY = "spender_address_key"

        fun newInstance(coin: Coin, amount: BigDecimal, spenderAddress: String): SwapApproveFragment {
            return SwapApproveFragment().apply {
                arguments = Bundle(3).apply {
                    putParcelable(COIN_KEY, coin)
                    putSerializable(AMOUNT_KEY, amount)
                    putString(SPENDER_ADDRESS_KEY, spenderAddress)
                }
            }
        }
    }
}
