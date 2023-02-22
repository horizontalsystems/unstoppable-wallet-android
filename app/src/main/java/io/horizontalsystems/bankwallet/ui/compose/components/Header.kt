package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun HeaderText(
    text: String,
    onInfoClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        subhead2_grey(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = text,
            maxLines = 1
        )
        onInfoClick?.let { onClick ->
            Spacer(Modifier.weight(1f))
            Icon(
                modifier = Modifier
                    .padding(end = 24.dp)
                    .size(20.dp)
                    .clickable(
                        onClick = onClick,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ),
                painter = painterResource(R.drawable.ic_info_20),
                tint = ComposeAppTheme.colors.grey,
                contentDescription = stringResource(R.string.Info_Title),
            )
        }
    }
}

@Composable
fun HeaderSorting(
    borderTop: Boolean = false,
    borderBottom: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(ComposeAppTheme.colors.tyler)
    ) {
        if (borderTop) {
            Divider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        if (borderBottom) {
            Divider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Preview
@Composable
fun Preview_HeaderText(){
    ComposeAppTheme {
        HeaderText(
            text = "Sample Header",
            { }
        )
    }
}
