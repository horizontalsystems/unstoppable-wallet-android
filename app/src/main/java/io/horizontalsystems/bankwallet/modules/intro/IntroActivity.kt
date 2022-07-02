package io.horizontalsystems.bankwallet.modules.intro

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.title3_leah

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

    private fun setStatusBarTransparent() {
        if (Build.VERSION.SDK_INT in 26..29) {
            window.statusBarColor = Color.TRANSPARENT
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.decorView.systemUiVisibility =
                SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or SYSTEM_UI_FLAG_LAYOUT_STABLE

        } else if (Build.VERSION.SDK_INT >= 30) {
            window.statusBarColor = Color.TRANSPARENT
            // Making status bar overlaps with the activity
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, IntroActivity::class.java)
            context.startActivity(intent)
        }
    }

}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun IntroScreen(viewModel: IntroViewModel, nightMode: Boolean, closeActivity: () -> Unit) {
    val pagerState = rememberPagerState(initialPage = 0)
    ComposeAppTheme {
        Box() {
            Image(
                painter = painterResource(if (nightMode) R.drawable.ic_intro_background else R.drawable.ic_intro_background_light),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            count = 3,
            state = pagerState,
            verticalAlignment = Alignment.Top,
        ) { index ->
            SlidingContent(viewModel.slides[index], nightMode)
        }

        StaticContent(viewModel, pagerState, closeActivity)
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun StaticContent(
    viewModel: IntroViewModel,
    pagerState: PagerState,
    closeActivity: () -> Unit
) {
    val context = LocalContext.current
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            title3_leah(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = stringResource(viewModel.slides[pagerState.currentPage].title),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            body_grey(
                text = stringResource(viewModel.slides[pagerState.currentPage].subtitle),
                modifier = Modifier.padding(horizontal = 48.dp),
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.weight(2f))
        ButtonPrimaryYellow(
            modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
            title = stringResource(R.string.Intro_Wallet_Screen1Description),
            onClick = {
                viewModel.onStartClicked()
                MainModule.start(context)
                closeActivity()
            }
        )
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
