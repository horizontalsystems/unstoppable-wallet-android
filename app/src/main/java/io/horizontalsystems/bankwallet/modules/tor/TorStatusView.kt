package io.horizontalsystems.bankwallet.modules.tor

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryTransparent
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay

@Composable
fun TorStatusView(
    viewModel: TorConnectionViewModel = viewModel(factory = TorConnectionModule.Factory())
) {

    val animatedSize by animateDpAsState(
        targetValue = if (viewModel.torViewState.torIsActive) 20.dp else 50.dp,
        animationSpec = tween(durationMillis = 250, easing = LinearOutSlowInEasing)
    )

    Divider(
        thickness = 1.dp,
        color = ComposeAppTheme.colors.steel10,
        modifier = Modifier.fillMaxWidth()
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(animatedSize),
        contentAlignment = Alignment.Center
    ) {
        if (viewModel.torViewState.torIsActive) {
            val startColor = ComposeAppTheme.colors.remus
            val endColor = ComposeAppTheme.colors.lawrence
            val color = remember { Animatable(startColor) }
            val startTextColor = ComposeAppTheme.colors.white
            val endTextColor = ComposeAppTheme.colors.leah
            val textColor = remember { Animatable(startTextColor) }
            LaunchedEffect(Unit) {
                delay(1000)
                color.animateTo(endColor, animationSpec = tween(250, easing = LinearEasing))
                textColor.animateTo(endTextColor, animationSpec = tween(250, easing = LinearEasing))
            }
            Box(
                modifier = Modifier.fillMaxWidth()
                    .fillMaxSize()
                    .background(color.value),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.Tor_TorIsActive),
                    style = ComposeAppTheme.typography.micro,
                    color = textColor.value,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ComposeAppTheme.colors.lawrence)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(viewModel.torViewState.stateText),
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                    style = ComposeAppTheme.typography.subhead2,
                    color = if (viewModel.torViewState.showRetryButton) ComposeAppTheme.colors.lucian else ComposeAppTheme.colors.leah,
                )
                if (viewModel.torViewState.showRetryButton) {
                    ButtonSecondaryTransparent(
                        title = stringResource(R.string.Button_Retry).toUpperCase(Locale.current),
                        onClick = { viewModel.restartTor() }
                    )
                }
            }
        }
    }

    if (viewModel.torViewState.showNetworkConnectionError) {
        val view = LocalView.current
        HudHelper.showErrorMessage(view, R.string.Hud_Text_NoInternet)
        viewModel.networkErrorShown()
    }
}
