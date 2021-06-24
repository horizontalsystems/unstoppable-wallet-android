package io.horizontalsystems.bankwallet.modules.receive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ShareCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_receive.*
import kotlinx.android.synthetic.main.fragment_receive.toolbar

class ReceiveFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_receive, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState != null) {
            //close fragment in case it's restoring
            findNavController().popBackStack()
        }

        try {
            val wallet = arguments?.getParcelable<Wallet>(WALLET_KEY)
                    ?: run { findNavController().popBackStack(); return }
            val viewModel by viewModels<ReceiveViewModel> { ReceiveModule.Factory(wallet) }

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

            receiveAddressView.text = viewModel.receiveAddress
            imgQrCode.setImageBitmap(TextHelper.getQrCodeBitmap(viewModel.receiveAddress))
            testnetLabel.isVisible = viewModel.testNet

            val addressType = if (viewModel.testNet) "Testnet" else viewModel.addressType

            receiverHint.text = when {
                addressType != null -> getString(R.string.Deposit_Your_Address) + " ($addressType)"
                else -> getString(R.string.Deposit_Your_Address)
            }

            val hintColor = if (viewModel.testNet) R.color.lucian else R.color.grey
            receiverHint.setTextColor(view.context.getColor(hintColor))

            btnShare.setOnSingleClickListener {
                context?.let {
                    ShareCompat.IntentBuilder(it)
                            .setType("text/plain")
                            .setText(viewModel.receiveAddress)
                            .startChooser()
                }
            }

            btnCopy.setOnSingleClickListener {
                TextHelper.copyText(viewModel.receiveAddress)
                HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Copied)
            }

            btnClose.setOnSingleClickListener {
                findNavController().popBackStack()
            }
        } catch (t: Throwable) {
            HudHelper.showErrorMessage(this.requireView(), t.message ?: t.javaClass.simpleName)
            findNavController().popBackStack()
        }
    }

    companion object {
        const val WALLET_KEY = "wallet_key"
    }

}
