package io.horizontalsystems.bankwallet.modules.send.evm.confirmation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.confirm.ConfirmTransactionScreen
import io.horizontalsystems.bankwallet.modules.confirm.ErrorSheet
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.send.evm.settings.SendEvmNonceSettingsPage
import io.horizontalsystems.bankwallet.modules.send.evm.settings.SendEvmSettingsPage
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionView
import io.horizontalsystems.bankwallet.serializers.HSScreenKClassSerializer
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.rememberAsyncAction
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class SendEvmConfirmationPage(val input: Input) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        SendEvmConfirmationScreen(navigation, input)
    }

    @Serializable
    data class Input(
        val transactionDataParcelable: SendEvmModule.TransactionDataParcelable,
        val additionalInfo: SendEvmData.AdditionalInfo?,
        val blockchainType: BlockchainType,
        @Serializable(with = HSScreenKClassSerializer::class) val sendEntryPointDestId: KClass<out HSPage>
    ) {
        val transactionData: TransactionData
            get() = TransactionData(
                Address(transactionDataParcelable.toAddress),
                transactionDataParcelable.value,
                transactionDataParcelable.input
            )

        constructor(
            sendData: SendEvmData,
            blockchainType: BlockchainType,
            sendEntryPointDestId: KClass<out HSPage>
        ) : this(
            SendEvmModule.TransactionDataParcelable(sendData.transactionData),
            sendData.additionalInfo,
            blockchainType,
            sendEntryPointDestId
        )
    }
}

@Composable
private fun SendEvmConfirmationScreen(
    navigation: HSNavigation,
    input: SendEvmConfirmationPage.Input
) {
    val logger = remember { AppLogger("send-evm") }

    val viewModel = viewModel<SendEvmConfirmationViewModel>(
        factory = SendEvmConfirmationViewModel.Factory(
            input.transactionData,
            input.additionalInfo,
            input.blockchainType,
        )
    )
    val uiState = viewModel.uiState

    ConfirmTransactionScreen(
        title = stringResource(R.string.Send_Confirmation_Title),
        initialLoading = uiState.initialLoading,
        onClickBack = { navigation.removeLastOrNull() },
        onClickFeeSettings = {
            navigation.slideFromBottom(SendEvmSettingsPage)
        },
        onClickNonceSettings = {
            navigation.slideFromBottom(SendEvmNonceSettingsPage)
        },
        buttonsSlot = {
            val view = LocalView.current
            val sendAction = rememberAsyncAction()

            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp),
                title = stringResource(if (sendAction.inProgress) R.string.Send_Sending else R.string.Send_Confirmation_Send_Button),
                onClick = {
                    logger.info("click send button")
                    sendAction.run {
                        try {
                            logger.info("sending tx")
                            viewModel.send()
                            logger.info("success")
                            stat(page = StatPage.SendConfirmation, event = StatEvent.Send)

                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                            delay(1200)

                            navigation.removeLastUntil(input.sendEntryPointDestId, true)
                        } catch (t: Throwable) {
                            logger.warning("failed", t)
                            navigation.slideFromBottom(ErrorSheet(
                                ErrorSheet.Input(t.message ?: t.javaClass.simpleName)
                            ))
                        }
                    }
                },
                enabled = !sendAction.inProgress && uiState.sendEnabled
            )
        }
    ) {
        SendEvmTransactionView(
            navigation,
            uiState.sectionViewItems,
            uiState.cautions,
            uiState.transactionFields,
            uiState.networkFee,
            StatPage.SendConfirmation
        )
    }
}

