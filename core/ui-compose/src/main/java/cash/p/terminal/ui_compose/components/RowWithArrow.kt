package cash.p.terminal.ui_compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import cash.p.terminal.ui_compose.R
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun RowWithArrow(
    text: String,
    modifier: Modifier = Modifier,
    showAlert: Boolean = false,
    onClick: (() -> Unit)
) {
    RowUniversal(
        modifier = modifier.padding(horizontal = 16.dp),
        onClick = onClick
    ) {
        body_leah(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (showAlert) {
            Image(
                modifier = Modifier.padding(horizontal = 8.dp).size(20.dp),
                painter = painterResource(id = R.drawable.ic_attention_red_20),
                contentDescription = null,
            )
        }
        Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun RowWithArrowPreview() {
    ComposeAppTheme {
        RowWithArrow(
            text = LoremIpsum(20).values.joinToString(),
            showAlert = true,
            onClick = {}
        )
    }
}