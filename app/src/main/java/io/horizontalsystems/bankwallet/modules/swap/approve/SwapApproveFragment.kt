package io.horizontalsystems.bankwallet.modules.swap.approve

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
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

        val coin = requireArguments().getParcelable<Coin>("coin")!!
        val amount = requireArguments().getSerializable("amount") as BigDecimal
        val spenderAddress = requireArguments().getString("spenderAddress")!!

        val viewModel by viewModels<SwapApproveViewModel> {
            SwapApproveModule.Factory(coin, amount, spenderAddress)
        }

        setTitle(getString(R.string.Approve_Title))
        setSubtitle(getString(R.string.Swap_Title))
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

        viewModel.feePresenter.errorLiveEvent.observe(viewLifecycleOwner, Observer {
            feeDataGroup.isVisible = it == null
            feeError.isVisible = it != null

            feeError.text = when (it) {
                is SwapApproveModule.InsufficientFeeBalance -> {
                    getString(R.string.Approve_InsufficientFeeAlert, it.coinValue.coin.title, App.numberFormatter.formatCoin(it.coinValue.value, it.coinValue.coin.code, 0, 8))
                }
                else -> it?.message ?: it.toString()
            }
        })

        viewModel.approveAllowed.observe(viewLifecycleOwner, Observer {
            btnApprove.isEnabled = it
        })

        viewModel.successLiveEvent.observe(viewLifecycleOwner, Observer {
            setFragmentResult(requestKey, bundleOf(resultKey to true))

            dismiss()
        })

        viewModel.errorLiveEvent.observe(viewLifecycleOwner, Observer {
            HudHelper.showErrorMessage(requireView(), it.message ?: it.toString())
        })

    }

    companion object {
        val requestKey = "approve"
        val resultKey = "result"

        fun newInstance(coin: Coin, amount: BigDecimal, spenderAddress: String): SwapApproveFragment {
            return SwapApproveFragment().apply {
                arguments = Bundle(3).apply {
                    putParcelable("coin", coin)
                    putSerializable("amount", amount)
                    putString("spenderAddress", spenderAddress)
                }
            }
        }
    }
}
