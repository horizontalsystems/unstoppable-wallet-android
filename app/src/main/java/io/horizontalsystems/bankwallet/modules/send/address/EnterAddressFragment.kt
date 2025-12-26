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
import androidx.compose.material.Icon
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
import io.horizontalsystems.bankwallet.core.adapters.StellarAssetAdapter
import io.horizontalsystems.bankwallet.core.address.AddressCheckResult
import io.horizontalsystems.bankwallet.core.address.AddressCheckType
import io.horizontalsystems.bankwallet.core.paidAction
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatPremiumTrigger
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.address.AddressParserModule
import io.horizontalsystems.bankwallet.modules.address.AddressParserViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.send.SendFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputAddress
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantError
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_lucian
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_remus
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.subscriptions.core.SecureSend
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

class EnterAddressFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            EnterAddressScreen(navController, input)
        }
    }

    @Parcelize
    data class Input(
        val wallet: Wallet,
        val title: String,
        val sendEntryPointDestId: Int? = null,
        val address: String? = null,
        val amount: BigDecimal? = null,
        val memo: String? = null,
    ) : Parcelable

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterAddressScreen(navController: NavController, input: EnterAddressFragment.Input) {
    val viewModel = viewModel<EnterAddressViewModel>(
        factory = EnterAddressViewModel.Factory(
            token = input.wallet.token,
            address = input.address,
            addressCheckerSkippable = true
        )
    )
    val wallet = input.wallet
    val amount = input.amount
    val paymentAddressViewModel = viewModel<AddressParserViewModel>(
        factory = AddressParserModule.Factory(wallet.token, amount)
    )

    val coroutineScope = rememberCoroutineScope()
    val infoModalBottomSheetState =
        androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var checkTypeInfoBottomSheet by remember { mutableStateOf<AddressCheckType?>(null) }

    val uiState = viewModel.uiState

    HSScaffold(
        title = stringResource(R.string.Send_EnterAddress),
        onBack = navController::popBackStack,
    ) {
        Column(
            modifier = Modifier.windowInsetsPadding(WindowInsets.ime)
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
                    AddressSuggestions(uiState.contacts) {
                        viewModel.onEnterAddress(it)
                    }
                } else if (uiState.addressCheckEnabled || uiState.addressValidationError != null) {
                    AddressCheck(
                        uiState.addressValidationInProgress,
                        uiState.addressValidationError,
                        uiState.checkResults,
                    ) { checkType ->
                        if (uiState.checkResults.any { it.value.checkResult == AddressCheckResult.NotAllowed }) {
                            navController.paidAction(SecureSend) {
                                viewModel.onEnterAddress(uiState.value)
                            }
                            stat(
                                page = StatPage.Send,
                                event = StatEvent.OpenPremium(StatPremiumTrigger.AddressChecker)
                            )
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
                                    sendEntryPointDestId = input.sendEntryPointDestId
                                        ?: R.id.enterAddressFragment,
                                    title = input.title,
                                    address = it,
                                    riskyAddress = uiState.checkResults.any { result -> result.value.checkResult == AddressCheckResult.Detected },
                                    amount = amount,
                                    memo = input.memo,
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
    if (addressValidationInProgress) {
        //show nothing
    } else if (addressValidationError != null) {
        val errorMessage = addressValidationError.getErrorMessage()
        if (errorMessage != null) {
            TextImportantError(
                modifier = Modifier.padding(horizontal = 16.dp),
                icon = R.drawable.ic_attention_20,
                title = stringResource(R.string.SwapSettings_Error_InvalidAddress),
                text = errorMessage
            )
            VSpacer(32.dp)
        }
    }

    if (checkResults.isNotEmpty()) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(
                    0.5.dp,
                    ComposeAppTheme.colors.blade,
                    RoundedCornerShape(16.dp)
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

@Composable
private fun Throwable.getErrorMessage() = when (this) {
    is StellarAssetAdapter.NoTrustlineError -> {
        stringResource(R.string.Error_AssetNotEnabled, code)
    }

    is AddressValidationError -> this.message
    else -> null
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
        tint = ComposeAppTheme.colors.andy,
    )
}

@Composable
fun AddressSuggestions(
    contacts: List<SContact>,
    onClick: (String) -> Unit
) {
    if (contacts.isNotEmpty()) {
        SectionHeaderText(stringResource(R.string.Contacts))
        Column(
            modifier = Modifier
                .padding(bottom = 24.dp)
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(
                    0.5.dp,
                    ComposeAppTheme.colors.blade,
                    RoundedCornerShape(16.dp)
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
                    headline2_leah(contact.name)
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
