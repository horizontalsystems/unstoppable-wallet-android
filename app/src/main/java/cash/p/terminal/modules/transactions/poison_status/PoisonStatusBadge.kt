@file:Suppress("PackageNaming")

package cash.p.terminal.modules.transactions.poison_status

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HsIconButton
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@get:DrawableRes
val PoisonStatus.iconRes: Int
    get() = when (this) {
        PoisonStatus.SUSPICIOUS -> R.drawable.ic_trust_suspicious_20
        PoisonStatus.CREATED -> R.drawable.ic_trust_wallet_20
        PoisonStatus.ADDRESS_BOOK -> R.drawable.ic_trust_address_book_20
        PoisonStatus.BLOCKCHAIN -> R.drawable.ic_trust_blockchain_20
    }

@get:StringRes
val PoisonStatus.labelRes: Int
    get() = when (this) {
        PoisonStatus.SUSPICIOUS -> R.string.address_poisoning_view_suspicious
        PoisonStatus.CREATED -> R.string.address_poisoning_view_created_by_wallet
        PoisonStatus.ADDRESS_BOOK -> R.string.address_poisoning_view_address_book
        PoisonStatus.BLOCKCHAIN -> R.string.address_poisoning_view_blockchain
    }

@get:StringRes
val PoisonStatus.descriptionRes: Int
    get() = when (this) {
        PoisonStatus.SUSPICIOUS -> R.string.transaction_status_suspicious_description
        PoisonStatus.CREATED -> R.string.transaction_status_created_description
        PoisonStatus.ADDRESS_BOOK -> R.string.transaction_status_address_book_description
        PoisonStatus.BLOCKCHAIN -> R.string.transaction_status_blockchain_description
    }

@Composable
fun PoisonStatusBadge(
    poisonStatus: PoisonStatus,
    modifier: Modifier = Modifier,
    text: String? = null,
    onInfoClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(poisonStatus.iconRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp),
        )
        HSpacer(6.dp)
        subhead2_grey(
            text = text ?: stringResource(poisonStatus.labelRes),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        HSpacer(6.dp)
        onInfoClick?.let {
            HsIconButton(
                onClick = onInfoClick,
                modifier = Modifier.size(20.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_info_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey,
                )
            }
        }
    }
}

@Composable
fun PoisonStatusInfoEntry(
    status: PoisonStatus,
    titleContent: @Composable (String) -> Unit,
    descriptionContent: @Composable (String) -> Unit,
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            painter = painterResource(status.iconRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            titleContent(stringResource(status.labelRes).replaceFirstChar { it.uppercase() })
            VSpacer(4.dp)
            descriptionContent(stringResource(status.descriptionRes))
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PoisonStatusBadgePreview() {
    ComposeAppTheme {
        Column {
            PoisonStatus.entries.forEach { status ->
                PoisonStatusBadge(
                    poisonStatus = status,
                    onInfoClick = {},
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
        }
    }
}
