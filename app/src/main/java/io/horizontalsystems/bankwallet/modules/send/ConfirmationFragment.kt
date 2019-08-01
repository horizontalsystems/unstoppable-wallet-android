package io.horizontalsystems.bankwallet.modules.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.extensions.AddressView
import io.horizontalsystems.bankwallet.ui.extensions.ButtonWithProgressbarView
import io.horizontalsystems.bankwallet.ui.extensions.CoinIconView
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper

class ConfirmationFragment : DialogFragment() {

    private lateinit var viewModel: SendViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(SendViewModel::class.java)
        }
        setStyle(STYLE_NO_TITLE, R.style.AlertDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_confirmation, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.sendConfirmationViewItemLiveData.observe(viewLifecycleOwner, Observer { viewItem ->
            viewItem?.let { sendConfirmationViewItem ->
                view.findViewById<CoinIconView>(R.id.coinIcon)?.bind(sendConfirmationViewItem.coin)
                view.findViewById<TextView>(R.id.txtTitle)?.text = getString(R.string.Send_Title, sendConfirmationViewItem.coin.code)
                view.findViewById<TextView>(R.id.primaryAmountText)?.text = sendConfirmationViewItem.primaryAmountInfo.getFormatted()
                view.findViewById<TextView>(R.id.secondaryAmountText)?.text = sendConfirmationViewItem.secondaryAmountInfo?.getFormatted()
                view.findViewById<AddressView>(R.id.addressView)?.bind(sendConfirmationViewItem.address)
                view.findViewById<TextView>(R.id.txtFeeValue)?.text = sendConfirmationViewItem.feeInfo.getFormatted()
                sendConfirmationViewItem.totalInfo?.getFormatted()?.let {
                    view.findViewById<TextView>(R.id.txtTotalValue)?.text = it
                } ?: run {
                    view.findViewById<TextView>(R.id.txtTotalTitle)?.visibility = View.GONE
                    view.findViewById<TextView>(R.id.txtTotalValue)?.visibility = View.GONE
                }
            }
        })

        viewModel.dismissConfirmationLiveEvent.observe(viewLifecycleOwner, Observer {
            dismiss()
        })

        viewModel.errorLiveData.observe(viewLifecycleOwner, Observer {error ->
            error?.let { HudHelper.showErrorMessage(it) }
            view.findViewById<ButtonWithProgressbarView>(R.id.buttonConfirm)?.let { buttonConfirm ->
                buttonConfirm.bind(R.string.Backup_Button_Confirm)
            }
        })

        view.findViewById<ButtonWithProgressbarView>(R.id.buttonConfirm)?.let { buttonConfirm ->
            buttonConfirm.bind(R.string.Backup_Button_Confirm)

            buttonConfirm.setOnClickListener {
                viewModel.delegate.onConfirmClicked()
                buttonConfirm.bind(R.string.Send_Sending, false, true)
            }
        }
    }

    companion object {
        fun show(activity: FragmentActivity) {
            val fragment = ConfirmationFragment()
            fragment.show(activity.supportFragmentManager, "confirm_send_fragment")
        }
    }
}
