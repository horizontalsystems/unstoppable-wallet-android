package io.horizontalsystems.bankwallet.ui.extensions

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellSecondary
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import io.horizontalsystems.core.helpers.HudHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetSelectorMultiple(
    sheetState: SheetState,
    config: BottomSheetSelectorMultipleDialog.Config,
    onItemsSelected: (List<Int>) -> Unit,
    onCloseClick: () -> Unit,
) {
    val selected =
        remember(config.uuid) { mutableStateListOf<Int>().apply { addAll(config.selectedIndexes) } }
    val localView = LocalView.current

    BottomSheetContent(
        onDismissRequest = onCloseClick,
        sheetState = sheetState,
        buttons = {
            HSButton(
                title = stringResource(R.string.Button_Done),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onItemsSelected(selected)
                    onCloseClick.invoke()
                },
                enabled = config.allowEmpty || selected.isNotEmpty()
            )
        }
    ) {
        BottomSheetHeaderV3(
            title = config.title
        )
        TextBlock(
            text = stringResource(R.string.AddressFormatSettings_Subtitle),
            textAlign = TextAlign.Center
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(0.5.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(16.dp))
                .padding(vertical = 8.dp),
        ) {
            config.viewItems.forEachIndexed { index, item ->
                val onClick = if (item.copyableString != null) {
                    {
                        HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
                        TextHelper.copyText(item.copyableString)
                    }
                } else {
                    null
                }

                if (index > 0) {
                    HsDivider()
                }

                CellSecondary(
                    middle = {
                        CellMiddleInfo(
                            title = item.title.hs,
                            subtitle = item.subtitle.hs
                        )
                    },
                    right = {
                        HsSwitch(
                            modifier = Modifier.padding(start = 5.dp),
                            checked = selected.contains(index),
                            onCheckedChange = { checked ->
                                if (checked) {
                                    selected.add(index)
                                } else {
                                    selected.remove(index)
                                }
                            },
                        )
                    },
                    onClick = onClick
                )
            }
        }

        TextBlock(text = stringResource(R.string.AddressFormatSettings_Description))
    }
}
