package io.horizontalsystems.walletkit.modules.manageaccount

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
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.managers.FaqManager
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.manageaccount.ui.ActionButton
import io.horizontalsystems.walletkit.modules.manageaccount.ui.ConfirmCopyBottomSheet
import io.horizontalsystems.walletkit.modules.manageaccount.ui.HidableContent
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.ui.compose.components.TextImportantWarning
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.helpers.TextHelper
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretKeyScreen(
    navigation: HSNavigation,
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
        onBack = navigation::removeLastOrNull,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Info_Title),
                icon = R.drawable.ic_info_24,
                onClick = {
                    FaqManager.showFaqPage(navigation, FaqManager.faqPathPrivateKeys)

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