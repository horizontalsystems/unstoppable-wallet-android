package io.horizontalsystems.bankwallet.modules.walletconnect.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_dapp_item.*

class DappInfoAdapter : RecyclerView.Adapter<DappInfoAdapter.ViewHolder>() {

    data class DappItemData(var titleStringResId: Int) {
        var value: String? = null
        var valueStringResId: Int? = null
        var valueColor: Int? = null
        var visible: Boolean = false
    }

    class ViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bind(dappItemData: DappItemData) {
            title.text = containerView.context.getString(dappItemData.titleStringResId)
            value.text = dappItemData.valueStringResId?.let { containerView.context.getString(it) }
                    ?: dappItemData.value

            dappItemData.valueColor?.let { color ->
                value.setTextColor(containerView.context.getColor(color))
            }
        }

    }

    private val allItems = listOf(
            DappItemData(R.string.WalletConnect_Status),
            DappItemData(R.string.WalletConnect_Url),
            DappItemData(R.string.WalletConnect_SignedTransactions),
    )

    private val visibleItems
        get() = allItems.filter { it.visible }

    private var itemCount = visibleItems.size

    var status: WalletConnectMainViewModel.Status? = null
        set(value) {
            field = value

            val statusStringId = when (value) {
                WalletConnectMainViewModel.Status.OFFLINE -> R.string.WalletConnect_Status_Offline
                WalletConnectMainViewModel.Status.ONLINE -> R.string.WalletConnect_Status_Online
                WalletConnectMainViewModel.Status.CONNECTING -> R.string.WalletConnect_Status_Connecting
                null -> null
            }

            val color = when (value) {
                WalletConnectMainViewModel.Status.OFFLINE -> R.color.lucian
                WalletConnectMainViewModel.Status.ONLINE -> R.color.remus
                WalletConnectMainViewModel.Status.CONNECTING -> R.color.leah
                null -> null
            }

            getItem(R.string.WalletConnect_Status).apply {
                valueStringResId = statusStringId
                visible = statusStringId != null
                this.valueColor = color
            }

            itemCount = visibleItems.size
            notifyDataSetChanged()
        }

    var url = ""
        set(value) {
            field = value

            getItem(R.string.WalletConnect_Url).apply {
                this.value = value
                visible = true
            }

            itemCount = visibleItems.size
            notifyDataSetChanged()
        }

    var signedTransactionsVisible: Boolean = false
        set(value) {
            field = value

            getItem(R.string.WalletConnect_SignedTransactions).visible = value

            itemCount = visibleItems.size
            notifyDataSetChanged()
        }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_dapp_item, parent, false))
    }

    override fun getItemCount(): Int {
        return itemCount
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(visibleItems[position])
    }

    private fun getItem(itemId: Int): DappItemData {
        return allItems.first { it.titleStringResId == itemId }
    }

}

