package io.horizontalsystems.bankwallet.modules.manageaccount.dialogs

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryRed
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_bottom_confirm_unlink.*
import kotlinx.android.synthetic.main.view_holder_confirmation.*

class UnlinkConfirmationDialog : BaseBottomSheetDialogFragment(), ConfirmationsAdapter.Listener {

    interface Listener {
        fun onUnlinkConfirm()
    }

    private var listener: Listener? = null
    private var checkboxItems = listOf<CheckBoxItem>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setContentView(R.layout.fragment_bottom_confirm_unlink)

        setTitle(getString(R.string.ManageKeys_Delete_Title))
        setSubtitle(requireArguments().getString(ACCOUNT_NAME))
        setHeaderIcon(R.drawable.ic_attention_red_24)

        confirmButtonCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        setButton()

        checkboxItems = requireArguments().getParcelableArrayList<CheckBoxItem>(CHECKBOX_ITEMS)?.toList()
                ?: listOf()
        val adapter = ConfirmationsAdapter(this, checkboxItems)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter.notifyDataSetChanged()
    }

    private fun setButton(enabled: Boolean = false) {
        confirmButtonCompose.setContent {
            ComposeAppTheme {
                ButtonPrimaryRed(
                    modifier = Modifier.padding(16.dp),
                    title = getString(R.string.ManageKeys_Delete_FromPhone),
                    onClick = {
                        listener?.onUnlinkConfirm()
                        dismiss()
                    },
                    enabled = enabled

                )
            }
        }
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    override fun onItemCheckMarkClick(position: Int, checked: Boolean) {
        checkboxItems[position].checked = checked
        checkConfirmations()
    }

    private fun checkConfirmations() {
        val enabled = checkboxItems.all { it.checked }
        setButton(enabled)
    }

    companion object {
        private const val ACCOUNT_NAME = "account_name"
        private const val CHECKBOX_ITEMS = "checkbox_items"

        fun show(fragmentManager: FragmentManager, accountName: String, checkboxItems: List<String>) {
            val fragment = UnlinkConfirmationDialog().apply {
                arguments = bundleOf(
                        ACCOUNT_NAME to accountName,
                        CHECKBOX_ITEMS to checkboxItems.map { CheckBoxItem(it) }
                )
            }

            fragmentManager.beginTransaction().apply {
                add(fragment, "unlink_confirmation_dialog")
                commitAllowingStateLoss()
            }
        }
    }
}

class ConfirmationsAdapter(private var listener: Listener, private val confirmations: List<CheckBoxItem>)
    : RecyclerView.Adapter<ViewHolderConfirmation>() {

    interface Listener {
        fun onItemCheckMarkClick(position: Int, checked: Boolean)
    }

    override fun getItemCount() = confirmations.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderConfirmation {
        return ViewHolderConfirmation(inflate(parent, R.layout.view_holder_confirmation), listener)
    }

    override fun onBindViewHolder(holder: ViewHolderConfirmation, position: Int) {
        holder.bind(confirmations[position].text)
    }
}

class ViewHolderConfirmation(
        override val containerView: View, private val listener: ConfirmationsAdapter.Listener
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        confirmationCheckBox.setOnCheckedChangeListener { _, isChecked ->
            listener.onItemCheckMarkClick(bindingAdapterPosition, isChecked)
        }
    }

    fun bind(text: String) {
        confirmationCheckBox.text = text
    }
}

@Parcelize
class CheckBoxItem(val text: String, var checked: Boolean = false) : Parcelable
