package io.horizontalsystems.bankwallet.modules.confirm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.message.DefenseSystemMessage
import io.horizontalsystems.bankwallet.uiv3.components.message.DefenseSystemState

@Composable
fun ConfirmTransactionScreen(
    title: String = stringResource(R.string.Swap_Confirm_Title),
    onClickBack: (() -> Unit)?,
    onClickSettings: (() -> Unit)?,
    onClickClose: (() -> Unit)?,
    buttonsSlot: @Composable() (ColumnScope.() -> Unit),
    content: @Composable() (ColumnScope.() -> Unit)
) {
    HSScaffold(
        title = title,
        onBack = onClickBack,
        menuItems = buildList {
            onClickSettings?.let {
                add(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Settings_Title),
                        icon = R.drawable.ic_manage_2_24,
                        onClick = onClickSettings
                    )
                )
            }
            onClickClose?.let {
                add(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = onClickClose
                    )
                )
            }
        },
    ) {
        Box(){
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                VSpacer(height = 12.dp)

                content.invoke(this)

                VSpacer(height = 362.dp)
            }
            Column(
                modifier = Modifier.align(Alignment.BottomCenter)
            ){

                SwapConfirmDefenseMessage()
                VSpacer(height = 16.dp)
                ButtonsGroupWithShade {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        content = buttonsSlot
                    )
                }
            }
        }
    }
}

@Composable
private fun SwapConfirmDefenseMessage(
    state: DefenseSystemState = DefenseSystemState.WARNING,
    onClick: () -> Unit = {},
) {
    val title: Int = when (state) {
        DefenseSystemState.WARNING -> R.string.SwapConfirm_DefenseMessage_Warning_Title
        DefenseSystemState.DANGER -> R.string.SwapConfirm_DefenseMessage_Danger_Title
        DefenseSystemState.SAFE -> R.string.SwapConfirm_DefenseMessage_Safe_Title
        DefenseSystemState.IDLE -> R.string.SwapConfirm_DefenseMessage_NotAvailable_Title
    }
    val content: Int? = when (state) {
        DefenseSystemState.WARNING -> R.string.SwapConfirm_DefenseMessage_Warning_Description
        DefenseSystemState.DANGER -> R.string.SwapConfirm_DefenseMessage_Danger_Description
        DefenseSystemState.SAFE -> R.string.SwapConfirm_DefenseMessage_Safe_Description
        DefenseSystemState.IDLE -> R.string.SwapConfirm_DefenseMessage_NotAvailable_Description
    }
    val icon = when (state) {
        DefenseSystemState.WARNING -> R.drawable.warning_filled_24
        DefenseSystemState.DANGER -> R.drawable.warning_filled_24
        DefenseSystemState.SAFE -> R.drawable.shield_check_filled_24
        DefenseSystemState.IDLE -> R.drawable.close_e_filled_24
    }
    val actionText: Int? = when (state) {
        DefenseSystemState.WARNING -> R.string.Button_Activate
        DefenseSystemState.IDLE -> R.string.Button_LearnMore
        else -> null
    }
    val onActionClick = when (state) {
        DefenseSystemState.IDLE -> onClick
        DefenseSystemState.WARNING -> onClick
        else -> null
    }
    DefenseSystemMessage(
        state = state,
        title = stringResource(title),
        content = content?.let { stringResource(it) },
        icon = icon,
        actionText = actionText?.let{ stringResource(it)},
        onClick = onActionClick
    )
}