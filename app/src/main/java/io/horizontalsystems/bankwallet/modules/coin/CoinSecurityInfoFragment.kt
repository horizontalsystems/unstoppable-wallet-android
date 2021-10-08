package io.horizontalsystems.bankwallet.modules.coin

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_coin_security_info.*
import kotlinx.android.synthetic.main.view_holder_coin_security_info.*
import kotlinx.android.synthetic.main.view_holder_coin_security_info_header.*

class CoinSecurityInfoFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_coin_security_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuClose -> {
                    findNavController().popBackStack()
                    true
                }
                else -> false
            }
        }

        val info = arguments?.getParcelable<CoinDataClickType.SecurityInfo>("info") ?: run {
            findNavController().popBackStack();
            return
        }

        recyclerView.adapter = DataAdapter(info.title, info.items)
    }
}

class DataAdapter(private val header: Int, private val items: List<CoinDataClickType.SecurityInfo.Item>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val viewTypeItem = 0
    private val viewTypeHeader = 1

    override fun getItemCount(): Int {
        return items.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> viewTypeHeader
            else -> viewTypeItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            viewTypeItem -> ViewHolder(inflate(parent, R.layout.view_holder_coin_security_info, false))
            viewTypeHeader -> ViewHolderHeader(inflate(parent, R.layout.view_holder_coin_security_info_header, false))
            else -> throw IllegalArgumentException("No such viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> holder.bind(items[position - 1])
            is ViewHolderHeader -> holder.bind(header)
        }
    }

    class ViewHolderHeader(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(title: Int) {
            securityInfoTitle.text = Translator.getString(title)
        }
    }

    class ViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(item: CoinDataClickType.SecurityInfo.Item) {
            infoTitle.text = Translator.getString(item.title)
            infoTitle.setTextColor(containerView.context.getColor(item.color))

            infoText.text = Translator.getString(item.info)
        }
    }
}

sealed class CoinDataClickType: Parcelable {
    @Parcelize
    class SecurityInfo(val title: Int, val items: List<Item>) : CoinDataClickType() {
        @Parcelize
        class Item(val title: Int, val color: Int, val info: Int): Parcelable
    }
}
