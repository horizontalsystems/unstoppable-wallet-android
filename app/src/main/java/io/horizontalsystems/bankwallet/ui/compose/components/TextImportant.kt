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
import androidx.compose.ui.unit.dp
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
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
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
                        style = ComposeAppTheme.typography.subhead1
                    )
                }
            }
        }
        if (text.isNotEmpty()) {
            subhead2_leah(text = text)
        }
    }
}
