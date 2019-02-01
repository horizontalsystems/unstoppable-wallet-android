package io.horizontalsystems.bankwallet.modules.send

import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.ui.extensions.AddressView
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper

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
                rootView.findViewById<TextView>(R.id.txtFiatAmount)?.text = sendConfirmationViewItem.currencyValue?.let {
                    App.numberFormatter.format(it)
                }
                rootView.findViewById<TextView>(R.id.txtCryptoAmount)?.text = App.numberFormatter.format(sendConfirmationViewItem.coinValue)
                rootView.findViewById<AddressView>(R.id.addressView)?.bind(sendConfirmationViewItem.address)
                rootView.findViewById<TextView>(R.id.txtFeeValue)?.text = sendConfirmationViewItem.feeInfo.getFormatted()
                rootView.findViewById<TextView>(R.id.txtTotalValue)?.text = sendConfirmationViewItem.totalInfo.getFormatted()
            }
        })

        viewModel.coinCodeLiveData.observe(this, Observer { coin ->
            coin?.let { coinCode ->
                context?.let {
                    val coinDrawable = ContextCompat.getDrawable(it, LayoutHelper.getCoinDrawableResource(coinCode))
                    rootView.findViewById<ImageView>(R.id.coinImg)?.setImageDrawable(coinDrawable)
                }
                rootView.findViewById<TextView>(R.id.txtTitle)?.text = getString(R.string.Send_Title, coinCode)
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
