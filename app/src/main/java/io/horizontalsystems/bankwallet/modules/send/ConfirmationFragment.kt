package io.horizontalsystems.bankwallet.modules.send

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.extensions.AddressView
import io.horizontalsystems.bankwallet.ui.extensions.ButtonWithProgressbarView
import io.horizontalsystems.bankwallet.ui.extensions.CoinIconView

class ConfirmationFragment : DialogFragment() {

    private lateinit var viewModel: SendViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(SendViewModel::class.java)
        }
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.AlertDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_confirmation, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.sendConfirmationViewItemLiveData.observe(this, Observer { viewItem ->
            viewItem?.let { sendConfirmationViewItem ->
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

        viewModel.coinLiveData.observe(this, Observer { coin ->
            coin?.let { coin1 ->
                context?.let {
                    view.findViewById<CoinIconView>(R.id.coinIcon)?.bind(coin1)
                }
                view.findViewById<TextView>(R.id.txtTitle)?.text = getString(R.string.Send_Title, coin1.code)
            }
        })

        viewModel.dismissConfirmationLiveEvent.observe(this, Observer {
            dismiss()
        })

        viewModel.errorLiveData.observe(this, Observer {
            view.findViewById<ButtonWithProgressbarView>(R.id.buttonConfirm)?.let { buttonConfirm ->
                isCancelable = true
                buttonConfirm.bind(R.string.Backup_Button_Confirm)
            }
        })

        view.findViewById<ButtonWithProgressbarView>(R.id.buttonConfirm)?.let { buttonConfirm ->
            buttonConfirm.bind(R.string.Backup_Button_Confirm)

            buttonConfirm.setOnClickListener {
                viewModel.delegate.onConfirmClicked()
                isCancelable = false
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
