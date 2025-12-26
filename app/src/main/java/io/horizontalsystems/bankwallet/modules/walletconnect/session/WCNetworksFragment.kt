package io.horizontalsystems.bankwallet.modules.walletconnect.session

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
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.title
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.BoxBordered
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellLeftImage
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.ImageType
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.core.findNavController
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
