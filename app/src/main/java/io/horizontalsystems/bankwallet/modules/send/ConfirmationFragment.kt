package io.horizontalsystems.bankwallet.modules.send

import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.extensions.AddressView
import io.horizontalsystems.bankwallet.ui.extensions.CoinIconView

class ConfirmationFragment : DialogFragment() {

    private lateinit var viewModel: SendViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(SendViewModel::class.java)
        }
    }

    override fun onCreateDialog(bundle: Bundle?): Dialog {
        val builder = activity?.let { AlertDialog.Builder(it, R.style.AlertDialog) }

        val rootView = View.inflate(context, R.layout.fragment_confirmation, null) as ViewGroup
        builder?.setView(rootView)

        viewModel.sendConfirmationViewItemLiveData.observe(this, Observer { viewItem ->
            viewItem?.let { sendConfirmationViewItem ->
                rootView.findViewById<TextView>(R.id.primaryAmountText)?.text = sendConfirmationViewItem.primaryAmountInfo.getFormatted()
                rootView.findViewById<TextView>(R.id.secondaryAmountText)?.text = sendConfirmationViewItem.secondaryAmountInfo?.getFormatted()
                rootView.findViewById<AddressView>(R.id.addressView)?.bind(sendConfirmationViewItem.address)
                rootView.findViewById<TextView>(R.id.txtFeeValue)?.text = sendConfirmationViewItem.feeInfo.getFormatted()
                sendConfirmationViewItem.totalInfo?.getFormatted()?.let {
                    rootView.findViewById<TextView>(R.id.txtTotalValue)?.text = it
                } ?: run {
                    rootView.findViewById<TextView>(R.id.txtTotalTitle)?.visibility = View.GONE
                    rootView.findViewById<TextView>(R.id.txtTotalValue)?.visibility = View.GONE
                }
            }
        })

        viewModel.coinLiveData.observe(this, Observer { coin ->
            coin?.let { coin1 ->
                context?.let {
                    rootView.findViewById<CoinIconView>(R.id.coinIcon)?.bind(coin1)
                }
                rootView.findViewById<TextView>(R.id.txtTitle)?.text = getString(R.string.Send_Title, coin1.code)
            }
        })


        rootView.findViewById<TextView>(R.id.buttonConfirm)?.let { buttonConfirm ->
            buttonConfirm.setOnClickListener {
                viewModel.delegate.onConfirmClicked()
                dismiss()
            }
        }

        val mDialog = builder?.create()
        return mDialog as Dialog
    }

    companion object {
        fun show(activity: FragmentActivity) {
            val fragment = ConfirmationFragment()
            fragment.show(activity.supportFragmentManager, "confirm_send_fragment")
        }
    }

}
