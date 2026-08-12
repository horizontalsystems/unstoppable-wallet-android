package io.horizontalsystems.walletkit.modules.balance.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.InfoTextBody
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.walletkit.uiv3.components.controls.HSButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZcashMigrationBottomSheet(
    sheetState: SheetState,
    onMigrateClick: () -> Unit,
    onClose: () -> Unit
) {
    BottomSheetContent(
        onDismissRequest = onClose,
        sheetState = sheetState,
        buttons = {
            HSButton(
                title = stringResource(R.string.Balance_Zcash_Migration_Migrate),
                variant = ButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth(),
                onClick = onMigrateClick
            )
        }
    ) {
        BottomSheetHeaderV3(
            image72 = painterResource(R.drawable.ic_share_24),
            title = stringResource(R.string.Balance_Zcash_Migration_Title)
        )
        InfoTextBody(
            text = stringResource(R.string.Balance_Zcash_Migration_Description),
            color = ComposeAppTheme.colors.grey,
            textAlign = TextAlign.Center
        )
        VSpacer(16.dp)
    }
}
