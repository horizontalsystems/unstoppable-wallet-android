package com.quantum.wallet.bankwallet.ui.extensions

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.modules.market.ImageSource
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.components.ButtonPrimaryYellow
import com.quantum.wallet.bankwallet.ui.compose.components.HsImage
import com.quantum.wallet.bankwallet.ui.compose.components.HsSwitch
import com.quantum.wallet.bankwallet.ui.compose.components.RowUniversal
import com.quantum.wallet.bankwallet.ui.compose.components.SectionUniversalItem
import com.quantum.wallet.bankwallet.ui.compose.components.TextImportantWarning
import com.quantum.wallet.bankwallet.ui.compose.components.headline2_leah
import com.quantum.wallet.bankwallet.ui.compose.components.subhead2_grey
import com.quantum.wallet.bankwallet.ui.helpers.TextHelper
import com.quantum.wallet.core.helpers.HudHelper
import java.util.UUID

class BottomSheetSelectorMultipleDialog(
    private val title: String,
    private val icon: ImageSource,
    private val items: List<BottomSheetSelectorViewItem>,
    private val selectedIndexes: List<Int>,
    private val onItemsSelected: (List<Int>) -> Unit,
    private val onCancelled: (() -> Unit)?,
    private val warningTitle: String?,
    private val warning: String?,
    private val notifyUnchanged: Boolean,
    private val allowEmpty: Boolean
) : BaseComposableBottomSheetFragment() {

    val selected = mutableStateListOf<Int>().apply {
        addAll(selectedIndexes)
    }

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
                    BottomSheetHeader(
                        iconPainter = icon.painter(),
                        title = title,
                        onCloseClick = { close() }
                    ) {
                        BSContent()
                    }
                }
            }
        }
    }

    @Composable
    private fun BSContent() {
        val localView = LocalView.current
        warning?.let {
            TextImportantWarning(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                title = warningTitle,
                text = it
            )
        }
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(0.5.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(16.dp))
        ) {
            items.forEachIndexed { index, item ->
                val onClick = if (item.copyableString != null) {
                    {
                        HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
                        TextHelper.copyText(item.copyableString)
                    }
                } else {
                    null
                }

                SectionUniversalItem(
                    borderTop = index != 0,
                ) {
                    RowUniversal(
                        onClick = onClick,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalPadding = 0.dp
                    ) {
                        item.icon?.let { url ->
                            HsImage(
                                url = url,
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(32.dp)
                            )
                        }
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                            headline2_leah(text = item.title)
                            subhead2_grey(text = item.subtitle)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        HsSwitch(
                            modifier = Modifier.padding(start = 5.dp),
                            checked = selected.contains(index),
                            onCheckedChange = { checked ->
                                if (checked) {
                                    selected.add(index)
                                } else {
                                    selected.remove(index)
                                }
                            },
                        )
                    }
                }
            }
        }
        ButtonPrimaryYellow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
            title = getString(R.string.Button_Done),
            onClick = {
                if (notifyUnchanged || !equals(selectedIndexes, selected)) {
                    onItemsSelected(selected)
                }
                dismiss()
            },
            enabled = allowEmpty || selected.isNotEmpty()
        )
    }

    private fun equals(list1: List<Int>, list2: List<Int>): Boolean {
        return (list1 - list2).isEmpty() && (list2 - list1).isEmpty()
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
            icon: ImageSource,
            items: List<BottomSheetSelectorViewItem>,
            selected: List<Int>,
            onItemSelected: (List<Int>) -> Unit,
            onCancelled: (() -> Unit)? = null,
            warningTitle: String? = null,
            warning: String? = null,
            notifyUnchanged: Boolean = false,
            allowEmpty: Boolean
        ) {
            BottomSheetSelectorMultipleDialog(
                title,
                icon,
                items,
                selected,
                onItemSelected,
                onCancelled,
                warningTitle,
                warning,
                notifyUnchanged,
                allowEmpty
            )
                .show(fragmentManager, "selector_dialog")
        }
    }

    data class Config(
        val title: String,
        val selectedIndexes: List<Int>,
        val viewItems: List<BottomSheetSelectorViewItem>,
        val descriptionTitle: String? = null,
        val allowEmpty: Boolean = false
    ) {
        val uuid = UUID.randomUUID().toString()
    }
}

data class BottomSheetSelectorViewItem(
    val title: String,
    val subtitle: String,
    val copyableString: String? = null,
    val icon: String? = null
)
