package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CellNews(
    source: String,
    title: String,
    body: String,
    date: String,
    onClick: () -> Unit
) {
    var titleLines by remember { mutableStateOf(0) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = 0.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = ComposeAppTheme.colors.lawrence,
        onClick = { onClick.invoke() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = source,
                color = ComposeAppTheme.colors.grey,
                style = ComposeAppTheme.typography.captionSB,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = ComposeAppTheme.colors.leah,
                style = ComposeAppTheme.typography.headline2,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { res -> titleLines = res.lineCount }
            )
            if (titleLines < 3) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = body,
                    color = ComposeAppTheme.colors.grey,
                    style = ComposeAppTheme.typography.subhead2,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = if (titleLines == 1) 2 else 1,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = date,
                color = ComposeAppTheme.colors.grey50,
                style = ComposeAppTheme.typography.micro,
                maxLines = 1,
            )
        }
    }
}
