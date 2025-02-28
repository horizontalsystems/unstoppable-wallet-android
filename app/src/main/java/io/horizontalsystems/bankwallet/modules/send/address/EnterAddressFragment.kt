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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import io.horizontalsystems.bankwallet.core.address.AddressCheckResult
import io.horizontalsystems.bankwallet.core.address.AddressCheckType
import io.horizontalsystems.bankwallet.core.paidAction
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.address.AddressParserModule
import io.horizontalsystems.bankwallet.modules.address.AddressParserViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.send.SendFragment
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
import io.horizontalsystems.subscriptions.core.AddressBlacklist
import kotlinx.coroutines.launch
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
            wallet = input.wallet,
            address = input.address,
            amount = input.amount
        )
    )
    val wallet = input.wallet
    val paymentAddressViewModel = viewModel<AddressParserViewModel>(
        factory = AddressParserModule.Factory(wallet.token, input.amount)
    )

    val coroutineScope = rememberCoroutineScope()
    val infoModalBottomSheetState =
        androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var checkTypeInfoBottomSheet by remember { mutableStateOf<AddressCheckType?>(null) }

    val uiState = viewModel.uiState
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
                    showStateIcon = false,
                    textPreprocessor = paymentAddressViewModel,
                    navController = navController,
                    chooseContactEnable = false,
                    blockchainType = null,
                ) {
                    viewModel.onEnterAddress(it)
                }

                if (uiState.value.isBlank()) {
                    AddressSuggestions(
                        uiState.recentAddress,
                        uiState.recentContact,
                        uiState.contacts
                    ) {
                        viewModel.onEnterAddress(it)
                    }
                } else {
                    AddressCheck(
                        uiState.addressValidationInProgress,
                        uiState.addressValidationError,
                        uiState.checkResults,
                    ) { checkType ->
                        if(uiState.checkResults.any { it.value.checkResult == AddressCheckResult.NotAllowed }) {
                            navController.paidAction(AddressBlacklist) {
                                viewModel.onEnterAddress(uiState.value)
                            }
                        } else {
                            checkTypeInfoBottomSheet = checkType
                            coroutineScope.launch {
                                infoModalBottomSheetState.show()
                            }
                        }
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
                                    sendEntryPointDestId = input.sendEntryPointDestId ?: R.id.enterAddressFragment,
                                    title = input.title,
                                    address = it,
                                    riskyAddress = uiState.checkResults.any { result -> result.value.checkResult == AddressCheckResult.Detected },
                                    amount = uiState.amount
                                )
                            )
                        }
                    },
                    enabled = uiState.canBeSendToAddress
                )
            }
        }
    }

    checkTypeInfoBottomSheet?.let { checkType ->
        AddressEnterInfoBottomSheet(
            checkType = checkType,
            bottomSheetState = infoModalBottomSheetState,
            hideBottomSheet = {
                coroutineScope.launch {
                    infoModalBottomSheetState.hide()
                }
                checkTypeInfoBottomSheet = null
            }
        )
    }
}

@Composable
fun AddressCheck(
    addressValidationInProgress: Boolean,
    addressValidationError: Throwable?,
    checkResults: Map<AddressCheckType, AddressCheckData>,
    onClick: (type: AddressCheckType) -> Unit
) {
    VSpacer(16.dp)
    if (addressValidationError == null) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(
                    0.5.dp,
                    ComposeAppTheme.colors.steel20,
                    RoundedCornerShape(12.dp)
                )
        ) {
            checkResults.forEach { (addressCheckType, checkData) ->
                CheckCell(
                    title = stringResource(addressCheckType.title),
                    checkType = addressCheckType,
                    inProgress = checkData.inProgress,
                    checkResult = checkData.checkResult,
                    onClick
                )
            }
        }
    }

    Errors(addressValidationError, checkResults)
}

@Composable
private fun Errors(
    addressValidationError: Throwable?,
    checkResults: Map<AddressCheckType, AddressCheckData>,
) {
    if (addressValidationError != null) {
        TextImportantError(
            modifier = Modifier.padding(horizontal = 16.dp),
            icon = R.drawable.ic_attention_20,
            title = stringResource(R.string.SwapSettings_Error_InvalidAddress),
            text = addressValidationError.message
                ?: stringResource(R.string.SwapSettings_Error_InvalidAddress)
        )
        VSpacer(32.dp)
    } else {
        checkResults.forEach { (addressCheckType, addressCheckData) ->
            if (addressCheckData.checkResult == AddressCheckResult.Detected) {
                TextImportantError(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    icon = R.drawable.ic_attention_20,
                    title = stringResource(addressCheckType.detectedErrorTitle),
                    text = stringResource(addressCheckType.detectedErrorDescription)
                )
                VSpacer(16.dp)
            }
        }

        if (checkResults.any { it.value.checkResult == AddressCheckResult.Detected }) {
            VSpacer(32.dp)
        }
    }
}

@Composable
private fun CheckCell(
    title: String,
    checkType: AddressCheckType,
    inProgress: Boolean,
    checkResult: AddressCheckResult,
    onClick: (type: AddressCheckType) -> Unit
) {
    Row(
        modifier = Modifier
            .clickable { onClick(checkType) }
            .height(40.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_star_filled_20),
            contentDescription = null,
            tint = ComposeAppTheme.colors.jacob,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(20.dp)
        )
        subhead2_grey(text = title)
        Spacer(Modifier.weight(1f))
        if (checkResult == AddressCheckResult.NotAllowed) {
            CheckLocked()
        } else {
            CheckValue(inProgress, checkResult)
        }
    }
}

@Composable
fun CheckValue(
    inProgress: Boolean,
    checkResult: AddressCheckResult,
) {
    if (inProgress) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = ComposeAppTheme.colors.grey,
            strokeWidth = 2.dp
        )
    } else {
        when (checkResult) {
            AddressCheckResult.Clear -> {
                subhead2_remus(stringResource(checkResult.title))
            }

            AddressCheckResult.Detected -> {
                subhead2_lucian(stringResource(checkResult.title))
            }

            else -> {
                subhead2_grey(stringResource(R.string.NotAvailable))
            }
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
                    ComposeAppTheme.colors.steel20,
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
