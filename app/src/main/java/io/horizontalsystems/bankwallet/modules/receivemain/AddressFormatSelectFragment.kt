package io.horizontalsystems.bankwallet.modules.receivemain

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.bitcoinCashCoinType
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.receive.ReceiveFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.SectionUniversalItem
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.TokenType

class AddressFormatSelectFragment : BaseFragment() {

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
                val coinUid = arguments?.getString("coinUid")

                if (coinUid == null) {
                    HudHelper.showErrorMessage(LocalView.current, R.string.Error_ParameterNotSet)
                    navController.popBackStack()
                } else {
                    AddressFormatScreen(navController, coinUid)
                }
            }
        }
    }

    companion object {
        fun prepareParams(coinUid: String) = bundleOf("coinUid" to coinUid)
    }
}

@Composable
fun AddressFormatScreen(
    navController: NavController,
    coinUid: String,
) {
    val viewModel = viewModel<AddressFormatSelectViewModel>(factory = AddressFormatSelectViewModel.Factory(coinUid))

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.Balance_Receive_AddressFormat),
                    navigationIcon = {
                        HsBackButton(onClick = { navController.popBackStack() })
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
                            onClick = {
                                navController.popBackStack(R.id.receiveTokenSelectFragment, true)
                            }
                        )
                    )
                )
            }
        ) {
            Column(Modifier.padding(it)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    InfoText(
                        text = stringResource(R.string.Balance_Receive_AddressFormatDescription)
                    )
                    VSpacer(20.dp)
                    CellUniversalLawrenceSection(viewModel.coinWallets) { wallet ->
                        val addressType = (wallet.token.type as TokenType.AddressTyped).type
                        val bitcoinCashCoinType = addressType.bitcoinCashCoinType

                        SectionUniversalItem {
                            AddressFormatCell(
                                title = stringResource(bitcoinCashCoinType.title),
                                subtitle = bitcoinCashCoinType.value.uppercase(),
                                onClick = {
                                    navController.slideFromBottom(
                                        R.id.receiveFragment,
                                        bundleOf(ReceiveFragment.WALLET_KEY to wallet)
                                    )
                                }
                            )
                        }
                    }
                    VSpacer(32.dp)
                    TextImportantWarning(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = stringResource(R.string.Balance_Receive_AddressFormat_RecommendedAddressType)
                    )
                }
            }
        }
    }
}

@Composable
fun AddressFormatCell(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    RowUniversal(
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            body_leah(text = title)
            subhead2_grey(text = subtitle)
        }
        Icon(
            modifier = Modifier.padding(horizontal = 16.dp),
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey
        )
    }
}
