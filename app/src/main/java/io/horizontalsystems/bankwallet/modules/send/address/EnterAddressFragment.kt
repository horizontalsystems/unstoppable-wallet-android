package io.horizontalsystems.bankwallet.modules.send.address

import android.os.Parcelable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tonapps.tonkeeper.api.shortAddress
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.address.AddressParserModule
import io.horizontalsystems.bankwallet.modules.address.AddressParserViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.send.SendFragment
import io.horizontalsystems.bankwallet.modules.sendtokenselect.PrefilledData
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputAddress
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantError
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_lucian
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_remus
import kotlinx.parcelize.Parcelize

class EnterAddressFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        EnterAddressScreen(navController, navController.requireInput())
    }

    @Parcelize
    data class Input(
        val wallet: Wallet,
        val title: String,
        val sendEntryPointDestId: Int = 0,
        val predefinedAddress: String? = null,
        val prefilledAddressData: PrefilledData? = null
    ) : Parcelable

}

@Composable
fun EnterAddressScreen(navController: NavController, input: EnterAddressFragment.Input) {
    val viewModel = viewModel<EnterAddressViewModel>(factory = EnterAddressViewModel.Factory(input.wallet))
    val prefilledData = input.prefilledAddressData
    val wallet = input.wallet
    val paymentAddressViewModel = viewModel<AddressParserViewModel>(
        factory = AddressParserModule.Factory(wallet.token, prefilledData?.amount)
    )

    val uiState = viewModel.uiState
    val addressError = uiState.addressError
    val addressErrorMessage: ErrorMessage? = ErrorMessage(
        title = stringResource(R.string.Send_Address_ErrorMessage_PhishingDetected),
        description = stringResource(R.string.Send_Address_ErrorMessage_PhishingDetected_Description)
    )
    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
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
                    textPreprocessor = paymentAddressViewModel,
                    navController = navController,
                    chooseContactEnable = false,
                    blockchainType = null,
                ) {
                    viewModel.onEnterAddress(it)
                }

                if (uiState.value.isBlank()) {
                    AddressSuggestions(uiState.recentAddress, uiState.contacts) {
                        viewModel.onEnterAddress(it)
                    }
                } else {
                    AddressCheck(false, addressErrorMessage)
                }
            }
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = stringResource(R.string.Button_Next),
                    onClick = {
                        navController.slideFromRight(
                            R.id.sendXFragment,
                            SendFragment.Input(
                                wallet = wallet,
                                sendEntryPointDestId = R.id.sendTokenSelectFragment,
                                title = input.title,
                                predefinedAddress = input.predefinedAddress,
                                prefilledAddressData = prefilledData,
                            )
                        )
                    },
                    enabled = uiState.canBeSendToAddress
                )
            }
        }
    }
}

@Composable
fun AddressCheck(
    locked: Boolean,
    addressErrorMessage: ErrorMessage?
) {
    Column(
        modifier = Modifier
            .padding(top = 16.dp)
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                0.5.dp,
                ComposeAppTheme.colors.steel20,
                RoundedCornerShape(12.dp)
            )
    ) {
        AddressCheckCell(
            title = stringResource(R.string.Send_Address_AddressCheck),
            inProgress = false,
            validationResult = AddressCheckResult.Correct
        )
        CheckCell(
            title = stringResource(R.string.Send_Address_PhishingCheck),
            locked = locked,
            inProgress = false,
            validationResult = AddressCheckResult.Detected
        )
        CheckCell(
            title = stringResource(R.string.Send_Address_BlacklistCheck),
            locked = locked,
            inProgress = false,
            validationResult = AddressCheckResult.Clear
        )
    }
    addressErrorMessage?.let { errorMessage ->
        VSpacer(16.dp)
        TextImportantError(
            modifier = Modifier.padding(horizontal = 16.dp),
            icon = R.drawable.ic_attention_20,
            title = errorMessage.title,
            text = errorMessage.description
        )
        VSpacer(32.dp)
    }
}

@Composable
private fun CheckCell(
    title: String,
    locked: Boolean,
    inProgress: Boolean,
    validationResult: AddressCheckResult?
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        subhead2_grey(text = title)
        Icon(
            painter = painterResource(R.drawable.prem_crown_yellow_16),
            contentDescription = null,
            tint = ComposeAppTheme.colors.jacob,
            modifier = Modifier
                .padding(start = 2.dp)
                .size(20.dp)
        )
        Spacer(Modifier.weight(1f))
        if (locked) {
            CheckLocked()
        } else {
            CheckValue(inProgress, validationResult)
        }
    }
}

@Composable
private fun AddressCheckCell(
    title: String,
    inProgress: Boolean,
    validationResult: AddressCheckResult?
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        subhead2_grey(
            text = title,
            modifier = Modifier.weight(1f)
        )
        CheckValue(inProgress, validationResult)
    }
}

@Composable
fun CheckValue(
    inProgress: Boolean,
    validationResult: AddressCheckResult?
) {
    if (inProgress) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = ComposeAppTheme.colors.grey,
            strokeWidth = 2.dp
        )
    } else {
        when (validationResult) {
            AddressCheckResult.Correct,
            AddressCheckResult.Clear -> subhead2_remus(stringResource(validationResult.stringResId))

            AddressCheckResult.Incorrect,
            AddressCheckResult.Detected -> subhead2_lucian(stringResource(validationResult.stringResId))

            else -> {}
        }
    }
}

@Composable
fun CheckLocked() {
    Icon(
        painter = painterResource(R.drawable.ic_lock_20),
        contentDescription = null,
        tint = ComposeAppTheme.colors.grey50,
    )
}

@Composable
fun AddressSuggestions(recent: String?, contacts: List<SContact>, onClick: (String) -> Unit) {
    recent?.let { address ->
        SectionHeaderText(stringResource(R.string.Send_Address_Recent))
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(
                    0.5.dp,
                    ComposeAppTheme.colors.steel20,
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
                    ComposeAppTheme.colors.steel20,
                    RoundedCornerShape(12.dp)
                )
        ) {
            contacts.forEachIndexed { index, contact ->
                if (index != 0) {
                    Divider(
                        modifier = Modifier.fillMaxWidth(),
                        color = ComposeAppTheme.colors.steel20,
                        thickness = 0.5.dp
                    )
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

enum class AddressCheckResult(val stringResId: Int) {
    Correct(R.string.Send_Address_Error_Correct),
    Incorrect(R.string.Send_Address_Error_Incorrect),
    Clear(R.string.Send_Address_Error_Clear),
    Detected(R.string.Send_Address_Error_Detected)
}

data class ErrorMessage(
    val title: String,
    val description: String
)
