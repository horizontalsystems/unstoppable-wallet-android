package io.horizontalsystems.bankwallet.modules.intro

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.title3_leah
import kotlinx.coroutines.launch

class IntroActivity : BaseActivity() {

    val viewModel by viewModels<IntroViewModel> { IntroModule.Factory() }

    private val nightMode by lazy {
        val uiMode =
            App.instance.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)
        uiMode == Configuration.UI_MODE_NIGHT_YES
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            IntroScreen(viewModel, nightMode) { finish() }
        }
        setStatusBarTransparent()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, IntroActivity::class.java)
            context.startActivity(intent)
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IntroScreen(viewModel: IntroViewModel, nightMode: Boolean, closeActivity: () -> Unit) {
    val pagerState = rememberPagerState(initialPage = 0)
    ComposeAppTheme {
        Box {
            Image(
                painter = painterResource(if (nightMode) R.drawable.ic_intro_background else R.drawable.ic_intro_background_light),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        val pageCount = 3
        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            pageCount = pageCount,
            state = pagerState,
            verticalAlignment = Alignment.Top,
        ) { index ->
            SlidingContent(viewModel.slides[index], nightMode)
        }

        StaticContent(viewModel, pagerState, closeActivity, pageCount)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StaticContent(
    viewModel: IntroViewModel,
    pagerState: PagerState,
    closeActivity: () -> Unit,
    pageCount: Int
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(2f))
        Spacer(Modifier.height(326.dp))
        Spacer(Modifier.weight(1f))
        SliderIndicator(viewModel.slides, pagerState.currentPage)
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
                    MainModule.start(context)
                    closeActivity()

                }
            })
        Spacer(Modifier.height(60.dp))
    }
}

@Composable
private fun SliderIndicator(slides: List<IntroModule.IntroSliderData>, currentPage: Int) {
    Row(
        modifier = Modifier.height(30.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        slides.forEachIndexed { index, _ ->
            SliderCell(index == currentPage)
        }
    }
}

@Composable
private fun SliderCell(highlighted: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(if (highlighted) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.steel20)
            .size(width = 20.dp, height = 4.dp),
    )
}

@Composable
private fun SlidingContent(
    slideData: IntroModule.IntroSliderData,
    nightMode: Boolean
) {
    Column {
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
