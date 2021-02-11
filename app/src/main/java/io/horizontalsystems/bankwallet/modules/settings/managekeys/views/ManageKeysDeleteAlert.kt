package io.horizontalsystems.bankwallet.modules.settings.managekeys.views

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_confirmation.*

class ManageKeysDeleteAlert(
        private val listener: Listener,
        private val checkboxItems: List<CheckBoxItem>,
        private val subtitle: String)
    : BaseBottomSheetDialogFragment(), ConfirmationsAdapter.Listener {

    interface Listener {
        fun onConfirmationSuccess()
    }

    private var adapter = ConfirmationsAdapter(this, checkboxItems)

    private lateinit var btnConfirm: Button
    private lateinit var recyclerView: RecyclerView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setContentView(R.layout.fragment_bottom_delete)

        setTitle(getString(R.string.ManageKeys_Delete_Title))
        setSubtitle(subtitle)
        setHeaderIcon(R.drawable.ic_attention_red_24)

        recyclerView = view.findViewById(R.id.recyclerView)
        btnConfirm = view.findViewById(R.id.btnConfirm)
        btnConfirm.setOnClickListener {
            listener.onConfirmationSuccess()
            dismiss()
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter.notifyDataSetChanged()
    }

    override fun onItemCheckMarkClick(position: Int, checked: Boolean) {
        checkboxItems[position].checked = checked
        checkConfirmations()
    }

    private fun checkConfirmations() {
        val allChecked = checkboxItems.all { it.checked }

        btnConfirm.isEnabled = allChecked
    }

    companion object {
        fun show(activity: FragmentActivity, subtitle: String, checkboxItems: List<String>, listener: Listener) {
            val fragment = ManageKeysDeleteAlert(listener, checkboxItems.map { CheckBoxItem(it) }, subtitle)

            activity.supportFragmentManager.beginTransaction().apply {
                add(fragment, "bottom_confirm_alert")
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

class ViewHolderConfirmation(override val containerView: View, private val listener: ConfirmationsAdapter.Listener)
    : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        confirmationCheckBox.setOnCheckedChangeListener { _, isChecked ->
            listener.onItemCheckMarkClick(bindingAdapterPosition, isChecked)
        }
    }

    fun bind(text: String) {
        confirmationCheckBox.text = text
    }
}

class CheckBoxItem(val text: String, var checked: Boolean = false)
