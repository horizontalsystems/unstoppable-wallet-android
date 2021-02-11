package io.horizontalsystems.bankwallet.modules.send.submodules.confirmation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.LocalizedException
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.send.SendPresenter
import io.horizontalsystems.bankwallet.modules.send.SendView
import io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.subviews.ConfirmationPrimaryView
import io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.subviews.ConfirmationSecondaryView
import io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.subviews.ConfirmationSendButtonView
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_confirmation.*
import java.net.UnknownHostException

class ConfirmationFragment(private var sendPresenter: SendPresenter?) : BaseFragment() {

    private var sendButtonView: ConfirmationSendButtonView? = null
    private val presenter by activityViewModels<SendConfirmationPresenter> { SendConfirmationModule.Factory() }
    private var sendView: SendView? = null
    private var presenterView: SendConfirmationView? = null
    private val logger = AppLogger("send")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_confirmation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuClose -> {
                    requireActivity().finish()
                    true
                }
                else -> false
            }
        }

        sendView = sendPresenter?.view as SendView

        sendView?.confirmationViewItems?.observe(viewLifecycleOwner, Observer {
            presenter.confirmationViewItems = it
            presenter.onViewDidLoad()
        })

        presenterView = presenter.view as SendConfirmationView

        presenterView?.addPrimaryDataViewItem?.observe(viewLifecycleOwner, Observer { primaryViewItem ->
            context?.let {
                val primaryItemView = ConfirmationPrimaryView(it)
                primaryItemView.bind(primaryViewItem) { presenter.onReceiverClick() }
                confirmationLinearLayout.addView(primaryItemView)
            }
        })

        presenterView?.showCopied?.observe(viewLifecycleOwner, Observer {
            HudHelper.showSuccessMessage(this.requireView(), R.string.Hud_Text_Copied)
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
                sendButtonView?.setOnSingleClickListener {
                    val actionLogger = logger.getScopedUnique()

                    actionLogger.info("click")

                    sendButtonView?.isEnabled = false
                    sendButtonView?.bind(SendConfirmationModule.SendButtonState.SENDING)
                    sendView?.delegate?.onSendConfirmed(actionLogger)
                }

                confirmationLinearLayout.addView(sendButtonView)
            }
        })

        sendView?.error?.observe(viewLifecycleOwner, Observer { errorMsgTextRes ->
            errorMsgTextRes?.let {
                HudHelper.showErrorMessage(this.requireView(), getErrorText(it))
            }
            presenter.onSendError()
        })

        presenterView?.sendButtonState?.observe(viewLifecycleOwner, Observer { state ->
            sendButtonView?.bind(state)
            sendButtonView?.isEnabled = state == SendConfirmationModule.SendButtonState.ACTIVE
        })
    }

    private fun getErrorText(error: Throwable): String {
        return when (error) {
            is UnknownHostException -> getString(R.string.Hud_Text_NoInternet)
            is LocalizedException -> getString(error.errorTextRes)
            else -> getString(R.string.Hud_UnknownError, error)
        }
    }

}
