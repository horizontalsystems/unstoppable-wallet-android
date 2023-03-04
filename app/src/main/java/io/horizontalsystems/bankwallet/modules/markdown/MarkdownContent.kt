package io.horizontalsystems.bankwallet.modules.markdown

import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView

@Composable
fun MarkdownContent(
    modifier: Modifier = Modifier,
    viewState: ViewState? = null,
    markdownBlocks: List<MarkdownBlock>,
    handleRelativeUrl: Boolean = false,
    onRetryClick: () -> Unit,
    onUrlClick: (String) -> Unit,
) {

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Crossfade(viewState) { viewState ->
            when (viewState) {
                is ViewState.Error -> {
                    ListErrorView(stringResource(id = R.string.Markdown_Error_NotFound)) {
                        onRetryClick()
                    }
                }
                ViewState.Loading -> {
                    Loading()
                }
                ViewState.Success -> {
                    AndroidView(
                        modifier = Modifier.weight(1f),
                        factory = { context ->
                            RecyclerView(context).apply {
                                layoutParams =
                                    ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT
                                    )
                                layoutManager = LinearLayoutManager(context)
                                adapter = MarkdownContentAdapter(
                                    handleRelativeUrl = handleRelativeUrl,
                                    listener = object : MarkdownContentAdapter.Listener {
                                        override fun onClick(url: String) {
                                            onUrlClick(url)
                                        }
                                    }).also { it.submitList(markdownBlocks) }
                            }
                        },
                        update = { recyclerview ->
                            recyclerview.apply {
                                adapter = MarkdownContentAdapter(
                                    handleRelativeUrl = handleRelativeUrl,
                                    listener = object : MarkdownContentAdapter.Listener {
                                        override fun onClick(url: String) {
                                            onUrlClick(url)
                                        }
                                    }).also { it.submitList(markdownBlocks) }
                            }
                        }
                    )
                }
                null -> {}
            }
        }
    }
}
