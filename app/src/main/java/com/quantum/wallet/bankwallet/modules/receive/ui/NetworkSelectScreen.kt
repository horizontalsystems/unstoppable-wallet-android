package com.quantum.wallet.bankwallet.modules.receive.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.description
import com.quantum.wallet.bankwallet.core.imageUrl
import com.quantum.wallet.bankwallet.entities.Account
import com.quantum.wallet.bankwallet.entities.Wallet
import com.quantum.wallet.bankwallet.modules.receive.viewmodels.NetworkSelectViewModel
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.components.HsDivider
import com.quantum.wallet.bankwallet.ui.compose.components.HsImage
import com.quantum.wallet.bankwallet.ui.compose.components.MenuItem
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellMiddleInfo
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellPrimary
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellRightNavigation
import com.quantum.wallet.bankwallet.uiv3.components.cell.hs
import com.quantum.wallet.bankwallet.uiv3.components.info.TextBlock
import io.horizontalsystems.marketkit.models.FullCoin
import kotlinx.coroutines.launch

@Composable
fun NetworkSelectScreen(
    navController: NavController,
    activeAccount: Account,
    fullCoin: FullCoin,
    closeModule: () -> Unit,
    onSelect: (Wallet) -> Unit
) {
    val viewModel = viewModel<NetworkSelectViewModel>(
        factory = NetworkSelectViewModel.Factory(
            activeAccount,
            fullCoin
        )
    )
    val coroutineScope = rememberCoroutineScope()

    HSScaffold(
        title = stringResource(R.string.Balance_Network),
        onBack = { navController.popBackStack() },
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Close),
                icon = R.drawable.ic_close,
                onClick = closeModule
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeAppTheme.colors.lawrence)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .background(ComposeAppTheme.colors.tyler)
                    .fillMaxWidth()
            ) {
                TextBlock(
                    stringResource(R.string.Balance_NetworkSelectDescription)
                )
                VSpacer(20.dp)
            }
            viewModel.eligibleTokens.forEach { token ->
                val blockchain = token.blockchain
                NetworkCell(
                    title = blockchain.name,
                    subtitle = blockchain.description,
                    imageUrl = blockchain.type.imageUrl,
                    onClick = {
                        coroutineScope.launch {
                            onSelect.invoke(viewModel.getOrCreateWallet(token))
                        }
                    }
                )
                HsDivider()
            }
            VSpacer(32.dp)
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
    CellPrimary(
        left = {
            HsImage(
                url = imageUrl,
                alternativeUrl = null,
                placeholder = R.drawable.ic_platform_placeholder_32,
                modifier = Modifier.size(32.dp),
            )
        },
        middle = {
            CellMiddleInfo(
                title = title.hs,
                subtitle = subtitle.hs,
            )
        },
        right = {
            CellRightNavigation()
        },
        onClick = onClick
    )
}
