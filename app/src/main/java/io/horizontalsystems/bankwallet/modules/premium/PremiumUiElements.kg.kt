package io.horizontalsystems.bankwallet.modules.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Darkest
import io.horizontalsystems.bankwallet.ui.compose.SteelLight
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefaults
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondary
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.SecondaryButtonDefaults.buttonColors

@Composable
fun highlightText(
    text: String,
    highlightPart: String,
    color: Color
): AnnotatedString {

    return buildAnnotatedString {
        val highlightIndex = text
            .lowercase()
            .indexOf(highlightPart.lowercase())

        if (highlightIndex != -1) {
            append(text.substring(0, highlightIndex))

            withStyle(
                SpanStyle(color = color)
            ) {
                append(
                    text.substring(
                        highlightIndex,
                        highlightIndex + highlightPart.length
                    )
                )
            }

            append(
                text.substring(highlightIndex + highlightPart.length)
            )
        } else {
            append(text)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ButtonPrimaryCustomColor(
    modifier: Modifier = Modifier,
    title: String,
    brush: Brush,
    onClick: () -> Unit,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonPrimaryDefaults.ContentPadding,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(25.dp))
            .background(brush),
        shape = RoundedCornerShape(25.dp),
        color = Color.Transparent,
        contentColor = ComposeAppTheme.colors.dark,
        onClick = onClick,
        enabled = enabled,
    ) {
        ProvideTextStyle(
            value = ComposeAppTheme.typography.headline2
        ) {
            Row(
                Modifier
                    .defaultMinSize(
                        minWidth = ButtonPrimaryDefaults.MinWidth,
                        minHeight = ButtonPrimaryDefaults.MinHeight
                    )
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun TitleCenteredTopBar(
    title: String,
    color: Color = SteelLight,
    modifier: Modifier = Modifier,
    onCloseClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .fillMaxWidth(),
    ) {
        Text(
            text = title,
            color = color,
            style = ComposeAppTheme.typography.headline1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.Center)
        )
        HsIconButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            onClick = onCloseClick
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = "close button",
                tint = ComposeAppTheme.colors.jacob,
            )
        }
    }
}

@Composable
fun ColoredTextSecondaryButton(
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ButtonSecondary(
        modifier = modifier,
        onClick = onClick,
        buttonColors = buttonColors(
            backgroundColor = ComposeAppTheme.colors.transparent,
            contentColor = color,
            disabledBackgroundColor = ComposeAppTheme.colors.transparent,
            disabledContentColor = ComposeAppTheme.colors.grey50,
        ),
        content = {
            Text(
                text = title,
                maxLines = 1,
                style = ComposeAppTheme.typography.body,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
fun ButtonsGroupWithDarkShade(
    buttonsContent: @Composable (() -> Unit)
) {
    Column(
        modifier = Modifier.offset(y = -(24.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            ComposeAppTheme.colors.transparent,
                            Darkest
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .background(Darkest)
                .padding(bottom = 8.dp)
        ) {
            buttonsContent()
        }
    }
}