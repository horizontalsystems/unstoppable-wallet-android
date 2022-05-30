package io.horizontalsystems.bankwallet.modules.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeModule
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.send.binance.SendBinanceModule
import io.horizontalsystems.bankwallet.modules.send.binance.SendBinanceScreen
import io.horizontalsystems.bankwallet.modules.send.binance.SendBinanceViewModel
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinModule
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinScreen
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinViewModel
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmScreen
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmViewModel
import io.horizontalsystems.bankwallet.modules.send.zcash.SendZCashModule
import io.horizontalsystems.bankwallet.modules.send.zcash.SendZCashScreen
import io.horizontalsystems.bankwallet.modules.send.zcash.SendZCashViewModel
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.CoinType

class SendFragment : BaseFragment() {

    private val wallet by lazy { requireArguments().getParcelable<Wallet>(walletKey)!! }
    private val amountInputModeViewModel by navGraphViewModels<AmountInputModeViewModel>(R.id.sendXFragment) { AmountInputModeModule.Factory(wallet) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                when (wallet.coinType) {
                    CoinType.Bitcoin,
                    CoinType.Litecoin,
                    CoinType.BitcoinCash,
                    CoinType.Dash,
                    -> {
                        val sendBitcoinViewModel by navGraphViewModels<SendBitcoinViewModel>(R.id.sendXFragment) { SendBitcoinModule.Factory(wallet) }

                        SendBitcoinScreen(findNavController(), sendBitcoinViewModel, amountInputModeViewModel)
                    }
                    is CoinType.Bep2 -> {
                        val sendBinanceViewModel by navGraphViewModels<SendBinanceViewModel>(R.id.sendXFragment) { SendBinanceModule.Factory(wallet) }

                        SendBinanceScreen(findNavController(), sendBinanceViewModel, amountInputModeViewModel)
                    }
                    CoinType.Zcash -> {
                        val sendZCashViewModel by navGraphViewModels<SendZCashViewModel>(R.id.sendXFragment) { SendZCashModule.Factory(wallet) }

                        SendZCashScreen(findNavController(), sendZCashViewModel, amountInputModeViewModel)
                    }
                    CoinType.Ethereum, is CoinType.Erc20,
                    CoinType.BinanceSmartChain, is CoinType.Bep20,
                    CoinType.Polygon, is CoinType.Mrc20 -> {
                        val sendEvmViewModel by navGraphViewModels<SendEvmViewModel>(R.id.sendXFragment) { SendEvmModule.Factory(wallet) }

                        SendEvmScreen(findNavController(), sendEvmViewModel, amountInputModeViewModel)
                    }
                    else -> {

                    }
                }

            }
        }
    }

    companion object {
        private const val walletKey = "walletKey"

        fun prepareParams(wallet: Wallet) = bundleOf(
            walletKey to wallet
        )
    }
}
