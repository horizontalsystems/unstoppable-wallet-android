package io.horizontalsystems.bankwallet.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.viewHelpers.bottomDialog
import io.horizontalsystems.bankwallet.viewHelpers.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_confirmation.*

class ManageKeysDeleteAlert(private val listener: Listener,
                            private val checkboxItems: List<CheckBoxItem>,
                            private val subtitle: String) : DialogFragment(), ConfirmationsAdapter.Listener {

    interface Listener {
        fun onConfirmationSuccess()
    }

    private var adapter = ConfirmationsAdapter(this, checkboxItems)

    private lateinit var btnConfirm: Button
    private lateinit var rootView: View
    private lateinit var closeBtn: ImageView
    private lateinit var subtitleView: TextView
    private lateinit var recyclerView: RecyclerView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        rootView = View.inflate(context, R.layout.fragment_bottom_delete, null) as ViewGroup
        recyclerView = rootView.findViewById(R.id.recyclerView)

        btnConfirm = rootView.findViewById(R.id.btnConfirm)
        closeBtn = rootView.findViewById(R.id.closeButton)
        btnConfirm.setOnClickListener {
            listener.onConfirmationSuccess()
            dismiss()
        }

        closeBtn.setOnClickListener { dismiss() }
        subtitleView = rootView.findViewById(R.id.confirmSubtitle)
        return bottomDialog(activity, rootView)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter.notifyDataSetChanged()

        subtitleView.text = subtitle
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
            listener.onItemCheckMarkClick(adapterPosition, isChecked)
        }
    }

    fun bind(text: String) {
        confirmationCheckBox.text = text
    }
}

class CheckBoxItem(val text: String, var checked: Boolean = false)
