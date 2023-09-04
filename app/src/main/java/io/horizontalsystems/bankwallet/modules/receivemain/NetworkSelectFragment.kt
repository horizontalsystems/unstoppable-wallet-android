package io.horizontalsystems.bankwallet.modules.receivemain

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.description
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.receive.address.ReceiveAddressFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.SectionUniversalItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class NetworkSelectFragment : BaseFragment() {

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
                val popupDestinationId = arguments?.getInt(
                    ReceiveAddressFragment.POPUP_DESTINATION_ID_KEY
                )

                if (coinUid == null) {
                    HudHelper.showErrorMessage(LocalView.current, R.string.Error_ParameterNotSet)
                    navController.popBackStack()
                } else {
                    NetworkSelectScreen(navController, coinUid, popupDestinationId)
                }

            }
        }
    }

    companion object {
        fun prepareParams(coinUid: String, popupDestinationId: Int?): Bundle {
            return bundleOf(
                "coinUid" to coinUid,
                ReceiveAddressFragment.POPUP_DESTINATION_ID_KEY to popupDestinationId
            )
        }
    }
}

@Composable
fun NetworkSelectScreen(
    navController: NavController,
    coinUid: String,
    popupDestinationId: Int?,
) {
    val viewModel = viewModel<NetworkSelectViewModel>(factory = NetworkSelectViewModel.Factory(coinUid))

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.Balance_Network),
                    navigationIcon = {
                        HsBackButton(onClick = { navController.popBackStack() })
                    },
                    menuItems = listOf()
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
                        text = stringResource(R.string.Balance_NetworkSelectDescription)
                    )
                    VSpacer(20.dp)
                    CellUniversalLawrenceSection(viewModel.coinWallets) { wallet ->
                        val blockchain = wallet.token.blockchain
                        SectionUniversalItem {
                            NetworkCell(
                                title = blockchain.name,
                                subtitle = blockchain.description,
                                imageUrl = blockchain.type.imageUrl,
                                onClick = {
                                    navController.slideFromRight(
                                        R.id.receiveFragment,
                                        bundleOf(
                                            ReceiveAddressFragment.WALLET_KEY to wallet,
                                            ReceiveAddressFragment.POPUP_DESTINATION_ID_KEY to popupDestinationId,
                                        )
                                    )
                                }
                            )
                        }
                    }
                    VSpacer(32.dp)
                }
            }
        }
    }
}

@Composable
fun NetworkCell(
    title: String,
    subtitle: String,
    imageUrl: String,
    onClick: (() -> Unit)? = null
) {
    RowUniversal(
        onClick = onClick
    ) {
        Image(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(32.dp),
            painter = rememberAsyncImagePainter(
                model = imageUrl,
                error = painterResource(R.drawable.ic_platform_placeholder_32)
            ),
            contentDescription = null,
        )
        Column(modifier = Modifier.weight(1f)) {
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
