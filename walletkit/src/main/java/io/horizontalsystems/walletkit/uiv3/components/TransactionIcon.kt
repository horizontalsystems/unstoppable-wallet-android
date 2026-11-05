package io.horizontalsystems.walletkit.uiv3.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme

/**
 * Muted: ended without completing, though nothing was lost (refunded, expired).
 * Warning: stopped until the user acts.
 */
enum class TransactionIconState { Pending, Completed, Failed, Muted, Warning }

@Composable
fun TransactionIcon(
    @DrawableRes icon: Int,
    state: TransactionIconState,
    modifier: Modifier = Modifier,
) {
    val tint = when (state) {
        TransactionIconState.Pending -> ComposeAppTheme.colors.grey
        TransactionIconState.Completed -> ComposeAppTheme.colors.leah
        TransactionIconState.Failed -> ComposeAppTheme.colors.lucian
        TransactionIconState.Muted -> ComposeAppTheme.colors.grey
        TransactionIconState.Warning -> ComposeAppTheme.colors.jacob
    }

    Box(modifier = modifier.size(40.dp), contentAlignment = Alignment.Center) {
        if (state == TransactionIconState.Pending) {
            LoaderPending()
        }
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(icon),
            tint = tint,
            contentDescription = null
        )
    }
}

@Preview
@Composable
fun Preview_TransactionIcon() {
    ComposeAppTheme(darkTheme = true) {
        Row {
            TransactionIcon(R.drawable.arrow_m_down_24, TransactionIconState.Pending)
            TransactionIcon(R.drawable.arrow_m_down_24, TransactionIconState.Completed)
            TransactionIcon(R.drawable.arrow_m_down_24, TransactionIconState.Failed)
            TransactionIcon(R.drawable.arrow_m_down_24, TransactionIconState.Muted)
            TransactionIcon(R.drawable.arrow_m_down_24, TransactionIconState.Warning)
        }
    }
}
