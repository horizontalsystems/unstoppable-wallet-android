package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.views.helpers.LayoutHelper
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_transaction_info_item.*

class TransactionDetailsAdapter(private val viewModel: TransactionInfoViewModel) : RecyclerView.Adapter<TransactionDetailsAdapter.DetailViewHolder>() {

    private var items = listOf<TransactionDetailViewItem>()

    fun setItems(items: List<TransactionDetailViewItem>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {

        return DetailViewHolder(inflate(parent, R.layout.view_holder_transaction_info), viewModel)
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class DetailViewHolder(override val containerView: View, private val viewModel: TransactionInfoViewModel) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        private val context get() = itemView.context

        fun bind(detail: TransactionDetailViewItem) {
            itemView.setOnClickListener(null)
            itemView.isClickable = false
            txtTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            decoratedText.isVisible = false
            btnAction.isVisible = false
            valueText.isVisible = false
            statusInfoIcon.isVisible = false
            valueText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            transactionStatusView.isVisible = false

            when (detail) {
                is TransactionDetailViewItem.Rate -> bindRate(detail)
                is TransactionDetailViewItem.Fee -> bindFee(detail)
                is TransactionDetailViewItem.From -> bindFrom(detail)
                is TransactionDetailViewItem.Memo -> bindMemo(detail)
                is TransactionDetailViewItem.To -> bindTo(detail)
                is TransactionDetailViewItem.Recipient -> bindRecipient(detail)
                is TransactionDetailViewItem.Id -> bindId(detail)
                is TransactionDetailViewItem.Status -> bindStatus(detail)
                is TransactionDetailViewItem.DoubleSpend -> bindDoubleSpend()
                is TransactionDetailViewItem.SentToSelf -> {
                    bindHint(context.getString(R.string.TransactionInfo_SentToSelfNote), iconStart = R.drawable.ic_incoming_20)
                }
                is TransactionDetailViewItem.RawTransaction -> bindRaw()
                is TransactionDetailViewItem.LockInfo -> bindLockInfo(detail)
            }
        }

        private fun bindRecipient(detail: TransactionDetailViewItem.Recipient) {
            bindAddress(context.getString(R.string.TransactionInfo_RecipientHash), detail.recipient) {
                viewModel.delegate.onClickRecipientHash()
            }
        }

        private fun bindLockInfo(detail: TransactionDetailViewItem.LockInfo) {
            if (detail.lockState.locked) {
                bindHint(context.getString(R.string.TransactionInfo_LockedUntil, DateHelper.getFullDate(detail.lockState.date)), R.drawable.ic_lock_20, R.drawable.ic_info_20)
                itemView.setOnSingleClickListener {
                    viewModel.delegate.onClickLockInfo()
                }
            } else {
                bindHint(context.getString(R.string.TransactionInfo_UnlockedAt, DateHelper.getFullDate(detail.lockState.date)), iconStart = R.drawable.ic_unlock_20)
            }
        }

        private fun bindDoubleSpend() {
            bindHint(context.getString(R.string.TransactionInfo_DoubleSpendNote), R.drawable.ic_double_spend_20, R.drawable.ic_info_20)
            itemView.setOnSingleClickListener {
                viewModel.delegate.onClickDoubleSpendInfo()
            }
        }

        private fun bindId(detail: TransactionDetailViewItem.Id) {
            bindHashId(itemView.context.getString(R.string.TransactionInfo_Id), detail.id)
            decoratedText.setOnSingleClickListener {
                viewModel.delegate.onClickTransactionId()
            }
            btnAction.setOnSingleClickListener {
                viewModel.delegate.onShare()
            }
        }

        private fun bindRaw() {
            txtTitle.text = itemView.context.getString(R.string.TransactionInfo_RawTransaction)
            btnAction.setImageResource(R.drawable.ic_copy_20)
            btnAction.isVisible = true
            btnAction.setOnSingleClickListener {
                viewModel.delegate.onRawTransaction()
            }
        }

        private fun bindTo(detail: TransactionDetailViewItem.To) {
            bindAddress(itemView.context.getString(R.string.TransactionInfo_To), detail.to) {
                viewModel.delegate.onClickTo()
            }
        }

        private fun bindFrom(detail: TransactionDetailViewItem.From) {
            bindAddress(itemView.context.getString(R.string.TransactionInfo_From), detail.from) {
                viewModel.delegate.onClickFrom()
            }
        }

        private fun bindMemo(detail: TransactionDetailViewItem.Memo) {
            bind(itemView.context.getString(R.string.TransactionInfo_Memo), detail.memo)
        }

        private fun bindFee(detail: TransactionDetailViewItem.Fee) {
            getFeeText(detail.coinValue, detail.currencyValue)?.let { feeText ->
                bind(itemView.context.getString(R.string.TransactionInfo_Fee), feeText)
            }
        }

        private fun bindRate(detail: TransactionDetailViewItem.Rate) {
            val rateFormatted = App.numberFormatter.formatFiat(detail.currencyValue.value, detail.currencyValue.currency.symbol, 2, 4)
            val rateValue = itemView.context.getString(R.string.Balance_RatePerCoin, rateFormatted, detail.coinCode)
            bind(itemView.context.getString(R.string.TransactionInfo_HistoricalRate), rateValue)
        }

        private fun bindStatus(detail: TransactionDetailViewItem.Status) {
            txtTitle.setText(R.string.TransactionInfo_Status)
            transactionStatusView.bind(detail.status, detail.incoming)
            transactionStatusView.isVisible = true
            if (detail.status != TransactionStatus.Completed){
                statusInfoIcon.isVisible = true
                itemView.setOnSingleClickListener {
                    viewModel.delegate.onClickStatusInfo()
                }
            }
        }

        private fun getFeeText(coinValue: CoinValue, currencyValue: CurrencyValue?): String? {
            var fee: String = App.numberFormatter.formatCoin(coinValue.value, coinValue.coin.code, 0, 8)
            currencyValue?.let {
                val fiatFee = App.numberFormatter.formatFiat(it.value, it.currency.symbol, 0, 2)
                fee += " | $fiatFee"
            }

            return fee
        }

        private fun bind(title: String, value: String) {
            txtTitle.text = title
            valueText.text = value
            valueText.isVisible = true
        }

        private fun bindAddress(title: String, address: String, l: ((v: View) -> Unit)) {
            txtTitle.text = title
            decoratedText.text = address
            decoratedText.isVisible = true
            decoratedText.setOnSingleClickListener(l)
        }


        private fun bindHashId(title: String, address: String) {
            txtTitle.text = title
            decoratedText.text = address
            decoratedText.isVisible = true
            btnAction.isVisible = true
        }

        private fun bindHint(hintText: String, @DrawableRes iconStart: Int = 0, @DrawableRes iconEnd: Int = 0) {
            txtTitle.text = hintText
            txtTitle.isVisible = true
            txtTitle.setCompoundDrawablesWithIntrinsicBounds(iconStart, 0, 0, 0)
            txtTitle.compoundDrawablePadding = LayoutHelper.dp(11f, itemView.context)

            // need to have a view in the right of the title to have title to be aligned to the left
            valueText.text = null
            valueText.isVisible = true
            valueText.setCompoundDrawablesWithIntrinsicBounds(0, 0, iconEnd, 0)
            valueText.compoundDrawablePadding = LayoutHelper.dp(16f, itemView.context)
        }
    }
}
