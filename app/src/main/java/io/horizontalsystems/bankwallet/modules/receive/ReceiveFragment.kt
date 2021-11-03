package io.horizontalsystems.bankwallet.modules.receive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString.ResString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.FullCoin
import kotlinx.android.synthetic.main.fragment_receive.*

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

            setToolbar(wallet.platformCoin.fullCoin)

            receiveAddressView.text = viewModel.receiveAddress
            receiveAddressView.setOnClickListener {
                copyAddress(viewModel.receiveAddress)
            }

            imgQrCode.setImageBitmap(TextHelper.getQrCodeBitmap(viewModel.receiveAddress))
            testnetLabel.isVisible = viewModel.testNet

            val addressType = if (viewModel.testNet) "Testnet" else viewModel.addressType

            receiverHint.text = when {
                addressType != null -> getString(R.string.Deposit_Your_Address) + " ($addressType)"
                else -> getString(R.string.Deposit_Your_Address)
            }

            val hintColor = if (viewModel.testNet) R.color.lucian else R.color.grey
            receiverHint.setTextColor(view.context.getColor(hintColor))

            buttonCloseCompose.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            btnsCopyShareCompose.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setCloseButton()

            setCopyShareButtons(viewModel)
        } catch (t: Throwable) {
            HudHelper.showErrorMessage(this.requireView(), t.message ?: t.javaClass.simpleName)
            findNavController().popBackStack()
        }

    }

    private fun setToolbar(fullCoin: FullCoin) {
        toolbarCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        toolbarCompose.setContent {
            ComposeAppTheme {
                AppBar(
                    title = ResString(R.string.Deposit_Title, fullCoin.coin.code),
                    navigationIcon = {
                        CoinImage(
                            iconUrl = fullCoin.coin.iconUrl,
                            placeholder = fullCoin.iconPlaceholder,
                            modifier = Modifier.padding(horizontal = 16.dp).size(24.dp)
                        )
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
                            onClick = {
                                findNavController().popBackStack()
                            }
                        )
                    )
                )
            }
        }
    }

    private fun setCopyShareButtons(viewModel: ReceiveViewModel) {
        btnsCopyShareCompose.setContent {
            ComposeAppTheme {
                Row(
                    modifier = Modifier.width(IntrinsicSize.Max)
                        .padding(top = 23.dp, bottom = 23.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    ButtonSecondaryDefault(
                        modifier = Modifier.padding(end = 6.dp),
                        title = getString(R.string.Alert_Copy),
                        onClick = {
                            copyAddress(viewModel.receiveAddress)
                        }
                    )
                    ButtonSecondaryDefault(
                        modifier = Modifier.padding(start = 6.dp),
                        title = getString(R.string.Deposit_Share),
                        onClick = {
                            context?.let {
                                ShareCompat.IntentBuilder(it)
                                    .setType("text/plain")
                                    .setText(viewModel.receiveAddress)
                                    .startChooser()
                            }
                        }
                    )
                }
            }
        }
    }

    private fun setCloseButton() {
        buttonCloseCompose.setContent {
            ComposeAppTheme {
                ButtonPrimaryYellow(
                    modifier = Modifier.padding(
                        start = 24.dp,
                        top = 24.dp,
                        end = 24.dp,
                        bottom = 44.dp
                    ),
                    title = getString(R.string.Button_Close),
                    onClick = {
                        findNavController().popBackStack()
                    }
                )
            }
        }
    }

    private fun copyAddress(address: String) {
        TextHelper.copyText(address)
        HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Copied)
    }

    companion object {
        const val WALLET_KEY = "wallet_key"
    }

}
