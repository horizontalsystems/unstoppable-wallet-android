package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_transactions_structure.*


class PrivacySettingsTransactionsStructureAdapter(private val listener: Listener)
    : RecyclerView.Adapter<PrivacySettingsTransactionsStructureAdapter.TransactionsStructureViewHolder>() {

    interface Listener {
        fun onClick()
    }

    private var dropDownValue: String = ""

    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionsStructureViewHolder {
        return TransactionsStructureViewHolder.create(parent, onClick = {
            listener.onClick()
        })
    }

    override fun onBindViewHolder(holder: TransactionsStructureViewHolder, position: Int) {
        holder.bind(dropDownValue)
    }

    fun bind(dropDownValue: String) {
        this.dropDownValue = dropDownValue
        notifyItemChanged(0)
    }

    class TransactionsStructureViewHolder(
            override val containerView: View,
            private val onClick: () -> Unit
    ) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        init {
            transactionsOrderSetting.setOnClickListener {
                onClick.invoke()
            }
        }

        fun bind(value: String) {
            transactionsOrderSetting.showDropdownValue(value)
        }


        companion object {
            const val layout = R.layout.view_holder_transactions_structure

            fun create(parent: ViewGroup, onClick: () -> Unit) = TransactionsStructureViewHolder(inflate(parent, layout, false), onClick)
        }

    }
}
