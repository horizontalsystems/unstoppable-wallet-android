package io.horizontalsystems.walletkit.modules.usersubscription.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.designkit.components.ButtonVariant
import io.horizontalsystems.designkit.components.HSButton
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.uiv3.TestTags
import io.horizontalsystems.walletkit.uiv3.components.info.TextBlock

@Composable
fun PremiumSubscribedScreen(
    onCloseClick: () -> Unit
) {
    PremiumPage(
        onClose = onCloseClick,
        closeTestTag = TestTags.PREMIUM_SUBSCRIBED_CLOSE,
        button = {
            HSButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.PREMIUM_SUBSCRIBED_GO_TO_APP),
                variant = ButtonVariant.Secondary,
                title = stringResource(R.string.Premium_GoToApp),
                onClick = onCloseClick,
            )
        }
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            text = stringResource(R.string.Premium_ThankYouForSubscription),
            color = ComposeAppTheme.colors.leah,
            style = ComposeAppTheme.typography.title3,
            textAlign = TextAlign.Center,
        )
        TextBlock(
            text = stringResource(R.string.Premium_EnjoyPremiumFeatures),
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
fun PremiumSubscribedScreenPreview() {
    ComposeAppTheme {
        PremiumSubscribedScreen(
            onCloseClick = {}
        )
    }
}
