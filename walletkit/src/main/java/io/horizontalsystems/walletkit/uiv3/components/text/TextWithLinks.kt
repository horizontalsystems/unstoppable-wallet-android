package io.horizontalsystems.walletkit.uiv3.components.text

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink

/**
 * [sentence] with each name in [links] turned into a link that calls its
 * action. The names are looked up in the sentence, so a translation is free
 * to put them in any order; a name the sentence does not hold stays plain.
 */
fun textWithLinks(
    sentence: String,
    links: List<Pair<String, () -> Unit>>,
    styles: TextLinkStyles,
): AnnotatedString {
    val found = links
        .map { (name, onClick) -> Triple(sentence.indexOf(name), name, onClick) }
        .filter { (start, _, _) -> start >= 0 }
        .sortedBy { (start, _, _) -> start }

    return buildAnnotatedString {
        var position = 0
        found.forEach { (start, name, onClick) ->
            if (start < position) return@forEach
            append(sentence.substring(position, start))
            withLink(LinkAnnotation.Clickable(tag = name, styles = styles) { onClick() }) {
                append(name)
            }
            position = start + name.length
        }
        append(sentence.substring(position))
    }
}
