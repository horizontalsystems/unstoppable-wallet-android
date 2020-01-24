package io.horizontalsystems.bankwallet.modules.receive

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper
import io.horizontalsystems.uikit.LayoutHelper
import kotlinx.android.synthetic.main.view_bottom_sheet_receive.*

class ReceiveFragment(
        private val wallet: Wallet,
        private var listener: Listener
) : BaseBottomSheetDialogFragment() {

    interface Listener {
        fun shareReceiveAddress(address: String)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setContentView(R.layout.view_bottom_sheet_receive)

        setTitle(activity?.getString(R.string.Deposit_Title, wallet.coin.code))
        setSubtitle(wallet.coin.title)
        context?.let { setHeaderIcon(LayoutHelper.getCoinDrawableResource(it, wallet.coin.code)) }

        val presenter = ViewModelProviders.of(this, ReceiveModule.Factory(wallet)).get(ReceivePresenter::class.java)
        observeView(presenter.view as ReceiveView)
        observeRouter(presenter.router as ReceiveRouter)
        presenter.viewDidLoad()

        btnShare.setOnClickListener { presenter.onShareClick() }
        receiveAddressView.setOnClickListener { presenter.onAddressClick() }
    }

    private fun observeRouter(receiveRouter: ReceiveRouter) {
        receiveRouter.shareAddress.observe(viewLifecycleOwner, Observer {
            listener.shareReceiveAddress(it)
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
            error?.let { HudHelper.showErrorMessage(it) }
            dismiss()
        })

        view.showCopied.observe(viewLifecycleOwner, Observer {
            HudHelper.showSuccessMessage(R.string.Hud_Text_Copied, 500)
        })
    }

}
