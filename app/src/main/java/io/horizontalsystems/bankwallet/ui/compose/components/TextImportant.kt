package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun TextImportant(text: String, title: String? = null, @DrawableRes icon: Int? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, ComposeAppTheme.colors.jacob, RoundedCornerShape(8.dp))
            .background(ComposeAppTheme.colors.yellow20)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (title != null || icon != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                icon?.let {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.jacob
                    )
                }
                title?.let {
                    Text(
                        text = it,
                        color = ComposeAppTheme.colors.jacob,
                        style = ComposeAppTheme.typography.subhead1
                    )
                }
            }
        }
        Text(
            text = text,
            color = ComposeAppTheme.colors.leah,
            style = ComposeAppTheme.typography.subhead2
        )
    }


}