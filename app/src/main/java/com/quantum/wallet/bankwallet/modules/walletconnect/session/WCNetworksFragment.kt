package com.quantum.wallet.bankwallet.modules.walletconnect.session

import android.os.Bundle
import android.os.Parcelable
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.getInput
import com.quantum.wallet.bankwallet.core.imageUrl
import com.quantum.wallet.bankwallet.core.title
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.components.MenuItem
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.uiv3.components.BoxBordered
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellLeftImage
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellMiddleInfo
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellPrimary
import com.quantum.wallet.bankwallet.uiv3.components.cell.ImageType
import com.quantum.wallet.bankwallet.uiv3.components.cell.hs
import com.quantum.wallet.core.findNavController
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize

class WCNetworksFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<Input>()
        if (input == null) {
            navController.popBackStack()
            return
        }
        NetworksScreen(
            input.blockchainTypes,
            navController = navController,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().popBackStack()
                }
            })
    }

    @Parcelize
    data class Input(val blockchainTypes: List<BlockchainType>) : Parcelable

}

@Composable
private fun NetworksScreen(
    blockchains: List<BlockchainType>,
    navController: NavController,
) {
    HSScaffold(
        title = stringResource(R.string.WalletConnect_Networks),
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Close),
                icon = R.drawable.ic_close,
                onClick = navController::popBackStack
            )
        ),
    ) {
        Column(
            modifier = Modifier.background(ComposeAppTheme.colors.lawrence)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                blockchains.forEach { item ->
                    BoxBordered(bottom = true) {
                        CellPrimary(
                            left = {
                                CellLeftImage(
                                    type = ImageType.Rectangle,
                                    size = 32,
                                    painter = rememberAsyncImagePainter(
                                        model = item.imageUrl,
                                        error = painterResource(R.drawable.ic_platform_placeholder_32)
                                    ),
                                )
                            },
                            middle = {
                                CellMiddleInfo(
                                    title = item.title.hs,
                                )
                            },
                        )
                    }
                }

                VSpacer(height = 32.dp)
            }
        }
    }
}
