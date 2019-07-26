package io.horizontalsystems.bankwallet.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.viewHelpers.bottomDialog
import io.horizontalsystems.bankwallet.viewHelpers.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_confirmation.*

class BottomConfirmAlert : DialogFragment(), ConfirmationsAdapter.Listener {

    interface Listener {
        fun onConfirmationSuccess()
    }

    private var adapter = ConfirmationsAdapter(this)
    private var checkboxItemList: MutableList<CheckBoxItem> = mutableListOf()

    private lateinit var listener: Listener
    private lateinit var btnConfirm: Button
    private lateinit var rootView: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var color: BottomButtonColor

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        rootView = View.inflate(context, R.layout.fragment_bottom_confirmations, null) as ViewGroup
        btnConfirm = rootView.findViewById(R.id.btnConfirm)
        recyclerView = rootView.findViewById(R.id.recyclerView)

        btnConfirm.setOnClickListener {
            listener.onConfirmationSuccess()
            dismiss()
        }

        btnConfirm.setBackgroundResource(getBackgroundResId(color))
        setButtonTextColor(color, false)

        return bottomDialog(activity, rootView)
    }

    private fun setButtonTextColor(buttonColor: BottomButtonColor, enabled: Boolean) {
        val colorRes = when {
            enabled -> when (buttonColor) {
                BottomButtonColor.RED -> R.color.white
                BottomButtonColor.YELLOW -> R.color.black
            }
            else -> R.color.grey_50
        }

        context?.let {
            btnConfirm.setTextColor(ContextCompat.getColor(it, colorRes))
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter.confirmations = checkboxItemList
        adapter.notifyDataSetChanged()
    }

    override fun onItemCheckMarkClick(position: Int, checked: Boolean) {
        checkboxItemList[position].checked = checked
        checkConfirmations()
    }

    private fun getBackgroundResId(color: BottomButtonColor): Int = when (color) {
        BottomButtonColor.RED -> R.drawable.button_red_background_12
        BottomButtonColor.YELLOW -> R.drawable.button_yellow_background_12
    }

    private fun checkConfirmations() {
        val allChecked = checkboxItemList.all { it.checked }

        btnConfirm.isEnabled = allChecked
        setButtonTextColor(color, allChecked)
    }

    companion object {
        fun show(activity: FragmentActivity, textResourcesList: MutableList<String>, listener: Listener, color: BottomButtonColor = BottomButtonColor.YELLOW) {
            val fragment = BottomConfirmAlert()
            fragment.listener = listener
            fragment.color = color
            textResourcesList.forEach {
                fragment.checkboxItemList.add(CheckBoxItem(it))
            }
            val ft = activity.supportFragmentManager.beginTransaction()
            ft.add(fragment, "bottom_confirm_alert")
            ft.commitAllowingStateLoss()
        }
    }
}

enum class BottomButtonColor {
    YELLOW, RED
}

class ConfirmationsAdapter(private var listener: Listener) : RecyclerView.Adapter<ViewHolderConfirmation>() {
    interface Listener {
        fun onItemCheckMarkClick(position: Int, checked: Boolean)
    }

    var confirmations = listOf<CheckBoxItem>()

    override fun getItemCount() = confirmations.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderConfirmation {
        return ViewHolderConfirmation(inflate(parent, R.layout.view_holder_confirmation), listener)
    }

    override fun onBindViewHolder(holder: ViewHolderConfirmation, position: Int) {
        holder.bind(confirmations[position].text)
    }
}

class ViewHolderConfirmation(override val containerView: View, val listener: ConfirmationsAdapter.Listener)
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
