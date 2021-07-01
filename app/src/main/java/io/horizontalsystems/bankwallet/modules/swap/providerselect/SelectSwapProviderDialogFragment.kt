package io.horizontalsystems.bankwallet.modules.swap.providerselect

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseDialogFragment
import io.horizontalsystems.bankwallet.modules.swap.SwapMainViewModel
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.setOnSingleClickListener
import io.horizontalsystems.views.ListPosition
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_swap_select_token.*

class SelectSwapProviderDialogFragment : BaseDialogFragment() {

    private val mainViewModel by navGraphViewModels<SwapMainViewModel>(R.id.swapFragment)
    private val viewModel by viewModels<SelectSwapProviderViewModel> { SelectSwapProviderModule.Factory(mainViewModel.service) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dialog?.window?.setWindowAnimations(R.style.RightDialogAnimations)
        return inflater.inflate(R.layout.fragment_swap_select_provider, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val adapter = SelectSwapProviderAdapter { position ->
            viewModel.onClick(position)
            findNavController().popBackStack()
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        viewModel.viewItemsLiveData.observe(viewLifecycleOwner, {
            adapter.items = it
            adapter.notifyDataSetChanged()
        })

    }

}

class SelectSwapProviderAdapter(private var onItemClick: (position: Int) -> Unit) : RecyclerView.Adapter<ViewHolderSwapProvider>() {

    var items = listOf<SwapProviderViewItem>()

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderSwapProvider {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolderSwapProvider(inflater.inflate(ViewHolderSwapProvider.layoutResourceId, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolderSwapProvider, position: Int) {
        holder.bind(items[position]) { onItemClick(position) }
    }

}

data class SwapProviderViewItem(
        val title: String,
        val iconName: String,
        val isSelected: Boolean,
        val listPosition: ListPosition
)

class ViewHolderSwapProvider(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: SwapProviderViewItem, onClick: () -> (Unit)) {
        val image = containerView.findViewById<ImageView>(io.horizontalsystems.languageswitcher.R.id.image)
        val title = containerView.findViewById<TextView>(io.horizontalsystems.languageswitcher.R.id.title)
        val subtitle = containerView.findViewById<TextView>(io.horizontalsystems.languageswitcher.R.id.subtitle)
        val checkmarkIcon = containerView.findViewById<ImageView>(io.horizontalsystems.languageswitcher.R.id.checkmarkIcon)

        containerView.setOnSingleClickListener { onClick.invoke() }
        image.setImageResource(getDrawableResource(containerView.context, item.iconName))

        title.text = item.title
        subtitle.isVisible = false
        checkmarkIcon.isVisible = item.isSelected
        containerView.setBackgroundResource(item.listPosition.getBackground())
    }

    private fun getDrawableResource(context: Context, providerId: String): Int {
        return context.resources.getIdentifier(providerId, "drawable", context.packageName)
    }

    companion object {
        val layoutResourceId: Int
            get() = R.layout.view_holder_item_with_checkmark
    }
}
