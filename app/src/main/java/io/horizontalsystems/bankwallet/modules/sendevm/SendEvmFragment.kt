package io.horizontalsystems.bankwallet.modules.sendevm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.modules.sendevm.confirmation.SendEvmConfirmationModule
import io.horizontalsystems.bankwallet.modules.sendx.AmountInputModeModule
import io.horizontalsystems.bankwallet.modules.sendx.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.core.findNavController

class SendEvmFragment : BaseFragment() {

    private val wallet by lazy { requireArguments().getParcelable<Wallet>(SendEvmModule.walletKey)!! }
    private val vmFactory by lazy { SendEvmModule.Factory(wallet) }
    private val viewModel by navGraphViewModels<SendEvmViewModel>(R.id.sendEvmFragment) { vmFactory }

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
                SendEvmScreen(
                    findNavController(),
                    wallet,
                    viewModel
                )
            }
        }
    }
}

@Composable
fun SendEvmScreen(
    navController: NavController,
    wallet: Wallet,
    viewModel: SendEvmViewModel
) {
    ComposeAppTheme {
        val amountInputModeViewModel = viewModel<AmountInputModeViewModel>(factory = AmountInputModeModule.Factory())

        val fullCoin = wallet.platformCoin.fullCoin
        val proceedEnabled = viewModel.proceedEnabled
        val focusRequester = remember { FocusRequester() }
        val amountInputType = amountInputModeViewModel.inputType

        SideEffect {
            focusRequester.requestFocus()
        }

        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.Send_Title, fullCoin.coin.code),
                navigationIcon = {
                    CoinImage(
                        iconUrl = fullCoin.coin.iconUrl,
                        placeholder = fullCoin.iconPlaceholder,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .size(24.dp)
                    )
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = {
                            navController.popBackStack()
                        }
                    )
                )
            )

            AvailableBalance(
                coin = wallet.coin,
                coinDecimal = viewModel.coinMaxAllowedDecimals,
                fiatDecimal = viewModel.fiatMaxAllowedDecimals,
                availableBalance = viewModel.availableBalance,
                amountInputType = amountInputType
            )

            Spacer(modifier = Modifier.height(12.dp))
            HSAmountInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                focusRequester = focusRequester,
                availableBalance = viewModel.availableBalance,
                coin = wallet.coin,
                coinDecimal = viewModel.coinMaxAllowedDecimals,
                fiatDecimal = viewModel.fiatMaxAllowedDecimals,
                amountValidator = viewModel,
                onClickHint = {
                    amountInputModeViewModel.onToggleInputType()
                },
                onValueChange = {
                    viewModel.onEnterAmount(it)
                },
                inputType = amountInputType
            )

            Spacer(modifier = Modifier.height(12.dp))
            HSAddressInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                coinType = wallet.coinType,
                coinCode = wallet.coin.code
            ) {
                viewModel.onEnterAddress(it)
            }
            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                title = stringResource(R.string.Send_DialogProceed),
                onClick = {
                    viewModel.sendData?.let {
                        navController.slideFromRight(
                            R.id.sendEvmConfirmationFragment,
                            SendEvmConfirmationModule.prepareParams(it)
                        )
                    }
                },
                enabled = proceedEnabled
            )
        }
    }
}
