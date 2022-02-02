package io.horizontalsystems.bankwallet.ui.extensions

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellMultilineLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning

class BottomSheetSelectorDialog(
    private val title: String,
    private val subtitle: String,
    @DrawableRes private val icon: Int,
    private val items: List<BottomSheetSelectorViewItem>,
    private val selectedIndex: Int,
    private val onItemSelected: (Int) -> Unit,
    private val onCancelled: (() -> Unit)?,
    private val warning: String?,
    private val notifyUnchanged: Boolean
) : BaseComposableBottomSheetFragment() {

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
                    BottomSheetScreen()
                }
            }
        }
    }

    @Composable
    private fun BottomSheetScreen() {
        var selected by remember { mutableStateOf(selectedIndex) }

        BottomSheetHeader(
            iconPainter = painterResource(icon),
            title = title,
            subtitle = subtitle,
            onCloseClick = { close() }
        ) {
            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10
            )
            warning?.let {
                TextImportantWarning(
                    modifier = Modifier.padding(horizontal = 21.dp, vertical = 12.dp),
                    text = it
                )
                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = ComposeAppTheme.colors.steel10
                )
            }
            items.forEachIndexed { index, item ->
                CellMultilineLawrence(
                    borderBottom = true
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                selected = index
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = item.title,
                                style = ComposeAppTheme.typography.body,
                                color = ComposeAppTheme.colors.leah
                            )
                            Text(
                                text = item.subtitle,
                                style = ComposeAppTheme.typography.subhead2,
                                color = ComposeAppTheme.colors.grey
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (index == selected) {
                            Image(
                                modifier = Modifier.padding(start = 5.dp),
                                painter = painterResource(id = R.drawable.ic_checkmark_20),
                                colorFilter = ColorFilter.tint(ComposeAppTheme.colors.jacob),
                                contentDescription = ""
                            )
                        }
                    }
                }
            }
            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                title = getString(R.string.Button_Done),
                onClick = {
                    if (notifyUnchanged || selectedIndex != selected) {
                        onItemSelected(selected)
                    }
                    dismiss()
                }
            )

        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onCancelled?.invoke()
    }

    override fun close() {
        super.close()
        onCancelled?.invoke()
    }

    companion object {
        fun show(
            fragmentManager: FragmentManager,
            title: String,
            subtitle: String,
            @DrawableRes icon: Int,
            items: List<BottomSheetSelectorViewItem>,
            selected: Int,
            onItemSelected: (Int) -> Unit,
            onCancelled: (() -> Unit)? = null,
            warning: String? = null,
            notifyUnchanged: Boolean = false
        ) {
            BottomSheetSelectorDialog(
                title,
                subtitle,
                icon,
                items,
                selected,
                onItemSelected,
                onCancelled,
                warning,
                notifyUnchanged
            )
                .show(fragmentManager, "selector_dialog")
        }
    }
}

data class BottomSheetSelectorViewItem(val title: String, val subtitle: String)
