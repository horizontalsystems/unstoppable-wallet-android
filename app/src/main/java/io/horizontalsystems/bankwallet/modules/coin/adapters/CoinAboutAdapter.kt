package io.horizontalsystems.bankwallet.modules.coin.adapters

import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.TextAppearanceSpan
import android.text.style.URLSpan
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.CoinMeta
import io.horizontalsystems.bankwallet.modules.coin.AboutText
import io.horizontalsystems.views.helpers.LayoutHelper
import io.horizontalsystems.views.inflate
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.core.spans.LastLineSpacingSpan
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_about.*
import org.commonmark.node.Heading
import org.commonmark.node.Paragraph

class CoinAboutAdapter(
        viewItemLiveData: MutableLiveData<AboutText>,
        viewLifecycleOwner: LifecycleOwner
) : ListAdapter<AboutText, CoinAboutAdapter.ViewHolder>(diff) {

    init {
        viewItemLiveData.observe(viewLifecycleOwner) {
            submitList(listOf(it))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(inflate(parent, R.layout.view_holder_coin_about, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val diff = object : DiffUtil.ItemCallback<AboutText>() {
            override fun areItemsTheSame(oldItem: AboutText, newItem: AboutText): Boolean = true

            override fun areContentsTheSame(oldItem: AboutText, newItem: AboutText): Boolean {
                return oldItem == newItem
            }
        }
    }

    class ViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        init {
            aboutTextToggle.setOnClickListener {
                if (aboutText.maxLines == Integer.MAX_VALUE) {
                    aboutText.maxLines = ABOUT_MAX_LINES
                    aboutTextToggle.text = containerView.context.getString(R.string.CoinPage_ReadMore)
                } else {
                    aboutText.maxLines = Integer.MAX_VALUE
                    aboutTextToggle.text = containerView.context.getString(R.string.CoinPage_ReadLess)
                }
            }
        }

        fun bind(about: AboutText) {
            val aboutTextSpanned = getText(about)

            aboutTitle.isVisible = about.type == CoinMeta.DescriptionType.HTML
            aboutText.text = removeLinkSpans(aboutTextSpanned)
            aboutText.maxLines = Integer.MAX_VALUE
            aboutText.doOnPreDraw {
                if (aboutText.lineCount > ABOUT_MAX_LINES + ABOUT_TOGGLE_LINES) {
                    aboutText.maxLines = ABOUT_MAX_LINES
                    aboutTextToggle.isVisible = true
                } else {
                    aboutTextToggle.isVisible = false
                }
            }
        }

        private fun getText(aboutText: AboutText): Spanned {
            return when (aboutText.type) {
                CoinMeta.DescriptionType.HTML -> {
                    Html.fromHtml(aboutText.value.replace("\n", "<br />"), Html.FROM_HTML_MODE_COMPACT)
                }
                CoinMeta.DescriptionType.MARKDOWN -> {
                    val markwon = Markwon.builder(containerView.context)
                            .usePlugin(object : AbstractMarkwonPlugin() {

                                override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                                    builder.setFactory(Heading::class.java) { _, _ ->
                                        arrayOf(
                                                TextAppearanceSpan(containerView.context, R.style.Headline2),
                                                ForegroundColorSpan(containerView.context.getColor(R.color.bran))
                                        )
                                    }
                                    builder.setFactory(Paragraph::class.java) { _, _ ->
                                        arrayOf(
                                                LastLineSpacingSpan(LayoutHelper.dp(24f, containerView.context)),
                                                TextAppearanceSpan(containerView.context, R.style.Subhead2),
                                                ForegroundColorSpan(containerView.context.getColor(R.color.grey))
                                        )
                                    }
                                }
                            })
                            .build()

                    markwon.toMarkdown(aboutText.value)
                }
            }
        }

        private fun removeLinkSpans(spanned: Spanned): Spannable {
            val spannable = SpannableString(spanned)
            for (u in spannable.getSpans(0, spannable.length, URLSpan::class.java)) {
                spannable.removeSpan(u)
            }
            return spannable
        }

        companion object {
            private const val ABOUT_MAX_LINES = 8
            private const val ABOUT_TOGGLE_LINES = 2
        }

    }
}
