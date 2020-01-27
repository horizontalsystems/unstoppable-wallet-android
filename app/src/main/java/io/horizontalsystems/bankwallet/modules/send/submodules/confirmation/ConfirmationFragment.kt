package io.horizontalsystems.bankwallet.modules.send.submodules.confirmation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.CoinException
import io.horizontalsystems.bankwallet.modules.send.SendPresenter
import io.horizontalsystems.bankwallet.modules.send.SendView
import io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.subviews.ConfirmationPrimaryView
import io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.subviews.ConfirmationSecondaryView
import io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.subviews.ConfirmationSendButtonView
import io.horizontalsystems.uikit.AlertDialogFragment
import io.horizontalsystems.uikit.TopMenuItem
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import kotlinx.android.synthetic.main.fragment_confirmation.*
import java.net.UnknownHostException

class ConfirmationFragment(private var sendPresenter: SendPresenter?) : Fragment() {

    private var sendButtonView: ConfirmationSendButtonView? = null
    private var presenter: SendConfirmationPresenter? = null
    private var sendView: SendView? = null
    private var presenterView: SendConfirmationView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_confirmation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        shadowlessToolbar.bind(
                title = getString(R.string.Send_Confirmation_Title),
                leftBtnItem = TopMenuItem(R.drawable.ic_back, onClick = { activity?.onBackPressed() }
                ))

        sendView = sendPresenter?.view as SendView
        presenter = ViewModelProvider(this, SendConfirmationModule.Factory())
                .get(SendConfirmationPresenter::class.java)

        sendView?.confirmationViewItems?.observe(viewLifecycleOwner, Observer {
            presenter?.confirmationViewItems = it
            presenter?.onViewDidLoad()
        })

        presenterView = presenter?.view as SendConfirmationView

        presenterView?.addPrimaryDataViewItem?.observe(viewLifecycleOwner, Observer { primaryViewItem ->
            context?.let {
                val primaryItemView = ConfirmationPrimaryView(it)
                primaryItemView.bind(primaryViewItem) { presenter?.onReceiverClick() }
                confirmationLinearLayout.addView(primaryItemView)
            }
        })

        presenterView?.showCopied?.observe(viewLifecycleOwner, Observer {
            HudHelper.showSuccessMessage(R.string.Hud_Text_Copied, 500)
        })

        presenterView?.addSecondaryDataViewItem?.observe(viewLifecycleOwner, Observer { secondaryData ->
            context?.let {
                val secondaryDataView = ConfirmationSecondaryView(it)
                secondaryDataView.bind(secondaryData)
                confirmationLinearLayout.addView(secondaryDataView)
            }
        })

        presenterView?.addSendButton?.observe(viewLifecycleOwner, Observer {
            context?.let {
                sendButtonView = ConfirmationSendButtonView(it)
                sendButtonView?.setOnClickListener {
                    sendButtonView?.bind(SendConfirmationModule.SendButtonState.SENDING)
                    sendView?.delegate?.onSendConfirmed()
                }

                confirmationLinearLayout.addView(sendButtonView)
            }
        })

        sendView?.error?.observe(viewLifecycleOwner, Observer { errorMsgTextRes ->
            errorMsgTextRes?.let { HudHelper.showErrorMessage(getErrorText(it)) }
            presenter?.onSendError()
        })

        sendView?.errorInDialog?.observe(viewLifecycleOwner, Observer { coinThrowable ->
            val errorText = coinThrowable.errorTextRes?.let { getString(it) } ?: coinThrowable.nonTranslatableText
            AlertDialogFragment.newInstance(
                    descriptionString = errorText,
                    buttonText = R.string.Alert_Ok,
                    listener = object : AlertDialogFragment.Listener {
                        override fun onButtonClick() {
                            activity?.onBackPressed()
                        }
                    }).show(parentFragmentManager, "alert_dialog")
        })

        presenterView?.sendButtonState?.observe(viewLifecycleOwner, Observer { state ->
            sendButtonView?.bind(state)
        })


    }

    private fun getErrorText(error: Throwable): String {
        return when (error) {
            is UnknownHostException -> getString(R.string.Hud_Text_NoInternet)
            is CoinException -> {
                error.errorTextRes?.let {
                    return getString(it)
                }

                return error.nonTranslatableText ?: getString(R.string.Hud_UnknownError, error)
            }
            else -> getString(R.string.Hud_UnknownError, error)
        }
    }

}
