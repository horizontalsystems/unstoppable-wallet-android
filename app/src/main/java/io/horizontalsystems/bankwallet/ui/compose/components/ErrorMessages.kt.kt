package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun ErrorScreenWithAction(
    text: String,
    @DrawableRes icon: Int,
    paddingValues: PaddingValues = PaddingValues(),
    actionsComposable: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(64.dp),
                    painter = painterResource(icon),
                    contentDescription = text,
                    tint = ComposeAppTheme.colors.grey
                )
            }
            VSpacer(16.dp)
            subhead_grey(
                modifier = Modifier.padding(horizontal = 48.dp),
                text = text,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
            )
            actionsComposable?.let { composable ->
                VSpacer(24.dp)
                composable.invoke()
            }
            VSpacer(100.dp) //to move the content up a bit by design
        }
    }
}