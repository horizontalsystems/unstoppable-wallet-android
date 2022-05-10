package io.horizontalsystems.bankwallet.modules.profeatures.yakauthorization

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun ProFeaturesBanner(
    title: String,
    description: String,
    borderTop: Boolean,
    onClick: () -> Unit
) {
    if (borderTop) {
        Spacer(modifier = Modifier.height(24.dp))
    }

    Box(
        modifier = Modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Yellow)
            .fillMaxWidth()
            .clickable {
                onClick.invoke()
            }
    ) {
        Column {
            Text(
                modifier = Modifier.padding(12.dp),
                text = title,
                color = Color.Black,
                style = ComposeAppTheme.typography.headline2,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(11.dp))

            Text(
                modifier = Modifier.padding(12.dp),
                text = description,
                color = Color.Black,
                style = ComposeAppTheme.typography.caption
            )
        }
    }

}
