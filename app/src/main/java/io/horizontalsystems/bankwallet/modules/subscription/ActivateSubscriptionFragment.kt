package io.horizontalsystems.bankwallet.modules.subscription

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
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
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.TitleAndValueCell
import io.horizontalsystems.bankwallet.ui.compose.components.TransactionInfoAddressCell
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

                Xxx(
                    mainContent = {
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
                            MessageToSignSection(
                                subscriptionInfo.walletName,
                                subscriptionInfo.walletAddress,
                                subscriptionInfo.messageToSign
                            )
                        }
                    },
                    buttonsContent = {
                        ButtonPrimaryYellow(
                            modifier = Modifier.fillMaxWidth(),
                            title = stringResource(R.string.Button_Sign),
                            onClick = {
                                viewModel.sign()
                            },
                        )
                        Spacer(Modifier.height(16.dp))
                        ButtonPrimaryDefault(
                            modifier = Modifier.fillMaxWidth(),
                            title = stringResource(R.string.Button_Cancel),
                            onClick = {
                                viewModel.cancel()
                                navController.popBackStack()
                            }
                        )
                    }
                )


//                val step = uiState.step
//                when (step) {
//                    ActivateSubscription.Step.FetchingMessageToSign -> {
//                        Xxx(
//                            mainContent = {
//                                Loading()
//                            },
//                            buttonsContent = {
//                                ButtonPrimaryDefault(
//                                    modifier = Modifier.fillMaxWidth(),
//                                    title = stringResource(R.string.Button_Cancel),
//                                    onClick = {
//                                        viewModel.cancel()
//                                        navController.popBackStack()
//                                    }
//                                )
//                            }
//                        )
//                    }
//
//                    ActivateSubscription.Step.FetchingMessageToSignFailed -> {
//                        Xxx(
//                            mainContent = {
//                                ListErrorView(
//                                    errorText = "Error",
//                                    icon = R.drawable.ic_error_48
//                                ) {
//                                    viewModel.fetchMessageToSign()
//                                }
//                            },
//                            buttonsContent = {
//                                ButtonPrimaryDefault(
//                                    modifier = Modifier.fillMaxWidth(),
//                                    title = stringResource(R.string.Button_Cancel),
//                                    onClick = {
//                                        viewModel.cancel()
//                                        navController.popBackStack()
//                                    }
//                                )
//                            }
//                        )
//                    }
//
//                    is ActivateSubscription.Step.FetchingMessageToSignSuccess -> {
//                        Xxx(
//                            mainContent = {
//                                MessageToSignSection(
//                                    step.walletName,
//                                    step.walletAddress,
//                                    step.messageToSign
//                                )
//                            },
//                            buttonsContent = {
//                                ButtonPrimaryYellow(
//                                    modifier = Modifier.fillMaxWidth(),
//                                    title = stringResource(R.string.Button_Sign),
//                                    onClick = {
//                                        viewModel.sign()
//                                    },
//                                )
//                                Spacer(Modifier.height(16.dp))
//                                ButtonPrimaryDefault(
//                                    modifier = Modifier.fillMaxWidth(),
//                                    title = stringResource(R.string.Button_Cancel),
//                                    onClick = {
//                                        viewModel.cancel()
//                                        navController.popBackStack()
//                                    }
//                                )
//                            }
//                        )
//                    }
//
//                    is ActivateSubscription.Step.SendingSignedMessage -> {
//                        Xxx(
//                            mainContent = {
//                                MessageToSignSection(
//                                    step.walletName,
//                                    step.walletAddress,
//                                    step.messageToSign
//                                )
//                            },
//                            buttonsContent = {
//                                ButtonPrimaryYellowWithSpinner(
//                                    modifier = Modifier.fillMaxWidth(),
//                                    title = stringResource(R.string.Button_Activating),
//                                    enabled = false,
//                                    onClick = {},
//                                    showSpinner = true,
//                                )
//                                Spacer(Modifier.height(16.dp))
//                                ButtonPrimaryDefault(
//                                    modifier = Modifier.fillMaxWidth(),
//                                    title = stringResource(R.string.Button_Cancel),
//                                    onClick = {
//                                        viewModel.cancel()
//                                        navController.popBackStack()
//                                    }
//                                )
//                            }
//                        )
//                    }
//
//                    ActivateSubscription.Step.SendingSignedMessageFailed -> {
//
//                    }
//
//                    ActivateSubscription.Step.SendingSignedMessageSuccess -> {
//                        navController.popBackStack()
//                    }
//                }
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

        add {
            TitleAndValueCell(
                stringResource(R.string.ActivateSubscription_MessageToSign),
                messageToSign
            )
        }
    })
    InfoText(text = stringResource(id = R.string.ActivateSubscription_SignMessageDescription))
}

@Composable
private fun ColumnScope.Xxx(
    mainContent: @Composable ColumnScope.() -> Unit,
    buttonsContent: @Composable ColumnScope.() -> Unit
) {
    mainContent.invoke(this)
    Spacer(modifier = Modifier.Companion.weight(1f))
    Column(Modifier.padding(horizontal = 24.dp)) {
        buttonsContent.invoke(this)
        Spacer(Modifier.height(32.dp))
    }
}
