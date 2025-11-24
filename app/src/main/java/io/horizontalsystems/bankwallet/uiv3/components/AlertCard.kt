package io.horizontalsystems.bankwallet.uiv3.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subheadR_leah

@Composable
fun AlertCard(
    modifier: Modifier = Modifier,
    format: AlertFormat,
    type: AlertType,
    text: String,
    onClick: (() -> Unit)? = null
) {
    val alertColor = when (type) {
        AlertType.Critical -> ComposeAppTheme.colors.lucian
        AlertType.Caution -> ComposeAppTheme.colors.jacob
    }

    val title = when (type) {
        AlertType.Critical -> stringResource(R.string.Alert_TitleWarning)
        AlertType.Caution -> stringResource(R.string.Attention_Title)
    }

    val clickModifier = when (onClick) {
        null -> Modifier
        else -> Modifier.clickable(onClick = onClick)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .then(clickModifier)
            .border(1.dp, alertColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        when (format) {
            AlertFormat.Structured -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.ic_warning_filled_24),
                        contentDescription = null,
                        tint = alertColor
                    )
                    HSpacer(8.dp)
                    Text(
                        text = title,
                        style = ComposeAppTheme.typography.headline2,
                        color = alertColor,
                    )
                }
                VSpacer(8.dp)
                subheadR_leah(
                    text,
                    textAlign = TextAlign.Center
                )
            }
            AlertFormat.Inline -> {
                Row {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.ic_warning_filled_24),
                        contentDescription = null,
                        tint = alertColor
                    )
                    HSpacer(8.dp)
                    subheadR_leah(text)
                }
            }
        }
    }
}

enum class AlertType {
    Critical, Caution
}

enum class AlertFormat {
    Structured, Inline
}

@Preview
@Composable
fun Preview_AlertCard() {
    ComposeAppTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AlertCard(
                format = AlertFormat.Structured,
                type = AlertType.Critical,
                text  = "You are about to send funds to a high-risk address. Proceeding may result in loss or other security risks.",
            )
            AlertCard(
                format = AlertFormat.Structured,
                type = AlertType.Caution,
                text  = "The contract you are connecting to is flagged as blacklisted. ",
            )
            AlertCard(
                format = AlertFormat.Inline,
                type = AlertType.Critical,
                text  = "You are about to send funds to a high-risk address. Proceeding may result in loss or other security risks.",
            )
            AlertCard(
                format = AlertFormat.Inline,
                type = AlertType.Caution,
                text  = "The contract you are connecting to is flagged as blacklisted.",
            )
        }
    }
}
