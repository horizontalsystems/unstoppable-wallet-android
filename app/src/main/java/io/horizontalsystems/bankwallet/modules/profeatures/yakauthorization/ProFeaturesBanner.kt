package io.horizontalsystems.bankwallet.modules.profeatures.yakauthorization

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun ProFeaturesBanner(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable {
                onClick.invoke()
            }
    ) {
        Image(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            painter = painterResource(R.drawable.ic_pro_nft_banner),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )

        Column {
            Text(
                modifier = Modifier.padding(12.dp),
                text = title,
                color = Color.Black,
                style = ComposeAppTheme.typography.headline2,
                maxLines = 1
            )

            Text(
                modifier = Modifier.padding(12.dp),
                text = description,
                color = Color.Black,
                style = ComposeAppTheme.typography.caption
            )
        }
    }

}
