package io.horizontalsystems.bankwallet.modules.subscription

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.findNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.MessageToSign
import io.horizontalsystems.bankwallet.ui.compose.components.TitleAndValueCell
import io.horizontalsystems.bankwallet.ui.compose.components.TransactionInfoAddressCell
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType

class ActivateSubscriptionFragment : BaseFragment() {

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
                val navController = findNavController()
                val address = arguments?.getString(addressKey)

                if (address == null) {
                    HudHelper.showErrorMessage(LocalView.current, R.string.Error_ParameterNotSet)

                    navController.popBackStack()
                } else {
                    ActivateSubscriptionScreen(navController, address)
                }
            }
        }
    }

    companion object {
        private const val addressKey = "addressKey"

        fun prepareParams(address: String) = bundleOf(addressKey to address)
    }
}

@Composable
fun ActivateSubscriptionScreen(navController: NavController, address: String) {
    val viewModel =
        viewModel<ActivateSubscriptionViewModel>(factory = ActivateSubscriptionViewModel.Factory(address))

    val uiState = viewModel.uiState

    val view = LocalView.current

    LaunchedEffect(uiState.fetchingTokenSuccess) {
        if (uiState.fetchingTokenSuccess) {
            navController.popBackStack()
        }
    }
    LaunchedEffect(uiState.fetchingTokenError) {
        uiState.fetchingTokenError?.let { error ->
            HudHelper.showErrorMessage(view, error.message ?: error.javaClass.simpleName)
        }
    }

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    TranslatableString.PlainString(stringResource(R.string.ActivateSubscription_Title)),
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
                            onClick = { navController.popBackStack() }
                        )
                    )
                )
            }
        ) {
            Column(
                modifier = Modifier.padding(it)
            ) {
                if (uiState.fetchingMessage) {
                    Loading()
                }
                uiState.fetchingMessageError?.let { error ->
                    ListErrorView(
                        errorText = error.message ?: error.javaClass.simpleName,
                        icon = R.drawable.ic_error_48
                    ) {
                        viewModel.fetchMessageToSign()
                    }
                }
                uiState.subscriptionInfo?.let { subscriptionInfo ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        MessageToSignSection(
                            subscriptionInfo.walletName,
                            subscriptionInfo.walletAddress,
                            subscriptionInfo.messageToSign
                        )
                    }
                }
                ButtonsGroupWithShade {
                    Column(Modifier.padding(horizontal = 24.dp)) {
                        if (uiState.signButtonState.visible) {
                            ButtonPrimaryYellow(
                                modifier = Modifier.fillMaxWidth(),
                                title = stringResource(R.string.Button_Sign),
                                enabled = uiState.signButtonState.enabled,
                                onClick = {
                                    viewModel.sign()
                                },
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                        ButtonPrimaryDefault(
                            modifier = Modifier.fillMaxWidth(),
                            title = stringResource(R.string.Button_Cancel),
                            onClick = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageToSignSection(
    walletName: String,
    walletAddress: String,
    messageToSign: String
) {
    VSpacer(height = 12.dp)
    CellUniversalLawrenceSection(buildList {
        add {
            TitleAndValueCell(
                stringResource(R.string.ActivateSubscription_Wallet),
                walletName
            )
        }

        add {
            TransactionInfoAddressCell(
                title = stringResource(id = R.string.ActivateSubscription_Address),
                value = walletAddress,
                showAdd = false,
                blockchainType = BlockchainType.Ethereum
            )
        }
    })
    MessageToSign(messageToSign)
}
