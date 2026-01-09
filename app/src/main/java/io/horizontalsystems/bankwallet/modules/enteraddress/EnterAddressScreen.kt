package io.horizontalsystems.bankwallet.modules.enteraddress

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tonapps.tonkeeper.api.shortAddress
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.address.AddressCheckResult
import io.horizontalsystems.bankwallet.core.address.AddressCheckType
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.address.AddressParserModule
import io.horizontalsystems.bankwallet.modules.address.AddressParserViewModel
import io.horizontalsystems.bankwallet.modules.premium.DefenseSystemFeatureDialog
import io.horizontalsystems.bankwallet.modules.premium.PremiumFeature
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputAddress
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.uiv3.components.message.DefenseAlertLevel
import io.horizontalsystems.bankwallet.uiv3.components.message.DefenseSystemMessage
import io.horizontalsystems.marketkit.models.Token

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterAddress(
    modifier: Modifier = Modifier,
    navController: NavController,
    token: Token,
    initialAddress: String? = null,
    onValueChange: ((Address?) -> Unit)? = null,
    onValidationError: ((Throwable?) -> Unit)? = null,
    onRiskyAddress: ((Boolean) -> Unit)? = null,
    onValidationInProgress: ((Boolean) -> Unit)? = null,
) {
    val viewModel = viewModel<EnterAddressViewModel>(
        factory = EnterAddressViewModel.Factory(
            token = token,
            address = initialAddress,
        )
    )

    val paymentAddressViewModel = viewModel<AddressParserViewModel>(
        factory = AddressParserModule.Factory(token, null)
    )

    val uiState = viewModel.uiState

    LaunchedEffect(uiState.address) {
        onValueChange?.invoke(uiState.address)
    }

    LaunchedEffect(uiState.addressValidationError) {
        onValidationError?.invoke(uiState.addressValidationError)
    }

    LaunchedEffect(uiState.checkResults) {
        val riskyAddress = uiState.checkResults.any { result -> result.value.checkResult == AddressCheckResult.Detected }
        onRiskyAddress?.invoke(riskyAddress)
    }

    LaunchedEffect(uiState.addressValidationInProgress) {
        onValidationInProgress?.invoke(uiState.addressValidationInProgress)
    }

    FormsInputAddress(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        value = uiState.value,
        hint = stringResource(id = R.string.Send_Hint_Address),
        state = uiState.inputState,
        showStateIcon = false,
        textPreprocessor = paymentAddressViewModel,
    ) {
        viewModel.onEnterAddress(it)
    }

    if (uiState.value.isBlank()) {
        AddressSuggestions(uiState.contacts) {
            viewModel.onEnterAddress(it)
        }
    } else if (uiState.addressCheckEnabled || uiState.addressValidationError != null) {
        AddressDefenseMessage(
            uiState.addressValidationInProgress,
            uiState.addressValidationError,
            uiState.checkResults,
        ) {
            navController.slideFromBottom(
                R.id.defenseSystemFeatureDialog,
                DefenseSystemFeatureDialog.Input(PremiumFeature.SecureSendFeature, true)
            )
        }
    }
}

@Composable
fun AddressDefenseMessage(
    addressValidationInProgress: Boolean,
    addressValidationError: Throwable?,
    checkResults: Map<AddressCheckType, AddressCheckData>,
    onActivateClick: () -> Unit
) {
    val noSubscription = checkResults.any { it.value.checkResult == AddressCheckResult.NotAllowed }
    val isDanger = checkResults.any { it.value.checkResult == AddressCheckResult.Detected }
    val invalidAddress = addressValidationError != null

    val level = when {
        addressValidationInProgress -> DefenseAlertLevel.IDLE
        invalidAddress -> DefenseAlertLevel.DANGER
        noSubscription -> DefenseAlertLevel.WARNING
        isDanger -> DefenseAlertLevel.DANGER
        else -> DefenseAlertLevel.SAFE
    }

    val title: Int = when {
        addressValidationInProgress -> R.string.WalletConnect_Checking
        invalidAddress -> R.string.Send_Address_Error_InvalidAddress
        noSubscription -> R.string.AddressEnter_NeedSubscription_Title
        isDanger -> R.string.AddressEnter_Danger_Title
        else -> R.string.AddressEnter_Safe_Title
    }

    val content: Int? = when {
        addressValidationInProgress -> null
        invalidAddress -> R.string.Send_Address_Error_InvalidAddress_Description
        noSubscription -> R.string.AddressEnter_NeedSubscription_Content
        isDanger -> R.string.AddressEnter_Danger_Content
        else -> R.string.AddressEnter_Safe_Content
    }

    val icon = when (level) {
        DefenseAlertLevel.WARNING -> R.drawable.warning_filled_24
        DefenseAlertLevel.DANGER -> R.drawable.warning_filled_24
        DefenseAlertLevel.SAFE -> R.drawable.shield_check_filled_24
        DefenseAlertLevel.IDLE -> null
    }

    val actionText = when {
        noSubscription && !addressValidationInProgress -> R.string.Button_Activate
        else -> null
    }

    DefenseSystemMessage(
        level = level,
        title = stringResource(title),
        content = content?.let { stringResource(it) },
        above = false,
        icon = icon,
        actionText = actionText?.let { stringResource(it)},
        onClick = onActivateClick
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
