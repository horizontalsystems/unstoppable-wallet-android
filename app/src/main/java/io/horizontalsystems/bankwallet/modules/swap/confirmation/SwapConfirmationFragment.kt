package io.horizontalsystems.bankwallet.modules.swap.confirmation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.swap.view.SwapViewModel
import io.horizontalsystems.views.TopMenuItem
import kotlinx.android.synthetic.main.fragment_confirmation.shadowlessToolbar
import kotlinx.android.synthetic.main.fragment_confirmation_swap.*

class SwapConfirmationFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_confirmation_swap, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel = activity?.let { ViewModelProvider(it).get(SwapViewModel::class.java) }
        val confirmationPresenter = viewModel?.confirmationPresenter

        shadowlessToolbar.bind(
                title = getString(R.string.Send_Confirmation_Title),
                leftBtnItem = TopMenuItem(R.drawable.ic_back, onClick = {
                    confirmationPresenter?.onCancelConfirmation()
                    activity?.onBackPressed()
                }))

        confirmationPresenter?.confirmationViewItem()?.let {
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
    }

}
