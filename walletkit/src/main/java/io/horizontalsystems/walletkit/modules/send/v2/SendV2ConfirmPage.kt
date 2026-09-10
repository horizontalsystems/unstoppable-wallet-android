package io.horizontalsystems.walletkit.modules.send.v2

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.SendConfirmationScreen
import io.horizontalsystems.walletkit.serializers.BigDecimalSerializer
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.ui.compose.components.MenuItemDropdown
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/** Confirmation step of [SendV2Page], one page for every blockchain type. */
@Serializable
data class SendV2ConfirmPage(val input: Input) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val chainSettings = navigation.viewModelForScreen<SendChainSettingsViewModel>(SendV2Page::class)
        val viewModel = navigation.viewModelForScreen<SendV2ConfirmViewModel>(
            contentKey(),
            SendV2ConfirmViewModel.Factory(input, chainSettings.settings),
        )
        SendV2ConfirmScreen(navigation, viewModel, contentKey())
    }

    @Serializable
    data class Input(
        val wallet: Wallet,
        @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal,
        val address: Address,
        val memo: String?,
    )
}

@Composable
private fun SendV2ConfirmScreen(
    navigation: HSNavigation,
    viewModel: SendV2ConfirmViewModel,
    screenContentKey: String,
) {
    val uiState = viewModel.uiState
    val token = uiState.wallet.token

    val settingsItems = buildList {
        if (uiState.hasSettings) {
            add(
                MenuItem(
                    title = TranslatableString.ResString(R.string.SendEvmSettings_EditFee),
                    onClick = { navigation.slideFromRight(SendV2SettingsPage(screenContentKey)) }
                )
            )
        }
        if (uiState.hasNonceSettings) {
            add(
                MenuItem(
                    title = TranslatableString.ResString(R.string.SendEvmSettings_Nonce),
                    onClick = { navigation.slideFromRight(SendV2NonceSettingsPage(screenContentKey)) }
                )
            )
        }
    }
    val menuItems = if (settingsItems.isEmpty()) {
        listOf()
    } else {
        listOf(
            MenuItemDropdown(
                title = TranslatableString.ResString(R.string.Settings_Title),
                icon = R.drawable.manage_24,
                items = settingsItems,
            )
        )
    }

    SendConfirmationScreen(
        navigation = navigation,
        coinMaxAllowedDecimals = token.decimals,
        feeCoinMaxAllowedDecimals = uiState.feeCoinDecimals,
        rate = uiState.rate,
        feeCoinRate = uiState.feeCoinRate,
        sendResult = viewModel.sendResult,
        token = token,
        feeCoin = uiState.feeCoin,
        amount = uiState.amount,
        address = uiState.address,
        contact = uiState.contact,
        fee = uiState.fee,
        memo = uiState.memo,
        onClickSend = viewModel::onClickSend,
        sendEntryPointDestId = SendV2Page::class,
        error = uiState.error,
        menuItems = menuItems,
        cautions = uiState.cautions,
        sendEnabled = uiState.sendable && !uiState.loading,
    ) {
        uiState.fields.forEach { it.GetContent(navigation) }
    }
}
