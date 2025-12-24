package io.horizontalsystems.bankwallet.uiv3.components.message

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellSecondary
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs

@Composable
fun DefenseSystemMessage(
    state: DefenseSystemState,
    title: String,
    content: String?,
    icon: Int? = null,
    actionText: String? = null,
    onClick: (() -> Unit)? = null,
) {

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            onClick = onClick,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        )
    } else {
        Modifier
    }

    val contentColor = when (state) {
        DefenseSystemState.SAFE,
        DefenseSystemState.WARNING -> Color.Black

        else -> Color.White
    }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(state.bubbleColor)
                .then(clickableModifier)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    icon?.let { iconRes ->
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    HSpacer(8.dp)
                    Text(
                        text = title,
                        style = ComposeAppTheme.typography.headline2,
                        color = contentColor
                    )
                }

                content?.let {
                    Text(
                        text = content,
                        style = ComposeAppTheme.typography.subheadR,
                        color = contentColor
                    )
                }

                actionText?.let { action ->
                    VSpacer(12.dp)
                    Row(modifier = Modifier.align(Alignment.End)) {
                        Text(
                            text = action,
                            style = ComposeAppTheme.typography.subheadSB,
                            color = contentColor
                        )
                        HSpacer(8.dp)
                        Icon(
                            painter = painterResource(R.drawable.arrow_m_right_24),
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        // Speech bubble tail
        Box(
            modifier = Modifier
                .offset(x = 48.dp, y = (-8).dp)
                .size(16.dp)
                .rotate(45f)
                .background(state.bubbleColor)
        )
    }

    CellSecondary(
        left = {
            Image(
                painter = painterResource(R.drawable.ic_defense_shield_20),
                contentDescription = null,
            )
        },
        middle = {
            CellMiddleInfo(
                subtitle = stringResource(R.string.Premium_DefenseSystem).hs
            )
        },
    )
}

enum class DefenseSystemState {
    WARNING,
    IDLE,
    DANGER,
    SAFE;

    val bubbleColor: Color
        @Composable
        get() {
            return when (this) {
                WARNING -> ComposeAppTheme.colors.yellowD
                IDLE -> ComposeAppTheme.colors.andy
                DANGER -> ComposeAppTheme.colors.redL
                SAFE -> ComposeAppTheme.colors.greenD
            }
        }
}