package bitcoin.wallet.ui.dialogs

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.Button
import bitcoin.wallet.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_confirmation.*

class BottomConfirmAlert : DialogFragment(), ConfirmationsAdapter.Listener {

    interface Listener {
        fun onConfirmationSuccess()
    }

    private var mDialog: Dialog? = null
    private lateinit var rootView: View
    private lateinit var btnConfirm: Button
    private var checkboxItemList: MutableList<CheckBoxItem> = mutableListOf()
    private var adapter = ConfirmationsAdapter(this)
    private lateinit var recyclerView: RecyclerView

    private lateinit var listener: Listener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = activity?.let { AlertDialog.Builder(it, R.style.BottomDialog) }

        rootView = View.inflate(context, R.layout.fragment_bottom_confirmations, null) as ViewGroup
        btnConfirm = rootView.findViewById(R.id.btnConfirm)
        recyclerView = rootView.findViewById(R.id.recyclerView)
        builder?.setView(rootView)

        mDialog = builder?.create()
        mDialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        mDialog?.window?.setGravity(Gravity.BOTTOM)
        mDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnConfirm.setOnClickListener {
            listener.onConfirmationSuccess()
            dismiss()
        }

        return mDialog as Dialog
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

    private fun checkConfirmations() {
        val uncheckedCount = checkboxItemList.asSequence().filter { !it.checked }.count()
        btnConfirm.isEnabled = uncheckedCount == 0
    }

    companion object {
        fun show(activity: FragmentActivity, textResourcesList: MutableList<Int>, listener: Listener) {
            val fragment = BottomConfirmAlert()
            fragment.listener = listener
            textResourcesList.forEach {
                fragment.checkboxItemList.add(CheckBoxItem(it))
            }
            val ft = activity.supportFragmentManager.beginTransaction()
            ft.add(fragment, "bottom_confirm_alert")
            ft.commitAllowingStateLoss()
        }
    }
}

class ConfirmationsAdapter(private var listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onItemCheckMarkClick(position: Int, checked: Boolean)
    }

    var confirmations: List<CheckBoxItem> = listOf()

    override fun getItemCount() = confirmations.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            ViewHolderConfirmation(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_confirmation, parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderConfirmation -> holder.bind(confirmations[position]) { checked ->
                listener.onItemCheckMarkClick(position, checked)
                notifyDataSetChanged()
            }
        }
    }
}

class ViewHolderConfirmation(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(checkBoxItem: CheckBoxItem, onClick: ((Boolean) -> (Unit))) {
        confirmationCheckBox.text = containerView.context.getString(checkBoxItem.textRes)
        confirmationCheckBox.isChecked = checkBoxItem.checked

        confirmationCheckBox.setOnCheckedChangeListener { _, _ ->
            onClick.invoke(confirmationCheckBox.isChecked)
        }
    }
}

class CheckBoxItem(val textRes: Int, var checked: Boolean = false)
