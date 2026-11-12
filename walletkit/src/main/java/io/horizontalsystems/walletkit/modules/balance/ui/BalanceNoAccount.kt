package io.horizontalsystems.walletkit.modules.balance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.designkit.components.ButtonVariant
import io.horizontalsystems.designkit.components.HSButton
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.modules.createaccount.CreateAccountPage
import io.horizontalsystems.walletkit.modules.importwallet.ImportWalletPage
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.uiv3.TestTags
import io.horizontalsystems.walletkit.uiv3.components.HSPreview
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold

/** The Balance tab once every wallet has been removed. */
@Composable
fun BalanceNoAccount(navigation: HSNavigation) {
    HSScaffold(
        title = stringResource(R.string.Wallet_Title)
    ) {
        NoWalletsMessage(
            // Leaves out the bottom navigation's height, so the message is
            // centred in what is visible
            modifier = Modifier.padding(bottom = 56.dp),
            onNewWalletClick = {
                navigation.slideFromRight(CreateAccountPage())
                stat(page = StatPage.Balance, event = StatEvent.Open(StatPage.NewWallet))
            },
            onExistingWalletClick = {
                navigation.slideFromRight(ImportWalletPage())
                stat(page = StatPage.Balance, event = StatEvent.Open(StatPage.ImportWallet))
            },
        )
    }
}

@Composable
private fun NoWalletsMessage(
    modifier: Modifier = Modifier,
    onNewWalletClick: () -> Unit,
    onExistingWalletClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            modifier = Modifier
                .padding(16.dp)
                .size(72.dp),
            painter = painterResource(R.drawable.wallet_add_24),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey,
        )
        VSpacer(16.dp)
        Text(
            modifier = Modifier.padding(horizontal = 40.dp),
            text = stringResource(R.string.Balance_NoWallets),
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.subheadR,
            textAlign = TextAlign.Center,
        )
        VSpacer(24.dp)
        HSButton(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.ONBOARDING_NEW_WALLET),
            variant = ButtonVariant.Primary,
            title = stringResource(R.string.ManageAccounts_CreateNewWallet),
            onClick = onNewWalletClick,
        )
        VSpacer(12.dp)
        HSButton(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.ONBOARDING_EXISTING_WALLET),
            variant = ButtonVariant.Secondary,
            title = stringResource(R.string.ManageAccounts_ExistingWallet),
            onClick = onExistingWalletClick,
        )
    }
}

@Preview
@Composable
private fun Preview_NoWalletsMessage() {
    HSPreview {
        NoWalletsMessage(onNewWalletClick = {}, onExistingWalletClick = {})
    }
}
