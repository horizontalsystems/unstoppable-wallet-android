package io.horizontalsystems.bankwallet.uiv3.components.bottomsheet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_leah

@Composable
fun BottomSheetHeaderV3(
    image72: Painter? = null,
    image120: Painter? = null,
    image400: Painter? = null,
    title: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 12.dp)
                .size(52.dp, 4.dp)
                .background(ComposeAppTheme.colors.blade, RoundedCornerShape(50))
        ) {  }

        image72?.let {
            Icon(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .size(72.dp),
                painter = it,
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
        }

        image120?.let {
            Icon(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .size(120.dp),
                painter = it,
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
        }

        image400?.let {
            Image(
                painter = it,
                contentDescription = null,
            )
        }

        headline1_leah(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 8.dp),
            text = title
        )
    }
}

@Preview
@Composable
fun Preview_BottomSheetHeaderV3() {
    ComposeAppTheme {
        BottomSheetHeaderV3(painterResource(R.drawable.warning_filled_24), title = "Confirm")
    }
}
