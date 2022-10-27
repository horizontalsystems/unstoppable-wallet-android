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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.restoreaccount.restore.ByMenu
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.delay

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
                    val popUpToInclusiveId = arguments?.getInt(ManageAccountsModule.popOffOnSuccessKey, -1) ?: -1
                    WatchAddressScreen(findNavController(), popUpToInclusiveId)
                }
            }
        }
    }
}

@Composable
fun WatchAddressScreen(navController: NavController, popUpToInclusiveId: Int) {
    val view = LocalView.current

    val viewModel = viewModel<WatchAddressViewModel>(factory = WatchAddressModule.Factory())
    val uiState = viewModel.uiState
    val accountCreated = uiState.accountCreated
    val submitEnabled = uiState.submitEnabled
    val type = uiState.type

    LaunchedEffect(accountCreated) {
        if (accountCreated) {
            HudHelper.showSuccessMessage(
                contenView = view,
                resId = R.string.Hud_Text_AddressAdded,
                icon = R.drawable.icon_binocule_24,
                iconTint = R.color.white
            )
            delay(300)

            if (popUpToInclusiveId != -1) {
                navController.popBackStack(popUpToInclusiveId, true)
            } else {
                navController.popBackStack()
            }
        }
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
                        enabled = submitEnabled
                    )
                )
            )

            Column {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    ByMenu(
                        menuTitle = stringResource(R.string.Watch_By),
                        menuValue = stringResource(type.titleResId),
                        selectorDialogTitle = stringResource(R.string.Watch_WatchBy),
                        selectorItems = WatchAddressViewModel.Type.values().map {
                            TabItem(stringResource(it.titleResId), it == type, it)
                        },
                        onSelectItem = {
                            viewModel.onSetType(it)
                        }
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                    when (type) {
                        WatchAddressViewModel.Type.Address -> {
                            HSAddressInput(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                tokenQuery = TokenQuery(BlockchainType.Ethereum, TokenType.Native),
                                coinCode = "ETH",
                                onValueChange = viewModel::onEnterAddress
                            )
                        }
                        WatchAddressViewModel.Type.XPubKey -> {
                            FormsInputMultiline(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp),
                                hint = stringResource(id = R.string.Watch_XPubKey_Hint),
                                qrScannerEnabled = true,
                            ) {
                                viewModel.onEnterXPubKey(it)
                            }
                        }
                    }


                    Spacer(Modifier.height(32.dp))
                }

                ButtonsGroupWithShade {
                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        title = stringResource(R.string.Watch_Address_Watch),
                        onClick = {
                            viewModel.onClickWatch()
                        },
                        enabled = submitEnabled
                    )
                }
            }
        }
    }
}
