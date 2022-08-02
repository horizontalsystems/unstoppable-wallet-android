package io.horizontalsystems.bankwallet.modules.watchaddress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType

class WatchAddressFragment : BaseFragment() {

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
                ComposeAppTheme {
                    WatchAddressScreen(findNavController())
                }
            }
        }
    }
}

@Composable
fun WatchAddressScreen(navController: NavController) {
    val viewModel = viewModel<WatchAddressViewModel>(factory = WatchAddressModule.Factory())

    if (viewModel.accountCreated) {
        navController.popBackStack()
    }

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.Watch_Address_Title),
                navigationIcon = {
                    HsIconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Watch_Address_Watch),
                        onClick = {
                            viewModel.onClickWatch()
                        },
                        enabled = viewModel.submitEnabled
                    )
                )
            )

            val focusRequester = remember { FocusRequester() }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                HeaderText(text = stringResource(R.string.Watch_Address_Title))

                HSAddressInput(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .focusRequester(focusRequester),
                    tokenQuery = TokenQuery(BlockchainType.Ethereum, TokenType.Native),
                    coinCode = "ETH",
                    onValueChange = viewModel::onEnterAddress
                )
                InfoText(text = stringResource(R.string.Watch_Address_Description))

                Spacer(Modifier.height(24.dp))

                HeaderText(text = stringResource(R.string.Restore_Name))

                FormsInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    initial = viewModel.nameState,
                    hint = viewModel.defaultName,
                    pasteEnabled = false,
                    onValueChange = {
                        viewModel.onNameChange(it)
                    }
                )

                Spacer(Modifier.height(32.dp))
            }

            SideEffect {
                focusRequester.requestFocus()
            }
        }
    }
}
