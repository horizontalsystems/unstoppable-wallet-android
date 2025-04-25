package cash.p.terminal.modules.nft.send

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import cash.p.terminal.R
import cash.p.terminal.core.slideFromRightForResult
import cash.p.terminal.modules.address.AddressParserViewModel
import cash.p.terminal.modules.address.AddressViewModel
import cash.p.terminal.modules.address.HSAddressInput
import cash.p.terminal.modules.send.evm.confirmation.SendEvmConfirmationFragment
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.headline1_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun SendEip721Screen(
    navController: NavController,
    viewModel: SendEip721ViewModel,
    addressViewModel: AddressViewModel,
    addressParserViewModel: AddressParserViewModel,
) {

    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.SendNft_Title),
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
                    inputState = addressViewModel.inputState.collectAsStateWithLifecycle().value,
                    address = addressViewModel.address.collectAsStateWithLifecycle().value,
                    value = addressViewModel.value.collectAsStateWithLifecycle().value,
                    error = viewModel.uiState.addressError,
                    textPreprocessor = addressParserViewModel,
                    navController = navController,
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
                            navController.slideFromRightForResult<SendEvmConfirmationFragment.Result>(
                                R.id.sendEvmConfirmationFragment,
                                SendEvmConfirmationFragment.Input(
                                    sendData = sendData,
                                    blockchainType = viewModel.getBlockchainType()
                                )
                            ) {
                                if (it.success) {
                                    navController.popBackStack()
                                }
                            }
                        }
                    },
                    enabled = viewModel.uiState.canBeSend
                )
            }
        }
    }
}
