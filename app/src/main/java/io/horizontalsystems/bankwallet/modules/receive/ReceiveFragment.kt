package io.horizontalsystems.bankwallet.modules.receive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ShareCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.snackbar.SnackbarGravity
import kotlinx.android.synthetic.main.fragment_backup_words_confirm.*
import kotlinx.android.synthetic.main.fragment_receive.*
import kotlinx.android.synthetic.main.fragment_receive.toolbar

class ReceiveFragment: BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_receive, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState != null) {
            //close fragment in case it's restoring
            findNavController().popBackStack()
        }

        val wallet = arguments?.getParcelable<Wallet>(WALLET_KEY) ?: run { findNavController().popBackStack(); return }

        toolbar.title = getString(R.string.Deposit_Title, wallet.coin.code)
        toolbar.navigationIcon = AppLayoutHelper.getCoinDrawable(requireContext(), wallet.coin.type)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.closeButton -> {
                    findNavController().popBackStack()
                    true
                }
                else -> false
            }
        }

        val presenter = ViewModelProvider(this, ReceiveModule.Factory(wallet)).get(ReceivePresenter::class.java)
        observeView(presenter.view as ReceiveView)
        observeRouter(presenter.router as ReceiveRouter)
        presenter.viewDidLoad()

        btnShare.setOnSingleClickListener {
            presenter.onShareClick()
        }

        btnCopy.setOnSingleClickListener {
            presenter.onAddressClick()
        }

        btnClose.setOnSingleClickListener {
            findNavController().popBackStack()
        }
    }

    private fun observeRouter(receiveRouter: ReceiveRouter) {
        receiveRouter.shareAddress.observe(viewLifecycleOwner, Observer { address ->
            activity?.let {
                ShareCompat.IntentBuilder
                    .from(it)
                    .setType("text/plain")
                    .setText(address)
                    .startChooser()
            }
        })
    }

    private fun observeView(view: ReceiveView) {
        view.setHintText.observe(viewLifecycleOwner, Observer {
            receiverHint.setText(it)

            view.hintDetails?.let {
                receiverHint.text = "${receiverHint.text} (${it})"
            }
        })

        view.showAddress.observe(viewLifecycleOwner, Observer {
            receiveAddressView.text = it.address
            imgQrCode.setImageBitmap(TextHelper.getQrCodeBitmap(it.address))
        })

        view.showError.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
                HudHelper.showErrorMessage(this.requireView(), it, gravity = SnackbarGravity.TOP_OF_VIEW)
            }

            findNavController().popBackStack()
        })

        view.showCopied.observe(viewLifecycleOwner, Observer {
            HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Copied)
        })
    }

    companion object {
        const val WALLET_KEY = "wallet_key"
    }

}
