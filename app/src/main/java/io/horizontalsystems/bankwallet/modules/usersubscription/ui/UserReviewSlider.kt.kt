package io.horizontalsystems.bankwallet.modules.usersubscription.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.SliderIndicator
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.caption_leah
import io.horizontalsystems.bankwallet.ui.compose.components.micro_grey

@Composable
fun ReviewSlider(
    reviews: List<Review>,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { reviews.size })

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ComposeAppTheme.colors.steel10),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VSpacer(24.dp)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val review = reviews[page]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(5) {
                        Icon(
                            painterResource(R.drawable.ic_star_filled_20),
                            contentDescription = "Star",
                            modifier = Modifier.size(16.dp),
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                }

                caption_leah(
                    text = "\"${review.content}\"",
                    modifier = Modifier.padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center
                )

                VSpacer(12.dp)
                Image(
                    painter = painterResource(id = review.authorImageRes),
                    contentDescription = "Author image",
                    modifier = Modifier.size(32.dp),
                    contentScale = ContentScale.Crop
                )
                VSpacer(6.dp)

                micro_grey(
                    text = review.authorName,
                    modifier = Modifier.padding(horizontal =24.dp)
                )
                micro_grey(
                    text = review.authorTitle,
                    modifier = Modifier.padding(horizontal =24.dp)
                )
            }
        }

        VSpacer(8.dp)
        SliderIndicator(
            total = reviews.size,
            current = pagerState.currentPage
        )
        VSpacer(12.dp)
    }
}

data class Review(
    val content: String,
    val authorName: String,
    val authorTitle: String,
    val authorImageRes: Int
)

@Composable
fun ReviewSliderBlock() {
    val reviews = listOf(
        Review(
            content = "Unstoppable is the first multi-coin wallet that supports Bitcoin, Ethereum, and fully shielded Zcash, as well as other coins, and it has a strong, user-centric architecture in which the users own their own keys and their own privacy.",
            authorName = "Zooko Wilcox-O'Hearn",
            authorTitle = "CEO of the ECC",
            authorImageRes = R.drawable.zoko_avatar
        ),
        Review(
            content = "Unstoppable is the first multi-coin wallet that supports Bitcoin, Ethereum, and fully shielded Zcash, as well as other coins, and it has a strong, user-centric architecture in which the users own their own keys and their own privacy.",
            authorName = "Zooko 2 Wilcox-O'Hearn",
            authorTitle = "CEO of the ECC",
            authorImageRes = R.drawable.zoko_avatar
        ),
        Review(
            content = "Unstoppable is the first multi-coin wallet that supports Bitcoin, Ethereum, and fully shielded Zcash, as well as other coins, and it has a strong, user-centric architecture in which the users own their own keys and their own privacy.",
            authorName = "Zooko 3 Wilcox-O'Hearn",
            authorTitle = "CEO of the ECC",
            authorImageRes = R.drawable.zoko_avatar
        ),
    )

    ReviewSlider(reviews = reviews)

}