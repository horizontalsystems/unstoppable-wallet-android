package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import kotlin.math.ceil
import kotlin.math.min

enum class SectionItemPosition {
    First, Last, Middle, Single
}

fun Modifier.sectionItemBorder(
    width: Dp,
    color: Color,
    cornerRadius: Dp,
    position: SectionItemPosition
) = then(
    drawWithCache {
        val brush = SolidColor(color)
        val cornerRadiusPx = cornerRadius.toPx()
        val strokeWidthPx = min(
            if (width == Dp.Hairline) 1f else ceil(width.toPx()),
            ceil(size.minDimension / 2)
        )

        val arcSize = Size(cornerRadiusPx * 2, cornerRadiusPx * 2)

        onDrawWithContent {
            when (position) {
                SectionItemPosition.First -> {
                    drawFirstItemBorder(strokeWidthPx, brush, arcSize)
                }
                SectionItemPosition.Last -> {
                    drawLastItemBorder(strokeWidthPx, brush, arcSize)
                }
                SectionItemPosition.Middle -> {
                    drawMiddleItemBorder(strokeWidthPx, brush)
                }
                SectionItemPosition.Single -> { }
            }
            drawContent()
        }
    }
)

private fun ContentDrawScope.drawMiddleItemBorder(
    strokeWidthPx: Float,
    brush: Brush
) {
    val halfStroke = strokeWidthPx / 2

    drawLine(
        brush = brush,
        start = Offset(halfStroke, 0f),
        end = Offset(halfStroke, size.height),
        strokeWidth = strokeWidthPx,
    )
    drawLine(
        brush = brush,
        start = Offset(size.width - halfStroke, 0f),
        end = Offset(size.width - halfStroke, size.height),
        strokeWidth = strokeWidthPx,
    )
}

private fun ContentDrawScope.drawLastItemBorder(
    strokeWidthPx: Float,
    brush: Brush,
    arcSize: Size
) {
    val halfStroke = strokeWidthPx / 2
    drawArc(
        brush = brush,
        startAngle = 90f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(halfStroke, size.height - arcSize.height - halfStroke),
        size = arcSize,
        style = Stroke(strokeWidthPx),
    )
    drawArc(
        brush = brush,
        startAngle = 0f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(size.width - arcSize.width - halfStroke, size.height - arcSize.height - halfStroke),
        size = arcSize,
        style = Stroke(strokeWidthPx),
    )
    drawLine(
        brush = brush,
        start = Offset(arcSize.width / 2, size.height - halfStroke),
        end = Offset(size.width - arcSize.width / 2, size.height - halfStroke),
        strokeWidth = strokeWidthPx,
    )
    drawLine(
        brush = brush,
        start = Offset(halfStroke, 0f),
        end = Offset(halfStroke, size.height - arcSize.height / 2),
        strokeWidth = strokeWidthPx,
    )
    drawLine(
        brush = brush,
        start = Offset(size.width - halfStroke, 0f),
        end = Offset(size.width - halfStroke, size.height - arcSize.height / 2),
        strokeWidth = strokeWidthPx,
    )
}

private fun ContentDrawScope.drawFirstItemBorder(
    strokeWidthPx: Float,
    brush: Brush,
    arcSize: Size
) {
    val halfStroke = strokeWidthPx / 2

    drawArc(
        brush = brush,
        startAngle = 180f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(halfStroke, halfStroke),
        size = arcSize,
        style = Stroke(strokeWidthPx),
    )
    drawArc(
        brush = brush,
        startAngle = -90f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(size.width - arcSize.width - halfStroke, halfStroke),
        size = arcSize,
        style = Stroke(strokeWidthPx),
    )
    drawLine(
        brush = brush,
        start = Offset(arcSize.width / 2, halfStroke),
        end = Offset(size.width - arcSize.width / 2, halfStroke),
        strokeWidth = strokeWidthPx,
    )
    drawLine(
        brush = brush,
        start = Offset(halfStroke, arcSize.height / 2),
        end = Offset(halfStroke, size.height),
        strokeWidth = strokeWidthPx,
    )
    drawLine(
        brush = brush,
        start = Offset(size.width - halfStroke, arcSize.height / 2),
        end = Offset(size.width - halfStroke, size.height),
        strokeWidth = strokeWidthPx,
    )
}