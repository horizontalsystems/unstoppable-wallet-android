package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.ISwapProvider
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah

class BottomSheetSwapProviderSelectDialog() : BaseComposableBottomSheetFragment() {

    var items: List<ISwapProvider>? = null
    var selectedItem: ISwapProvider? = null
    var onSelectListener: ((ISwapProvider) -> Unit)? = null

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
                    BottomSheetScreen(
                        swapProviders = items,
                        selectedItem = selectedItem,
                        onSelectListener = onSelectListener,
                        onCloseClick = { close() }
                    )
                }
            }
        }
    }

}

@Composable
private fun BottomSheetScreen(
    swapProviders: List<ISwapProvider>?,
    selectedItem: ISwapProvider?,
    onSelectListener: ((ISwapProvider) -> Unit)?,
    onCloseClick: () -> Unit
) {
    val context = LocalContext.current

    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_swap_24),
        title = stringResource(R.string.Swap_SelectSwapProvider_Title),
        onCloseClick = onCloseClick,
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob)
    ) {
        Spacer(Modifier.height(12.dp))
        swapProviders?.let { items ->
            CellUniversalLawrenceSection(items, showFrame = true) { item ->
                RowUniversal(
                    onClick = {
                        onSelectListener?.invoke(item)
                        onCloseClick.invoke()
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Image(
                        modifier = Modifier.size(32.dp),
                        painter = painterResource(
                            id = getDrawableResource(item.id, context)
                                ?: R.drawable.coin_placeholder
                        ),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    body_leah(text = item.title)
                    Spacer(modifier = Modifier.weight(1f))
                    if (item == selectedItem) {
                        Image(
                            modifier = Modifier.padding(start = 5.dp),
                            painter = painterResource(id = R.drawable.ic_checkmark_20),
                            colorFilter = ColorFilter.tint(ComposeAppTheme.colors.jacob),
                            contentDescription = null
                        )
                    }

                }
            }
        }
        Spacer(Modifier.height(44.dp))
    }
}

private fun getDrawableResource(name: String, context: Context): Int? {
    val resourceId = context.resources.getIdentifier(name, "drawable", context.packageName)
    return if (resourceId == 0) null else resourceId
}
