package com.quantum.wallet.bankwallet.modules.manageaccount

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.managers.FaqManager
import com.quantum.wallet.bankwallet.modules.manageaccount.ui.ActionButton
import com.quantum.wallet.bankwallet.modules.manageaccount.ui.ConfirmCopyBottomSheet
import com.quantum.wallet.bankwallet.modules.manageaccount.ui.HidableContent
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.components.MenuItem
import com.quantum.wallet.bankwallet.ui.compose.components.TextImportantWarning
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.ui.helpers.TextHelper
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold
import com.quantum.wallet.core.helpers.HudHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretKeyScreen(
    navController: NavController,
    secretKey: String,
    title: String,
    hideScreenText: String,
    onCopyKey: () -> Unit,
    onOpenFaq: () -> Unit,
    onToggleHidden: () -> Unit,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    HSScaffold(
        title = title,
        onBack = navController::popBackStack,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Info_Title),
                icon = R.drawable.ic_info_24,
                onClick = {
                    FaqManager.showFaqPage(navController, FaqManager.faqPathPrivateKeys)

                    onOpenFaq()
                }
            )
        )
    ) {
        Column {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top
            ) {
                VSpacer(12.dp)
                TextImportantWarning(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.PrivateKeys_NeverShareWarning)
                )
                VSpacer(24.dp)
                HidableContent(secretKey, hideScreenText, onToggleHidden)
            }

            ActionButton(R.string.Alert_Copy) {
                showBottomSheet = true
            }
        }
        if (showBottomSheet) {
            ConfirmCopyBottomSheet(
                sheetState = sheetState,
                onConfirm = {
                    scope.launch {
                        sheetState.hide()
                        showBottomSheet = false

                    }
                    TextHelper.copyText(secretKey)
                    HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)

                    onCopyKey()
                },
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                        showBottomSheet = false
                    }
                }
            )
        }
    }
}