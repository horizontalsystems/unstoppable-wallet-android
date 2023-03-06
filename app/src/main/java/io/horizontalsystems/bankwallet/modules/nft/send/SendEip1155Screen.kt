package io.horizontalsystems.bankwallet.modules.nft.send

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.address.AddressParserViewModel
import io.horizontalsystems.bankwallet.modules.address.AddressViewModel
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.modules.send.evm.confirmation.SendEvmConfirmationModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*

@Composable
fun SendEip1155Screen(
    navController: NavController,
    viewModel: SendEip1155ViewModel,
    addressViewModel: AddressViewModel,
    addressParserViewModel: AddressParserViewModel,
    nftSendFragment: Int,
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
                    Spacer(Modifier.height(12.dp))
                    AvailableNftBalance(viewModel.availableNftBalance)
                    Spacer(Modifier.height(12.dp))
                    ItemCountInput(
                        state = viewModel.uiState.amountState,
                        onValueChange = { amount ->
                            viewModel.onAmountChange(amount)
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    HSAddressInput(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        viewModel = addressViewModel,
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
                                navController.slideFromRight(
                                    R.id.sendEvmConfirmationFragment,
                                    SendEvmConfirmationModule.prepareParams(
                                        sendData,
                                        nftSendFragment
                                    )
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

@Composable
private fun AvailableNftBalance(availableBalance: String) {
    AdditionalDataCell2 {
        subhead2_grey(text = stringResource(R.string.Send_DialogAvailableBalance))

        Spacer(modifier = Modifier.weight(1f))

        subhead2_leah(text = availableBalance)
    }
}

@Composable
private fun ItemCountInput(
    initial: Int = 1,
    state: DataState<Any>? = null,
    onValueChange: (Int) -> Unit,
) {
    val borderColor = when (state) {
        is DataState.Error -> ComposeAppTheme.colors.red50
        else -> ComposeAppTheme.colors.steel20
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(ComposeAppTheme.colors.lawrence),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue(initial.toString()))
        }

        BasicTextField(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .weight(1f),
            value = textState,
            onValueChange = { textFieldValue ->
                val filtered = textFieldValue.text.filter { it.isDigit() }
                textState = textState.copy(text = filtered, selection = TextRange(filtered.length))
                onValueChange.invoke(filtered.toIntOrNull() ?: 0)
            },
            singleLine = true,
            cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        ButtonSecondaryCircle(
            modifier = Modifier.padding(end = 16.dp),
            icon = R.drawable.ic_minus_20,
            onClick = {
                var number = textState.text.toIntOrNull() ?: 1
                if (number > 1) {
                    number -= 1
                }
                val stringNumber = number.toString()
                textState =
                    textState.copy(text = stringNumber, selection = TextRange(stringNumber.length))
                onValueChange.invoke(number)
            }
        )

        ButtonSecondaryCircle(
            modifier = Modifier.padding(end = 16.dp),
            icon = R.drawable.ic_plus_20,
            onClick = {
                var number = textState.text.toIntOrNull() ?: 0
                number += 1
                val stringNumber = number.toString()
                textState =
                    textState.copy(text = stringNumber, selection = TextRange(stringNumber.length))
                onValueChange.invoke(number)
            }
        )
    }

    if (state is DataState.Error) {
        caption_lucian(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp),
            text = stringResource(R.string.SendNft_Error_NotEnoughtBalance),
        )
    }
}
