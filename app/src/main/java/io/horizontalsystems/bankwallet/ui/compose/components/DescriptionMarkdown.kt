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
import androidx.core.view.doOnPreDraw
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.helpers.LayoutHelper
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.core.spans.LastLineSpacingSpan
import org.commonmark.node.Heading
import org.commonmark.node.Paragraph

@Composable
fun DescriptionMarkdown(
    textMaxLines: Int,
    toggleLines: Int,
    text: String,
    expanded: Boolean,
    f: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val markdownRender = remember { buildMarkwon(context) }
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        factory = {
            TextView(it).also { aboutText ->
                aboutText.doOnPreDraw {
                    val overflow = aboutText.lineCount > textMaxLines + toggleLines

                    aboutText.maxLines = if (overflow) textMaxLines else Integer.MAX_VALUE
                    f.invoke(overflow)
                }
            }
        },
        update = { aboutText ->
            val aboutTextSpanned = markdownRender.toMarkdown(text)
            aboutText.text = removeLinkSpans(aboutTextSpanned)
            aboutText.maxLines = if (expanded) Integer.MAX_VALUE else textMaxLines
        }
    )
}

private fun buildMarkwon(context: Context): Markwon {
    return Markwon.builder(context)
        .usePlugin(object : AbstractMarkwonPlugin() {

            override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                builder.setFactory(Heading::class.java) { _, _ ->
                    arrayOf(
                        TextAppearanceSpan(context, R.style.Headline2),
                        ForegroundColorSpan(context.getColor(R.color.bran))
                    )
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
