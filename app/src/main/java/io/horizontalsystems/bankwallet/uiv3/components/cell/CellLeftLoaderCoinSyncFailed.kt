package io.horizontalsystems.bankwallet.uiv3.components.cell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun CellLeftLoaderCoinSyncFailed(onClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(ComposeAppTheme.colors.blade),
        contentAlignment = Alignment.Center
    ) {
        val clickModifier = if (onClick != null) {
            Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, color = ComposeAppTheme.colors.leah),
                onClick = onClick
            )
        } else {
            Modifier
        }

        Icon(
            modifier = Modifier.then(clickModifier),
            painter = painterResource(id = R.drawable.warning_filled_24),
            contentDescription = "sync failed",
            tint = ComposeAppTheme.colors.lucian
        )
    }
}

@Preview
@Composable
fun Preview_CellLeftLoaderCoinSyncFailed() {
    ComposeAppTheme {
        CellLeftLoaderCoinSyncFailed {

        }
    }
}
