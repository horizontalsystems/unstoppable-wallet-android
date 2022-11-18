package io.horizontalsystems.bankwallet.modules.walletconnect.pairing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController

class WC2PairingsFragment : BaseFragment() {

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
                WCPairingsScreen(findNavController())
            }
        }
    }
}

@Composable
fun WCPairingsScreen(navController: NavController) {
    val viewModel = viewModel<WCPairingsViewModel>(factory = WCPairingsViewModel.Factory())
    val uiState = viewModel.uiState

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.WalletConnect_PairedDApps),
                    navigationIcon = {
                        HsIconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back),
                                contentDescription = "back button",
                                tint = ComposeAppTheme.colors.jacob
                            )
                        }
                    },
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                val pairings = uiState.pairings
                CellMultilineLawrenceSection(pairings) { pairing ->
                    Pairing(pairing = pairing) {
                        viewModel.delete(pairing)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                CellSingleLineLawrenceSection {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxSize()
                            .clickable {
                                viewModel.deleteAll()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete_20),
                            contentDescription = null,
                            tint = ComposeAppTheme.colors.lucian
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        body_lucian(text = stringResource(id = R.string.WalletConnect_Pairings_DeleteAll))
                    }

                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun Pairing(pairing: PairingViewItem, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp)),
            painter = rememberAsyncImagePainter(
                model = pairing.icon,
                error = painterResource(R.drawable.ic_platform_placeholder_24)
            ),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            val name = if (pairing.name.isNullOrBlank()) {
                stringResource(id = R.string.WalletConnect_DAppUnknown)
            } else {
                pairing.name
            }

            body_leah(text = name)
            Spacer(modifier = Modifier.height(1.dp))
            subhead2_grey(text = pairing.url ?: "")
        }
        Spacer(modifier = Modifier
            .defaultMinSize(minWidth = 16.dp)
            .weight(1f))
        ButtonSecondaryCircle(
            icon = R.drawable.ic_delete_20,
            tint = ComposeAppTheme.colors.lucian,
            onClick = onDelete
        )
    }
}
