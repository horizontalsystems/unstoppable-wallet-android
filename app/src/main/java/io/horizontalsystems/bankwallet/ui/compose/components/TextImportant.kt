package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun TextImportantWarning(
    modifier: Modifier = Modifier,
    text: String,
    title: String? = null,
    @DrawableRes icon: Int? = null
) {
    TextImportant(
        modifier = modifier,
        text = text,
        title = title,
        icon = icon,
        borderColor = ComposeAppTheme.colors.jacob,
        backgroundColor = ComposeAppTheme.colors.yellow20,
        textColor = ComposeAppTheme.colors.jacob,
        iconColor = ComposeAppTheme.colors.jacob
    )
}

@Composable
fun TextImportantError(
    modifier: Modifier = Modifier,
    text: String,
    title: String? = null,
    @DrawableRes icon: Int? = null
) {
    TextImportant(
        modifier = modifier,
        text = text,
        title = title,
        icon = icon,
        borderColor = ComposeAppTheme.colors.lucian,
        backgroundColor = ComposeAppTheme.colors.red20,
        textColor = ComposeAppTheme.colors.lucian,
        iconColor = ComposeAppTheme.colors.lucian
    )
}

@Composable
fun TextImportant(
    modifier: Modifier = Modifier,
    text: String,
    title: String? = null,
    @DrawableRes icon: Int? = null,
    borderColor: Color,
    backgroundColor: Color,
    textColor: Color,
    iconColor: Color
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(0.5.dp, borderColor, RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (title != null || icon != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon?.let {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        tint = iconColor
                    )
                }
                title?.let {
                    Text(
                        text = it,
                        color = textColor,
                        style = ComposeAppTheme.typography.subhead
                    )
                }
            }
        }
        if (text.isNotEmpty()) {
            subhead2_leah(text = text)
        }
    }
}

@Composable
fun TextAttention(
    modifier: Modifier = Modifier,
    text: String,
) {
    TextWarningVersion2(
        modifier = modifier,
        text = text,
        title = stringResource(R.string.Attention_Title),
        icon = R.drawable.ic_warning_filled_20
    )
}

@Composable
fun TextWarningVersion2(
    modifier: Modifier = Modifier,
    text: String,
    title: String? = null,
    @DrawableRes icon: Int? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(0.5.dp, ComposeAppTheme.colors.jacob, RoundedCornerShape(16.dp))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (title != null || icon != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon?.let {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.jacob
                    )
                }
                title?.let {
                    headline2_jacob(
                        text = it,
                    )
                }
            }
        }
        if (text.isNotEmpty()) {
            subheadR_leah(
                text = text,
                textAlign = TextAlign.Center
            )
        }
    }
}