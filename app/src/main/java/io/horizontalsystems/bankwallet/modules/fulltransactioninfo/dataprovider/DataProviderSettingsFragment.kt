package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.dataprovider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseDialogFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.core.dismissOnBackPressed
import io.horizontalsystems.views.ViewHolderProgressbar
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_explorer_switcher.*
import kotlinx.android.synthetic.main.view_holder_explorer_item.*

class DataProviderSettingsFragment : BaseDialogFragment(), DataProviderSettingsAdapter.Listener {

    private var adapter: DataProviderSettingsAdapter? = null

    private lateinit var viewModel: DataProviderSettingsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dialog?.window?.setWindowAnimations(R.style.RightDialogAnimations)
        dialog?.dismissOnBackPressed { dismiss() }
        return inflater.inflate(R.layout.fragment_explorer_switcher, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener {
            dismiss()
        }

        val coin = arguments?.getParcelable<Coin>("coinKey") ?: run {
            dismiss()
            return
        }

        viewModel = ViewModelProvider(this).get(DataProviderSettingsViewModel::class.java)
        viewModel.init(coin)

        adapter = DataProviderSettingsAdapter(this)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        viewModel.providerItems.observe(viewLifecycleOwner, Observer { items ->
            adapter?.items = items
            adapter?.notifyDataSetChanged()
        })

        viewModel.closeLiveEvent.observe(viewLifecycleOwner, Observer {
            dismiss()
        })
    }

    override fun onChangeProvider(item: DataProviderSettingsItem) {
        viewModel.delegate.onSelect(item)
    }

    companion object {
        fun arguments(coin: Coin) = Bundle(1).apply {
            putParcelable("coinKey", coin)
        }
    }
}

class DataProviderSettingsAdapter(private var listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val VIEW_TYPE_ITEM = 1
    private val VIEW_TYPE_LOADING = 2

    interface Listener {
        fun onChangeProvider(item: DataProviderSettingsItem)
    }

    var items = listOf<DataProviderSettingsItem>()

    override fun getItemCount() = if (items.isEmpty()) 1 else items.size

    override fun getItemViewType(position: Int): Int = if (items.isEmpty()) {
        VIEW_TYPE_LOADING
    } else {
        VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_ITEM -> {
                val containerView = inflater.inflate(ViewHolderDataProviderSettings.layoutResourceId, parent, false)
                ViewHolderDataProviderSettings(containerView)
            }
            else -> ViewHolderProgressbar(inflater.inflate(ViewHolderProgressbar.layoutResourceId, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderDataProviderSettings -> holder.bind(items[position], showBottomShade = (position == itemCount - 1)) { listener.onChangeProvider(items[position]) }
        }
    }

}

class ViewHolderDataProviderSettings(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: DataProviderSettingsItem, showBottomShade: Boolean, onClick: () -> (Unit)) {

        containerView.setOnSingleClickListener { onClick.invoke() }
        title.text = item.name
        subtitle.text = containerView.context.getString(if (item.online) R.string.FullInfo_Source_Online else R.string.FullInfo_Source_Offline)
        subtitle.setTextColor(ContextCompat.getColor(subtitle.context, if (item.online) R.color.green_d else R.color.red_d))

        statusChecking.isVisible = item.checking
        subtitle.isVisible = !item.checking

        checkmarkIcon.isVisible = item.selected
        bottomShade.isVisible = showBottomShade
    }

    companion object {
        const val layoutResourceId = R.layout.view_holder_explorer_item
    }

}
