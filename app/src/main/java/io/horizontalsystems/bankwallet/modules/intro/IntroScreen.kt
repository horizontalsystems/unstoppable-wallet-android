package io.horizontalsystems.bankwallet.modules.intro

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.RadialBackground
import io.horizontalsystems.bankwallet.ui.compose.components.SliderIndicator
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.title3_leah
import kotlinx.coroutines.launch

@Composable
fun IntroScreen() {
    val viewModel = hiltViewModel<IntroViewModel>()

    val pageCount = 3
    val pagerState = rememberPagerState(initialPage = 0) { pageCount }
    ComposeAppTheme {
        RadialBackground()
        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            state = pagerState,
            verticalAlignment = Alignment.Top,
        ) { index ->
            SlidingContent(viewModel.slides[index])
        }

        StaticContent(viewModel, pagerState, pageCount)
    }
}

@Composable
private fun StaticContent(viewModel: IntroViewModel, pagerState: PagerState, pageCount: Int) {
    val coroutineScope = rememberCoroutineScope()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(2f))
        Spacer(Modifier.height(326.dp))
        Spacer(Modifier.weight(1f))
        SliderIndicator(
            total = pageCount,
            current = pagerState.currentPage
        )
        Spacer(Modifier.weight(1f))
        //Text
        Column(
            modifier = Modifier
                .height(120.dp)
                .fillMaxWidth(),
        ) {
            val title = viewModel.slides[pagerState.currentPage].title
            Crossfade(targetState = title) { titleRes ->
                title3_leah(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    text = stringResource(titleRes),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(16.dp))
            val subtitle = viewModel.slides[pagerState.currentPage].subtitle
            Crossfade(targetState = subtitle) { subtitleRes ->
                body_grey(
                    text = stringResource(subtitleRes),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.weight(2f))
        ButtonPrimaryYellow(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            title = stringResource(R.string.Button_Next),
            onClick = {
                if (pagerState.currentPage + 1 < pageCount) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                } else {
                    viewModel.onStartClicked()
                }
            })
        Spacer(Modifier.height(60.dp))
    }
}

@Composable
private fun SlidingContent(slideData: IntroModule.IntroSliderData) {
    val nightMode = isSystemInDarkTheme()
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(2f))
        Image(
            modifier = Modifier.size(width = 326.dp, height = 326.dp),
            painter = painterResource(if (nightMode) slideData.imageDark else slideData.imageLight),
            contentDescription = null,
        )
        Spacer(Modifier.weight(1f))
        //switcher
        Spacer(Modifier.height(30.dp))
        Spacer(Modifier.weight(1f))
        //Text
        Spacer(Modifier.height(120.dp))
        Spacer(Modifier.weight(2f))
        Spacer(Modifier.height(110.dp))
    }
}
