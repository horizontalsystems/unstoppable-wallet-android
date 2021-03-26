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
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.approve.SwapApproveModule.dataKey
import io.horizontalsystems.bankwallet.modules.swap.approve.SwapApproveModule.requestKey
import io.horizontalsystems.bankwallet.modules.swap.approve.SwapApproveModule.resultKey
import io.horizontalsystems.bankwallet.modules.swap.approve.confirmation.SwapApproveConfirmationModule
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationResult
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
        val viewModel by navGraphViewModels<SwapApproveViewModel>(R.id.swapApproveFragment) { vmFactory }

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

        btnProceed.setOnSingleClickListener {
            viewModel.onProceed()
        }

        viewModel.approveAllowedLiveData.observe(viewLifecycleOwner, {
            btnProceed.isEnabled = it
        })

        viewModel.amountErrorLiveData.observe(viewLifecycleOwner, {
            amountError.isVisible = it != null
            amountError.text = it
        })

        viewModel.openConfirmationLiveEvent.observe(viewLifecycleOwner, { sendEvmData ->
            SwapApproveConfirmationModule.start(this, R.id.swapApproveFragment_to_swapApproveConfirmationFragment, navOptions(), sendEvmData)
        })

        getNavigationResult(requestKey)?.let {
            if (it.getBoolean(resultKey)) {
                setNavigationResult(requestKey, bundleOf(resultKey to true))
                findNavController().popBackStack(R.id.swapFragment, false)
            }
        }
    }

}
