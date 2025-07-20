package cash.p.terminal.modules.send.address

import android.os.Parcelable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.modules.address.AddressParserModule
import cash.p.terminal.modules.address.AddressParserViewModel
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.modules.send.SendFragment
import cash.p.terminal.modules.sendtokenselect.PrefilledData
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.ui.compose.components.FormsInputAddress
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.HsDivider
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.subhead1_grey
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.requireInput
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.Wallet
import com.tonapps.tonkeeper.api.shortAddress
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

class EnterAddressFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        EnterAddressScreen(navController, navController.requireInput())
    }

    @Parcelize
    data class Input(
        val wallet: Wallet,
        val title: String,
        val sendEntryPointDestId: Int? = null,
        val address: String? = null,
        val amount: BigDecimal? = null,
    ) : Parcelable

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterAddressScreen(navController: NavController, input: EnterAddressFragment.Input) {
    val viewModel = viewModel<EnterAddressViewModel>(
        factory = EnterAddressViewModel.Factory(
            token = input.wallet.token,
            address = input.address
        )
    )
    val wallet = input.wallet
    var amount by remember { mutableStateOf<BigDecimal?>(null) }
    val paymentAddressViewModel = viewModel<AddressParserViewModel>(
        factory = AddressParserModule.Factory(wallet.token,
            PrefilledData(input.address.orEmpty(), amount)
        )
    )

    val uiState = viewModel.uiState
    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.Send_EnterAddress),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
            )
        },
    ) { innerPaddings ->
        Column(
            modifier = Modifier
                .padding(innerPaddings)
                .windowInsetsPadding(WindowInsets.ime)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                FormsInputAddress(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    value = uiState.value,
                    hint = stringResource(id = R.string.Send_Hint_Address),
                    state = uiState.inputState,
                    showStateIcon = false,
                    textPreprocessor = paymentAddressViewModel,
                    navController = navController,
                    chooseContactEnable = false,
                    blockchainType = null,
                    onValueChange = viewModel::onEnterAddress,
                    onAmountChange = {
                        amount = it
                    }
                )

                if (uiState.value.isBlank()) {
                    AddressSuggestions(
                        uiState.recentAddress,
                        uiState.recentContact,
                        uiState.contacts
                    ) {
                        viewModel.onEnterAddress(it)
                    }
                }
            }
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = if (uiState.addressValidationError != null)
                        stringResource(R.string.Send_Address_Error_InvalidAddress)
                    else
                        stringResource(R.string.Button_Next),
                    onClick = {
                        uiState.address?.let {
                            navController.slideFromRight(
                                R.id.sendXFragment,
                                SendFragment.Input(
                                    wallet = wallet,
                                    sendEntryPointDestId = input.sendEntryPointDestId
                                        ?: R.id.enterAddressFragment,
                                    title = input.title,
                                    address = it,
                                    amount = amount
                                )
                            )
                        }
                    },
                    enabled = uiState.canBeSendToAddress
                )
            }
        }
    }
}

@Composable
fun AddressSuggestions(
    recent: String?,
    recentContact: SContact?,
    contacts: List<SContact>,
    onClick: (String) -> Unit
) {
    if (recentContact != null) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(
                    0.5.dp,
                    ComposeAppTheme.colors.blade,
                    RoundedCornerShape(12.dp)
                )
                .clickable {
                    onClick.invoke(recentContact.address)
                }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            body_leah(recentContact.name)
            subhead2_grey(recentContact.address.shortAddress)
        }
    } else recent?.let { address ->
        SectionHeaderText(stringResource(R.string.Send_Address_Recent))
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(
                    0.5.dp,
                    ComposeAppTheme.colors.blade,
                    RoundedCornerShape(12.dp)
                )
                .clickable {
                    onClick.invoke(address)
                }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            body_leah(address)
        }
    }
    if (contacts.isNotEmpty()) {
        SectionHeaderText(stringResource(R.string.Contacts))
        Column(
            modifier = Modifier
                .padding(bottom = 24.dp)
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(
                    0.5.dp,
                    ComposeAppTheme.colors.blade,
                    RoundedCornerShape(12.dp)
                )
        ) {
            contacts.forEachIndexed { index, contact ->
                if (index != 0) {
                    HsDivider(modifier = Modifier.fillMaxWidth())
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onClick.invoke(contact.address)
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    body_leah(contact.name)
                    subhead2_grey(contact.address.shortAddress)
                }
            }
        }
    }
}

@Composable
fun SectionHeaderText(title: String) {
    Box(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        subhead1_grey(title)
    }
}

data class SContact(
    val name: String,
    val address: String
)
