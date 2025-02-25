package cash.p.terminal.modules.manageaccount.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.modules.manageaccount.recoveryphrase.RecoveryPhraseModule
import cash.p.terminal.ui_compose.components.B2
import cash.p.terminal.ui_compose.components.ButtonPrimaryRed
import cash.p.terminal.ui_compose.components.ButtonPrimaryTransparent
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.C2
import cash.p.terminal.ui_compose.components.CellSingleLineLawrenceSection
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.D1
import cash.p.terminal.ui_compose.components.D2
import cash.p.terminal.ui_compose.components.D7
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui.compose.components.InfoText
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.TextImportantWarning
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui.extensions.BottomSheetHeader
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun ActionButton(title: Int, onClick: () -> Unit) {
    ButtonsGroupWithShade {
        ButtonPrimaryYellow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp),
            title = stringResource(title),
            onClick = onClick,
        )
    }
}

@Composable
fun HidableContent(
    content: String,
    hideScreenText: String? = null,
    onToggleHidden: (() -> Unit)? = null
) {
    var hidden by remember { mutableStateOf(hideScreenText != null) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.steel20, RoundedCornerShape(24.dp))
            .clickable(enabled = hideScreenText != null, onClick = {
                hidden = !hidden
                onToggleHidden?.invoke()
            })
    ) {

        D2(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            text = content
        )

        if (hideScreenText != null && hidden) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(ComposeAppTheme.colors.tyler),
                contentAlignment = Alignment.Center
            ) {
                subhead2_grey(hideScreenText)
            }
        }
    }
}

@Composable
fun KeyActionItem(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    CellUniversalLawrenceSection(
        listOf {
            RowUniversal(
                onClick = onClick
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                body_leah(
                    modifier = Modifier.weight(1f),
                    text = title,
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
        })
    InfoText(text = description)
    Spacer(Modifier.height(20.dp))
}

@Composable
fun ConfirmCopyBottomSheet(onConfirm: () -> Unit, onCancel: () -> Unit) {
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_attention_24),
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
        title = stringResource(R.string.RecoveryPhrase_CopyWarning_Title),
        onCloseClick = onCancel
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        TextImportantWarning(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.ShowKey_PrivateKeyCopyWarning_Text)
        )

        Spacer(modifier = Modifier.height(32.dp))

        ButtonPrimaryRed(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            title = stringResource(R.string.ShowKey_PrivateKeyCopyWarning_Proceed),
            onClick = onConfirm
        )

        Spacer(modifier = Modifier.height(12.dp))

        ButtonPrimaryTransparent(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            title = stringResource(R.string.ShowKey_PrivateKeyCopyWarning_Cancel),
            onClick = onCancel
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun PassphraseCell(passphrase: String, hidden: Boolean) {
    if (passphrase.isNotBlank()) {
        CellSingleLineLawrenceSection(
            listOf {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_key_phrase_20),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.grey
                    )
                    D1(
                        text = stringResource(R.string.ShowKey_Passphrase),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    C2(text = if (hidden) "*****" else passphrase)
                }
            })
        Spacer(Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SeedPhraseList(
    wordsNumbered: List<RecoveryPhraseModule.WordNumbered>,
    hidden: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(24.dp))
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            maxItemsInEachRow = 4,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalArrangement = Arrangement.Center
        ) {
            wordsNumbered.forEach { word ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    D7(text = word.number.toString())
                    HSpacer(8.dp)
                    B2(text = word.word)
                }
            }
        }

        if (hidden) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(ComposeAppTheme.colors.tyler),
                contentAlignment = Alignment.Center
            ) {
                subhead2_grey(text = stringResource(R.string.RecoveryPhrase_ShowPhrase))
            }
        }
    }
}
