package io.horizontalsystems.bankwallet.modules.settings.main.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.components.SliderIndicator
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import kotlinx.coroutines.delay

@Composable
fun BannerCarousel(
    modifier: Modifier = Modifier,
    banners: List<@Composable () -> Unit>,
    autoScrollDurationMillis: Long = 5000, // Auto-scroll every 5 seconds
    enableAutoScroll: Boolean = true
) {
    if (banners.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { banners.size })

    if (enableAutoScroll && banners.size > 1) {
        LaunchedEffect(key1 = pagerState) {
            while (true) {
                delay(autoScrollDurationMillis)
                val nextPage = (pagerState.currentPage + 1) % banners.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            banners[page]()
        }

        if (banners.size > 1) {
            SliderIndicator(
                total = banners.size,
                current = pagerState.currentPage
            )
        } else {
            VSpacer(20.dp)
        }
    }
}