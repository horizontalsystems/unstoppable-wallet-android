package io.horizontalsystems.bankwallet.modules.enteraddress

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import com.tonapps.tonkeeper.api.shortAddress
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.adapters.StellarAssetAdapter
import io.horizontalsystems.bankwallet.core.addFromBottom
import io.horizontalsystems.bankwallet.core.address.AddressCheckResult
import io.horizontalsystems.bankwallet.core.address.AddressCheckType
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.address.AddressParserModule
import io.horizontalsystems.bankwallet.modules.address.AddressParserViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.premium.DefenseSystemFeatureDialog
import io.horizontalsystems.bankwallet.modules.premium.PremiumFeature
import io.horizontalsystems.bankwallet.modules.settings.security.securesend.SecureSendConfigDialog
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputAddress
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.uiv3.components.AlertCard
import io.horizontalsystems.bankwallet.uiv3.components.AlertFormat
import io.horizontalsystems.bankwallet.uiv3.components.AlertType
import io.horizontalsystems.bankwallet.uiv3.components.BoxBordered
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.section.SectionHeader
import io.horizontalsystems.marketkit.models.Token

@Composable
fun EnterAddressScreen(
    navController: NavBackStack<HSScreen>,
    token: Token,
    title: String,
    buttonTitle: String,
    allowNull: Boolean,
    initialAddress: String?,
    onResult: (address: Address?, risky: Boolean) -> Unit
) {
    val viewModel = viewModel<EnterAddressViewModel>(
        factory = EnterAddressViewModel.Factory(
            token = token,
            address = initialAddress,
            allowNull = allowNull,
        )
    )
    val paymentAddressViewModel = viewModel<AddressParserViewModel>(
        factory = AddressParserModule.Factory(token = token, prefilledAmount = null)
    )

    val uiState = viewModel.uiState

    HSScaffold(
        title = title,
        onBack = navController::removeLastOrNull,
    ) {
        Column(
            modifier = Modifier.windowInsetsPadding(WindowInsets.ime)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
            ) {
                FormsInputAddress(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    value = uiState.value,
                    hint = stringResource(id = R.string.Send_Hint_Address),
                    state = uiState.inputState,
                    showStateIcon = false,
                    textPreprocessor = paymentAddressViewModel,
                ) {
                    viewModel.onEnterAddress(it)
                }

                if (uiState.value.isBlank()) {
                    AddressSuggestions(
                        uiState.recentAddress,
                        uiState.recentContact,
                        uiState.contacts,
                    ) {
                        viewModel.onEnterAddress(it)
                    }
                } else {
                    AddressCheck(
                        uiState.addressValidationInProgress,
                        uiState.addressValidationError,
                        uiState.checkResults,
                    ) {
                        if (uiState.hasPremium){
                            navController.addFromBottom(SecureSendConfigDialog())
                        } else {
                            navController.slideFromBottom(
                                DefenseSystemFeatureDialog(),
                                DefenseSystemFeatureDialog.Input(PremiumFeature.SecureSendFeature)
                            )
                        }
                    }
                }

                VSpacer(32.dp)
            }
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = if (uiState.addressValidationError != null)
                        stringResource(R.string.Send_Address_Error_InvalidAddress)
                    else
                        buttonTitle,
                    onClick = {
                        onResult.invoke(uiState.address, uiState.risky)
                    },
                    enabled = uiState.canBeSendToAddress
                )
            }
        }
    }
}


@Composable
fun AddressSuggestions(
    recentAddress: String?,
    recentContact: SContact?,
    contacts: List<SContact>,
    onClick: (String) -> Unit
) {
    if (recentContact != null) {
        AddressCardContainer(onClick = { onClick(recentContact.address) }) {
            ContactItem(recentContact)
        }
    } else recentAddress?.let { address ->
        SectionHeaderText(stringResource(R.string.Send_Address_Recent))
        AddressCardContainer(onClick = { onClick(address) }) {
            body_leah(address)
        }
    }
    if (contacts.isNotEmpty()) {
        SectionHeaderText(stringResource(R.string.Contacts))
        AddressCardContainer {
            contacts.forEachIndexed { index, contact ->
                if (index != 0) {
                    HsDivider(modifier = Modifier.fillMaxWidth())
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick(contact.address) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    ContactItem(contact)
                }
            }
        }
    }
}

@Composable
private fun AddressCardContainer(
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var modifier: Modifier = Modifier
        .padding(horizontal = 16.dp)
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .border(0.5.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(16.dp))
    if (onClick != null) {
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    }
    Column(modifier = modifier) {
        content()
    }
}

@Composable
fun AddressCheck(
    addressValidationInProgress: Boolean,
    addressValidationError: Throwable?,
    checkResults: Map<AddressCheckType, AddressCheckData>,
    onClick: (type: AddressCheckType) -> Unit
) {
    if (addressValidationInProgress) {
        //show nothing
    } else if (addressValidationError != null) {
        AlertCard(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            format = AlertFormat.Structured,
            type = AlertType.Critical,
            titleCustom = stringResource(R.string.SwapSettings_Error_InvalidAddress),
            text = stringResource(R.string.Send_Address_Error_InvalidAddress_Description),
        )
        VSpacer(32.dp)
    }

    if (checkResults.isNotEmpty()) {
        SectionHeader(
            modifier = Modifier.padding(horizontal = 16.dp),
            title = stringResource(R.string.Premium_UpgradeFeature_SecureSend),
            icon = R.drawable.defense_gradient_filled_24
        )
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
            checkResults.entries.forEachIndexed { index, (addressCheckType, checkData) ->
                CheckCell(
                    title = stringResource(addressCheckType.title),
                    checkType = addressCheckType,
                    inProgress = checkData.inProgress,
                    disabled = checkData.disabled,
                    showDivider = index != 0,
                    checkResult = checkData.checkResult,
                    onClick
                )
            }
        }
    }

    checkResults.forEach { (addressCheckType, addressCheckData) ->
        if (addressCheckData.checkResult == AddressCheckResult.Detected) {
            AlertCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                format = AlertFormat.Structured,
                type = AlertType.Critical,
                titleCustom = stringResource(addressCheckType.detectedErrorTitle),
                text = stringResource(addressCheckType.detectedErrorDescription),
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

    else -> this.message
}

@Composable
private fun CheckCell(
    title: String,
    checkType: AddressCheckType,
    inProgress: Boolean,
    disabled: Boolean,
    showDivider: Boolean,
    checkResult: AddressCheckResult,
    onClick: (type: AddressCheckType) -> Unit
) {
    BoxBordered(
        top = showDivider
    ) {
        CellPrimary(
            middle = {
                CellMiddleInfo(
                    subtitle = title.hs
                )
            },
            right = {
                when {
                    checkResult == AddressCheckResult.NotAllowed -> CheckLocked()
                    disabled -> CheckDisabled()
                    else -> CheckValue(inProgress, checkResult)
                }
            },
            onClick = { onClick(checkType) }
        )
    }
}

@Composable
fun CheckDisabled() {
    CellRightInfo(
        titleSubheadSb = stringResource(R.string.SecureSend_Config_Disabled).hs(color = ComposeAppTheme.colors.leah)
    )
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
        val color = when (checkResult) {
            AddressCheckResult.Clear -> ComposeAppTheme.colors.remus
            AddressCheckResult.Detected -> ComposeAppTheme.colors.lucian
            else -> ComposeAppTheme.colors.grey
        }
        val text = when (checkResult) {
            AddressCheckResult.Clear ->
                stringResource(checkResult.title)
            AddressCheckResult.Detected ->
                stringResource(checkResult.title)
            else ->
                stringResource(R.string.NotAvailable)
        }

        CellRightInfo(
            titleSubheadSb = text.hs(color = color)
        )
    }
}

@Composable
fun CheckLocked() {
    Icon(
        painter = painterResource(R.drawable.lock_filled_24),
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = ComposeAppTheme.colors.grey,
    )
}

@Composable
private fun ContactItem(contact: SContact) {
    headline2_leah(contact.name)
    subhead2_grey(contact.address.shortAddress)
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
