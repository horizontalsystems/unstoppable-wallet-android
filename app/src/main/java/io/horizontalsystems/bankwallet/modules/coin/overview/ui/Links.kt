package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.CoinLink
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineClear
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.marketkit.models.LinkType

@Preview
@Composable
fun LinksPreview() {
    ComposeAppTheme {
        val links = listOf(
            CoinLink(
                "http://q.com",
                LinkType.Guide,
                "@twitter",
                R.drawable.ic_academy_20
            ),
            CoinLink(
                "http://q.com",
                LinkType.Guide,
                "@twitter",
                R.drawable.ic_globe_20
            ),
            CoinLink(
                "http://q.com",
                LinkType.Twitter,
                "@twitter",
                R.drawable.ic_twitter_20
            ),
            CoinLink(
                "http://q.com",
                LinkType.Telegram,
                "Telegram",
                R.drawable.ic_telegram_20
            ),
        )
        Links(links = links, onCoinLinkClick = {})
    }
}

@Composable
fun Links(links: List<CoinLink>, onCoinLinkClick: (CoinLink) -> Unit) {
    Column {
        CellSingleLineClear(borderTop = true) {
            body_leah(text = stringResource(id = R.string.CoinPage_Links))
        }

        CellUniversalLawrenceSection(links) { link ->
            RowUniversal(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = { onCoinLinkClick(link) }
            ) {
                Image(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(id = link.icon),
                    contentDescription = "link",
                )
                body_leah(
                    text = link.title,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = ""
                )
            }
        }
    }
}