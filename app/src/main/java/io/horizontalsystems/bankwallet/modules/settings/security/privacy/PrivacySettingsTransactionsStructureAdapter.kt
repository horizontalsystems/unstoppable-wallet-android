package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.databinding.ViewHolderTransactionsStructureBinding

class PrivacySettingsTransactionsStructureAdapter(private val listener: Listener) :
    RecyclerView.Adapter<PrivacySettingsTransactionsStructureAdapter.TransactionsStructureViewHolder>() {

    interface Listener {
        fun onClick()
    }

    private var dropDownValue: String = ""

    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            : TransactionsStructureViewHolder {
        return TransactionsStructureViewHolder(
            ViewHolderTransactionsStructureBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        ) { listener.onClick() }
    }

    override fun onBindViewHolder(holder: TransactionsStructureViewHolder, position: Int) {
        holder.bind(dropDownValue)
    }

    fun bind(dropDownValue: String) {
        this.dropDownValue = dropDownValue
        notifyItemChanged(0)
    }

    class TransactionsStructureViewHolder(
        private val binding: ViewHolderTransactionsStructureBinding,
        private val onClick: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.transactionsOrderSetting.setOnClickListener {
                onClick.invoke()
            }
        }

        fun bind(value: String) {
            binding.transactionsOrderSetting.showDropdownValue(value)
        }

    }
}
