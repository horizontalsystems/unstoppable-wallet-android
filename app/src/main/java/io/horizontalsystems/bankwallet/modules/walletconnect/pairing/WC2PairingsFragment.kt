package io.horizontalsystems.bankwallet.modules.walletconnect.pairing

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.body_lucian
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.core.findNavController

class WC2PairingsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        WCPairingsScreen(findNavController())
    }

}

@Composable
fun WCPairingsScreen(navController: NavController) {
    val viewModel = viewModel<WCPairingsViewModel>(factory = WCPairingsViewModel.Factory())
    val uiState = viewModel.uiState

    LaunchedEffect(uiState.closeScreen) {
        if (uiState.closeScreen) {
            navController.popBackStack()
        }
    }

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = stringResource(R.string.WalletConnect_PairedDApps),
                    navigationIcon = {
                        HsBackButton(onClick = { navController.popBackStack() })
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
                CellUniversalLawrenceSection(pairings) { pairing ->
                    Pairing(pairing = pairing) {
                        viewModel.delete(pairing)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                CellUniversalLawrenceSection(
                    listOf {
                        RowUniversal(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            onClick = {
                                ConfirmDeleteAllPairingsDialog.onConfirm(navController) {
                                    viewModel.deleteAll()
                                }
                                navController.slideFromBottom(R.id.confirmDeleteAllPairingsDialog)
                            }
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
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun Pairing(pairing: PairingViewItem, onDelete: () -> Unit) {
    RowUniversal(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    ) {
        Image(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp)),
            painter = rememberAsyncImagePainter(
                model = pairing.icon,
                error = painterResource(R.drawable.ic_platform_placeholder_24)
            ),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            val name = if (pairing.name.isNullOrBlank()) {
                stringResource(id = R.string.WalletConnect_Unnamed)
            } else {
                pairing.name
            }

            body_leah(
                text = name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            subhead2_grey(
                text = pairing.url ?: "---",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        ButtonSecondaryCircle(
            modifier = Modifier.padding(start = 16.dp),
            icon = R.drawable.ic_delete_20,
            tint = ComposeAppTheme.colors.lucian,
            onClick = onDelete
        )
    }
}
