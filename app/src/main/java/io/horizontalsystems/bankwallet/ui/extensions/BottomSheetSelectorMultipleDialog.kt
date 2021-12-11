package io.horizontalsystems.bankwallet.ui.extensions

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_bottom_selector.*
import kotlinx.android.synthetic.main.view_item_with_switch.*

class BottomSheetSelectorMultipleDialog(
    private val title: String,
    private val subtitle: String,
    private val icon: ImageSource,
    private val items: List<BottomSheetSelectorViewItem>,
    private val selected: List<Int>,
    private val onItemsSelected: (List<Int>) -> Unit,
    private val onCancelled: (() -> Unit)?,
    private val warning: String?,
    private val notifyUnchanged: Boolean
) : BaseBottomSheetDialogFragment() {

    private lateinit var itemsAdapter: MultipleSelectorItemsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setContentView(R.layout.fragment_bottom_selector)

        setTitle(title)
        setSubtitle(subtitle)
        setHeaderIcon(icon)

        setButton(selected.isNotEmpty())

        itemsAdapter = MultipleSelectorItemsAdapter(items, selected.toMutableList()) {
            setButton(it.isNotEmpty())
        }

        textWarning.text = warning
        textWarning.isVisible = warning != null
        divider.isVisible = warning != null

        rvItems.adapter = itemsAdapter

        buttonDoneCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )
    }

    private fun setButton(enabled: Boolean = false) {
        buttonDoneCompose.setContent {
            ComposeAppTheme {
                ButtonPrimaryYellow(
                    modifier = Modifier.padding(16.dp),
                    title = getString(R.string.Button_Done),
                    onClick = {
                        if (notifyUnchanged || !equals(itemsAdapter.selected, selected)) {
                            onItemsSelected(itemsAdapter.selected)
                        }
                        dismiss()
                    },
                    enabled = enabled
                )
            }
        }
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
            subtitle: String,
            icon: ImageSource,
            items: List<BottomSheetSelectorViewItem>,
            selected: List<Int>,
            onItemSelected: (List<Int>) -> Unit,
            onCancelled: (() -> Unit)? = null,
            warning: String? = null,
            notifyUnchanged: Boolean = false
        ) {
            BottomSheetSelectorMultipleDialog(
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

    data class Config(
        val icon: ImageSource,
        val title: String,
        val subtitle: String,
        val selectedIndexes: List<Int>,
        val viewItems: List<BottomSheetSelectorViewItem>,
        val description: String
    )
}

class MultipleSelectorItemsAdapter(
    private val items: List<BottomSheetSelectorViewItem>,
    val selected: MutableList<Int>,
    val onSelectedItemsChanged: (List<Int>) -> Unit
) : RecyclerView.Adapter<ItemViewHolderMultiple>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolderMultiple {
        return ItemViewHolderMultiple(inflate(parent, R.layout.view_item_with_switch, false)) { isChecked, position ->
            if (isChecked) {
                if (position !in selected) {
                    selected.add(position)
                }
            } else {
                selected.remove(position)
            }
            onSelectedItemsChanged(selected.toList())
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ItemViewHolderMultiple, position: Int) {
        holder.bind(items[position].title, items[position].subtitle, selected.contains(position))
    }
}

class ItemViewHolderMultiple(
    override val containerView: View,
    val onSwitch: (isChecked: Boolean, position: Int) -> Unit
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        containerView.setOnClickListener {
            toggleSwitch.isChecked = !toggleSwitch.isChecked
        }
    }

    fun bind(title: String, subtitle: String, selected: Boolean) {
        itemTitle.text = title
        itemSubtitle.text = subtitle

        toggleSwitch.setOnCheckedChangeListener(null)
        toggleSwitch.isChecked = selected
        toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            onSwitch(isChecked, bindingAdapterPosition)
        }
    }

}
