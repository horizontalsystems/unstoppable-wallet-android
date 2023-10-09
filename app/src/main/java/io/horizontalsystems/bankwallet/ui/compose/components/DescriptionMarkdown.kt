package io.horizontalsystems.bankwallet.ui.compose.components

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.TextAppearanceSpan
import android.text.style.URLSpan
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.helpers.LayoutHelper
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.RenderProps
import io.noties.markwon.core.CoreProps
import io.noties.markwon.core.spans.LastLineSpacingSpan
import org.commonmark.node.Heading
import org.commonmark.node.Paragraph

@Composable
fun DescriptionMarkdown(
    text: String,
) {
    val context = LocalContext.current
    val markdownRender = remember { buildMarkwon(context) }
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        factory = {
            TextView(it)
        },
        update = { aboutText ->
            val aboutTextSpanned = markdownRender.toMarkdown(text)
            aboutText.text = removeLinkSpans(aboutTextSpanned)
        }
    )
}

private fun buildMarkwon(context: Context): Markwon {
    return Markwon.builder(context)
        .usePlugin(object : AbstractMarkwonPlugin() {

            override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                builder.setFactory(Heading::class.java) { _, props: RenderProps ->
                    val level = CoreProps.HEADING_LEVEL.require(props)
                    if (level == 1) {
                        arrayOf(
                            TextAppearanceSpan(context, R.style.Title3NoColor),
                            ForegroundColorSpan(context.getColor(R.color.leah))
                        )
                    } else {
                        arrayOf(
                            TextAppearanceSpan(context, R.style.Headline2),
                            ForegroundColorSpan(context.getColor(R.color.leah))
                        )
                    }
                }
                builder.setFactory(Paragraph::class.java) { _, _ ->
                    arrayOf(
                        LastLineSpacingSpan(LayoutHelper.dp(24f, context)),
                        TextAppearanceSpan(context, R.style.Subhead2),
                        ForegroundColorSpan(context.getColor(R.color.grey))
                    )
                }
            }
        })
        .build()
}

private fun removeLinkSpans(spanned: Spanned): Spannable {
    val spannable = SpannableString(spanned)
    for (u in spannable.getSpans(0, spannable.length, URLSpan::class.java)) {
        spannable.removeSpan(u)
    }
    return spannable
}
