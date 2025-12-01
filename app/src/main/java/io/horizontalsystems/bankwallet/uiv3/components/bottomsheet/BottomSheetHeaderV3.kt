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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_leah
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSIconButton

@Composable
fun BottomSheetHeaderV3(
    image72: Painter? = null,
    image120: Painter? = null,
    image400: Painter? = null,
    image400Background: Painter? = null,
    imageTint: Color = ComposeAppTheme.colors.grey,
    title: String,
    onCloseClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (image400 == null) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 12.dp)
                    .size(52.dp, 4.dp)
                    .background(ComposeAppTheme.colors.blade, RoundedCornerShape(50))
            ) { }
        }

        image72?.let {
            Icon(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .size(72.dp),
                painter = it,
                contentDescription = null,
                tint = imageTint
            )
        }

        image120?.let {
            Icon(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .size(120.dp),
                painter = it,
                contentDescription = null,
                tint = imageTint
            )
        }

        image400?.let { image ->
            Box() {
                image400Background?.let { background ->
                    Image(
                        modifier = Modifier.fillMaxWidth(),
                        painter = background,
                        contentScale = ContentScale.FillWidth,
                        contentDescription = null,
                    )
                }
                Image(
                    modifier = Modifier.fillMaxWidth(),
                    painter = image,
                    contentScale = ContentScale.FillWidth,
                    contentDescription = null,
                )
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopEnd)
                ) {
                    HSIconButton(
                        icon = painterResource(id = R.drawable.ic_close),
                        variant = ButtonVariant.Secondary,
                        size = ButtonSize.Small,
                        onClick = { onCloseClick?.invoke() }
                    )
                }
            }
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
