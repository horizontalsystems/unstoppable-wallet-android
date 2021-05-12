package io.horizontalsystems.bankwallet.modules.basecurrency

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.views.helpers.LayoutHelper
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_base_currency_settings.*
import java.util.*

class BaseCurrencySettingsFragment : BaseFragment(), RVAdapter.ViewHolder.Listener {

    private val viewModel by viewModels<BaseCurrencySettingsViewModel> { BaseCurrencySettingsModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_base_currency_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.disclaimerLiveEvent.observe(viewLifecycleOwner) {
            val dialog = BaseCurrencyDisclaimerDialog(it)
            dialog.onConfirm = {
                viewModel.onAcceptDisclaimer()
            }
            dialog.show(childFragmentManager, "selector_dialog")
        }

        viewModel.finishLiveEvent.observe(viewLifecycleOwner) {
            findNavController().popBackStack()
        }

        val adapterPopularItems = RVAdapter(viewModel.popularItems, this)
        val adapterOtherItems = RVAdapter(viewModel.otherItems, this)

        recyclerView.adapter = ConcatAdapter(adapterPopularItems, RVAdapterSectionHeader(getString(R.string.SettingsCurrency_Other)), adapterOtherItems)
    }

    override fun onSelectItem(item: CurrencyViewItemWrapper) {
        viewModel.setBaseCurrency(item.currency)
    }
}

class RVAdapterSectionHeader(val title: String) : RecyclerView.Adapter<RVAdapterSectionHeader.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder.create(parent)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(title)
    override fun getItemCount() = 1

    class ViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(title: String) {
            containerView.findViewById<TextView>(R.id.title).text = title
        }

        companion object {
            fun create(parent: ViewGroup): ViewHolder {
                val view = inflate(parent, R.layout.view_holder_section_header)
                return ViewHolder(view)
            }
        }
    }
}

class RVAdapter(val items: List<CurrencyViewItemWrapper>, val listener: ViewHolder.Listener) : RecyclerView.Adapter<RVAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder.create(parent, listener)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class ViewHolder(override val containerView: View, private val listener: Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        private var item: CurrencyViewItemWrapper? = null

        interface Listener {
            fun onSelectItem(item: CurrencyViewItemWrapper)
        }

        init {
            containerView.setOnSingleClickListener {
                item?.let {
                    listener.onSelectItem(it)
                }
            }
        }

        fun bind(item: CurrencyViewItemWrapper) {
            this.item = item

            val image = containerView.findViewById<ImageView>(R.id.image)
            val title = containerView.findViewById<TextView>(R.id.title)
            val subtitle = containerView.findViewById<TextView>(R.id.subtitle)
            val checkmarkIcon = containerView.findViewById<ImageView>(R.id.checkmarkIcon)

            image.setImageResource(LayoutHelper.getCurrencyDrawableResource(containerView.context, item.currency.code.toLowerCase(Locale.ENGLISH)))
            title.text = item.currency.code
            subtitle.text = item.currency.symbol
            checkmarkIcon.isVisible = item.selected

            containerView.setBackgroundResource(item.listPosition.getBackground())
        }

        companion object {
            fun create(parent: ViewGroup, listener: Listener): ViewHolder {
                val view = inflate(parent, R.layout.view_holder_item_with_checkmark)
                return ViewHolder(view, listener)
            }
        }
    }
}
