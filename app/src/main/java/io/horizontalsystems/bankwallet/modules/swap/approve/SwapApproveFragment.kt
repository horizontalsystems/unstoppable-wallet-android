package io.horizontalsystems.bankwallet.modules.swap.approve

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.ethereum.EthereumFeeViewModel
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceService
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.setNavigationResult
import kotlinx.android.synthetic.main.fragment_swap_approve.*

class SwapApproveFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_swap_approve, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuClose -> {
                    findNavController().popBackStack()
                    true
                }
                else -> false
            }
        }

        val approveData = requireArguments().getParcelable<SwapAllowanceService.ApproveData>(dataKey)!!

        val vmFactory = SwapApproveModule.Factory(approveData)
        val viewModel by viewModels<SwapApproveViewModel> { vmFactory }
        val feeViewModel by viewModels<EthereumFeeViewModel> { vmFactory }

        amount.setText(viewModel.amount)

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                val amountText = s?.toString() ?: ""

                if (viewModel.validateAmount(amountText)) {
                    viewModel.amount = amountText
                } else {
                    amount.removeTextChangedListener(this)
                    amount.setText(viewModel.amount)
                    amount.setSelection(viewModel.amount.length)
                    amount.startAnimation(AnimationUtils.loadAnimation(context, R.anim.shake_edittext))
                    amount.addTextChangedListener(this)
                }
            }
        }
        amount.addTextChangedListener(watcher)

        btnApprove.setOnSingleClickListener {
            viewModel.onApprove()
        }

        viewModel.approveAllowed.observe(viewLifecycleOwner, Observer {
            btnApprove.isEnabled = it
        })

        viewModel.approveSuccessLiveEvent.observe(viewLifecycleOwner, Observer {
            setNavigationResult(requestKey, bundleOf(resultKey to true))

            findNavController().popBackStack()
        })

        viewModel.approveError.observe(viewLifecycleOwner, Observer {
            HudHelper.showErrorMessage(requireView(), it)
        })

        viewModel.amountError.observe(viewLifecycleOwner, Observer {
            amountError.isVisible = it != null
            amountError.text = it
        })

        viewModel.error.observe(viewLifecycleOwner, Observer {
            error.isVisible = it != null
            error.text = it
        })

        feeSelectorView.setFeeSelectorViewInteractions(
                feeViewModel,
                feeViewModel,
                viewLifecycleOwner,
                parentFragmentManager,
                showSpeedInfoListener = {
                    findNavController().navigate(R.id.swapApproveFragment_to_feeSpeedInfo, null, navOptions())
                }
        )
    }

    companion object {
        const val requestKey = "approve"
        const val resultKey = "result"
        const val dataKey = "data_key"
    }
}
