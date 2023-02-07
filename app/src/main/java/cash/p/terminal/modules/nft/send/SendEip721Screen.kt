package cash.p.terminal.modules.nft.send

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import cash.p.terminal.R
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.modules.address.AddressParserViewModel
import cash.p.terminal.modules.address.AddressViewModel
import cash.p.terminal.modules.address.HSAddressInput
import cash.p.terminal.modules.send.evm.confirmation.SendEvmConfirmationModule
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.headline1_leah

@Composable
fun SendEip721Screen(
    navController: NavController,
    viewModel: SendEip721ViewModel,
    addressViewModel: AddressViewModel,
    addressParserViewModel: AddressParserViewModel,
    parentNavId: Int,
) {

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.SendNft_Title),
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
            Column(Modifier.padding(it)) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    viewModel.uiState.imageUrl?.let { imageUrl ->
                        Spacer(Modifier.height(12.dp))
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = imageUrl,
                            ),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .heightIn(0.dp, 100.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    headline1_leah(
                        text = viewModel.uiState.name
                    )
                    Spacer(Modifier.height(24.dp))
                    HSAddressInput(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        viewModel = addressViewModel,
                        error = viewModel.uiState.addressError,
                        textPreprocessor = addressParserViewModel,
                    ) { address ->
                        viewModel.onEnterAddress(address)
                    }
                    Spacer(Modifier.height(24.dp))
                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                        title = stringResource(R.string.Button_Next),
                        onClick = {
                            viewModel.getSendData()?.let { sendData ->
                                navController.slideFromRight(
                                    R.id.sendEvmConfirmationFragment,
                                    SendEvmConfirmationModule.prepareParams(sendData, parentNavId)
                                )
                            }
                        },
                        enabled = viewModel.uiState.canBeSend
                    )
                }
            }
        }
    }
}
