package io.horizontalsystems.bankwallet.modules.coin.audits

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.coin.CoinAuditItem
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_audit_item.*
import kotlinx.android.synthetic.main.view_holder_coin_investors_section_header.*

class CoinAuditsAdapter(
    private val items: List<CoinAuditItem>,
    private val listener: Listener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onItemClick(url: String)
    }

    private val viewTypeHeader = 0
    private val viewTypeItem = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            viewTypeItem -> ViewHolderAuditReport(inflate(parent, R.layout.view_holder_coin_audit_item))
            else -> ViewHolderAuditSectionHeader(inflate(parent, R.layout.view_holder_coin_investors_section_header))
        }
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = when (items[position]) {
        is CoinAuditItem.Report -> viewTypeItem
        is CoinAuditItem.Header -> viewTypeHeader
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is ViewHolderAuditReport -> holder.bind(item as CoinAuditItem.Report) {
                listener.onItemClick(item.link)
            }
            is ViewHolderAuditSectionHeader -> holder.bind(item as CoinAuditItem.Header)
        }
    }
}

class ViewHolderAuditReport(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: CoinAuditItem.Report, onClick: () -> Unit) {
        title.text = DateHelper.formatDate(item.date, "MMM d, yyyy")
        subtitle.text = item.name
        issuesCount.text = Translator.getString(R.string.CoinPage_Audits_Issues, item.issues)

        backgroundView.setBackgroundResource(item.position.getBackground())
        containerView.setOnClickListener {
            onClick.invoke()
        }
    }
}

class ViewHolderAuditSectionHeader(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: CoinAuditItem.Header) {
        sectionTitle.text = item.name
    }
}
