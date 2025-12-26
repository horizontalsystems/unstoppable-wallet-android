package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun HeaderStick(
    borderTop: Boolean = false,
    borderBottom: Boolean = false,
    text: String,
    color: Color = ComposeAppTheme.colors.tyler,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .background(color)
    ) {
        if (borderTop) {
            HsDivider(modifier = Modifier.align(Alignment.TopCenter))
        }

        subheadSB_andy(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 12.dp),
            text = text,
            maxLines = 1,
        )

        if (borderBottom) {
            HsDivider(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

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
    backgroundColor: Color = ComposeAppTheme.colors.tyler,
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(backgroundColor)
    ) {
        if (borderTop) {
            HsDivider(modifier = Modifier.align(Alignment.TopCenter))
        }

        if (borderBottom) {
            HsDivider(modifier = Modifier.align(Alignment.BottomCenter))
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun PremiumHeader(
    @StringRes  title: Int = R.string.Premium_Title,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .height(32.dp)
            .padding(top = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier
                .padding(end = 10.dp)
                .size(16.dp),
            painter = painterResource(R.drawable.star_filled_yellow_16),
            tint = ComposeAppTheme.colors.jacob,
            contentDescription = null,
        )
        subhead1_jacob(
            text = stringResource(title),
            maxLines = 1
        )
    }
}

@Preview
@Composable
fun Preview_HeaderText() {
    ComposeAppTheme {
        HeaderText(
            text = "Sample Header",
            { }
        )
    }
}
