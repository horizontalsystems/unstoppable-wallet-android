package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun SemiCircleChart(
    modifier: Modifier = Modifier,
    percentValues: List<Float>,
    title: String
) {
    val paintColor = ComposeAppTheme.colors.yellowD
    var startAngle = 180F
    val colorParts = 255 / percentValues.size

    val proportions = percentValues.mapIndexed { index, item ->
        val sweepAngle = item / 100 * 180F
        val colorAlpha = (255 - index * colorParts) / 255f
        val proportion = Triple(startAngle, sweepAngle, paintColor.copy(alpha = colorAlpha))

        startAngle += sweepAngle
        proportion
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2f)
    ) {
        Canvas(
            modifier = Modifier.fillMaxWidth()
        ) {
            val strokeWidth = size.width * 0.2f // 20% of total width of chart
            val diameter = size.width - strokeWidth

            proportions.forEach { (startAngle, sweepAngle, color) ->
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(x = strokeWidth / 2f, y = strokeWidth / 2f),
                    style = Stroke(width = strokeWidth),
                    size = Size(diameter, diameter)
                )
            }
        }

        title3_jacob(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            text = title,
        )
    }
}

@Preview
@Composable
fun SemiCircleChartPreview() {
    ComposeAppTheme {
        Column(modifier = Modifier.padding(top = 24.dp)) {
            SemiCircleChart(
                Modifier.padding(horizontal = 32.dp),
                listOf(33f, 33f, 34f),
                "38.95%"
            )

            subhead1_grey(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 32.dp, top = 12.dp),
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                text = "Топ-10 кошельков Ethereum",
            )

            subhead1_grey(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 38.dp),
                textAlign = TextAlign.Start,
                overflow = TextOverflow.Ellipsis,
                text = "Chart above shows the % of all tokens in circulation held by top 10 wallets. The lower the % the more distributed the coin is.",
            )
        }
    }
}
