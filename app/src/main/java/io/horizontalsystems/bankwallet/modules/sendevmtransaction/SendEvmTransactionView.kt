package io.horizontalsystems.bankwallet.modules.sendevmtransaction

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ethereum.EthereumFeeViewModel
import io.horizontalsystems.views.ListPosition
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_amount.*
import kotlinx.android.synthetic.main.view_holder_amount.backgroundView
import kotlinx.android.synthetic.main.view_holder_title_value_hex.*
import kotlinx.android.synthetic.main.view_send_evm_transaction.view.*

class SendEvmTransactionView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.view_send_evm_transaction, this)
    }

    fun init(
            transactionViewModel: SendEvmTransactionViewModel,
            ethereumFeeViewModel: EthereumFeeViewModel,
            viewLifecycleOwner: LifecycleOwner,
            fragmentManager: FragmentManager,
            showSpeedInfoListener: () -> Unit
    ) {
        feeSelectorView.setFeeSelectorViewInteractions(
                ethereumFeeViewModel,
                ethereumFeeViewModel,
                viewLifecycleOwner,
                fragmentManager,
                showSpeedInfoListener
        )

        val adapter = SendEvmTransactionAdapter()
        recyclerView.adapter = adapter

        transactionViewModel.viewItemsLiveData.observe(viewLifecycleOwner, {
            adapter.items = it
            adapter.notifyDataSetChanged()
        })

        transactionViewModel.errorLiveData.observe(viewLifecycleOwner, {
            error.text = it
        })
    }

}

class SendEvmTransactionAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items: List<ViewItem> = listOf()

    enum class ViewType {
        Amount, TitleValue, TitleValueHex, TitleValueItalic
    }

    private val viewTypes = ViewType.values()

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ViewItem.Amount -> ViewType.Amount.ordinal
            is ViewItem.Address -> ViewType.TitleValueHex.ordinal
            is ViewItem.Input -> ViewType.TitleValue.ordinal
            is ViewItem.Value -> ViewType.TitleValue.ordinal
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewTypes[viewType]) {
            ViewType.Amount -> AmountViewHolder.create(parent)
            ViewType.TitleValueHex -> TitleValueHexViewHolder.create(parent)
            ViewType.TitleValue -> TitleValueViewHolder.create(parent)
            ViewType.TitleValueItalic -> TitleValueItalicViewHolder.create(parent)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val listPosition = ListPosition.Companion.getListPosition(itemCount, position)
        when (val item = items[position]) {
            is ViewItem.Amount -> (holder as? AmountViewHolder)?.bind(item, listPosition)
            is ViewItem.Address -> (holder as? TitleValueHexViewHolder)?.bind(item.title, item.value, listPosition)
            is ViewItem.Input -> (holder as? TitleValueViewHolder)?.bind(item.title, item.value, listPosition)
            is ViewItem.Value -> (holder as? TitleValueViewHolder)?.bind(item.title, item.value, listPosition)
        }
    }

    class AmountViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(item: ViewItem.Amount, position: ListPosition) {
            primaryName.text = item.amountData.primary.getAmountName()
            primaryAmount.text = item.amountData.primary.getFormatted()

            secondaryName.text = item.amountData.secondary?.getAmountName()
            secondaryAmount.text = item.amountData.secondary?.getFormatted()

            backgroundView.setBackgroundResource(position.getBackground())
        }

        companion object {
            fun create(parent: ViewGroup) = AmountViewHolder(inflate(parent, R.layout.view_holder_amount, false))
        }
    }

    class TitleValueHexViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(title: String, value: String, position: ListPosition) {
            titleTextView.text = title
            valueTextView.text = value

            backgroundView.setBackgroundResource(position.getBackground())
        }

        companion object {
            fun create(parent: ViewGroup) = TitleValueHexViewHolder(inflate(parent, R.layout.view_holder_title_value_hex, false))
        }
    }

    class TitleValueViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(title: String, value: String, position: ListPosition) {
            titleTextView.text = title
            valueTextView.text = value

            backgroundView.setBackgroundResource(position.getBackground())
        }

        companion object {
            fun create(parent: ViewGroup) = TitleValueViewHolder(inflate(parent, R.layout.view_holder_title_value, false))
        }
    }

    class TitleValueItalicViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(title: String, value: String, position: ListPosition) {
            titleTextView.text = title
            valueTextView.text = value

            backgroundView.setBackgroundResource(position.getBackground())
        }

        companion object {
            fun create(parent: ViewGroup) = TitleValueItalicViewHolder(inflate(parent, R.layout.view_holder_title_value_italic, false))
        }
    }
}
