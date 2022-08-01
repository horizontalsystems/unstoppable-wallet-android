package io.horizontalsystems.bankwallet.modules.markdown

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.core.findNavController

class MarkdownFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                ComposeAppTheme {
                    MarkdownScreen(
                        handleRelativeUrl = arguments?.getBoolean(handleRelativeUrlKey) ?: false,
                        markdownUrl = arguments?.getString(markdownUrlKey) ?: "",
                        onCloseClick = { findNavController().popBackStack() },
                        onUrlClick = { url ->
                            findNavController().slideFromRight(
                                R.id.markdownFragment, bundleOf(markdownUrlKey to url)
                            )
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val markdownUrlKey = "urlKey"
        const val handleRelativeUrlKey = "handleRelativeUrlKey"
    }
}

@Composable
private fun MarkdownScreen(
    handleRelativeUrl: Boolean,
    markdownUrl: String,
    onCloseClick: () -> Unit,
    onUrlClick: (String) -> Unit,
    viewModel: MarkdownViewModel = viewModel(factory = MarkdownModule.Factory(markdownUrl))
) {

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                navigationIcon = {
                    HsIconButton(onClick = onCloseClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back button",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                }
            )

            MarkdownContent(
                viewState = viewModel.viewState,
                markdownBlocks = viewModel.markdownBlocks,
                handleRelativeUrl = handleRelativeUrl,
                onRetryClick = { viewModel.retry() },
                onUrlClick = onUrlClick
            )

        }
    }
}
