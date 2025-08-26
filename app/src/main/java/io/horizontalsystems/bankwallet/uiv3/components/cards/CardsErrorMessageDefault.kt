package io.horizontalsystems.bankwallet.uiv3.components.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subheadR_grey
import io.horizontalsystems.bankwallet.uiv3.components.HSPreview
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonStyle
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton

@Composable
fun CardsErrorMessageDefault(
    modifier: Modifier = Modifier,
    icon: Painter,
    iconTint: Color = LocalContentColor.current,
    title: String? = null,
    text: String? = null,
    buttonTitle: String,
    buttonTitle2: String,
    buttonTitle3: String,
    onClick: () -> Unit,
    onClick2: () -> Unit,
    onClick3: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier
                .padding(16.dp)
                .size(72.dp),
            painter = icon,
            contentDescription = null,
            tint = iconTint
        )
        title?.let {
            VSpacer(16.dp)
            headline2_leah(title)
        }
        text?.let {
            VSpacer(8.dp)
            subheadR_grey(text)
        }
        VSpacer(24.dp)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HSButton(
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Primary,
                style = ButtonStyle.Solid,
                title = buttonTitle,
                onClick = onClick
            )
            HSButton(
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Secondary,
                style = ButtonStyle.Solid,
                title = buttonTitle2,
                onClick = onClick2
            )
            HSButton(
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Secondary,
                style = ButtonStyle.Transparent,
                title = buttonTitle3,
                onClick = onClick3
            )
        }
    }
}



@Preview
@Composable
fun Preview_CardsErrorMessageDefault() {
    HSPreview {
        CardsErrorMessageDefault(
            icon = painterResource(R.drawable.ic_warning_filled_24),
            title = "Title",
            text = "Please wait for the sync to finish.",
            buttonTitle = "Button1",
            buttonTitle2 = "Button2",
            buttonTitle3 = "Button3",
            onClick = { },
            onClick2 = { },
            onClick3 = { },
        )
    }
}