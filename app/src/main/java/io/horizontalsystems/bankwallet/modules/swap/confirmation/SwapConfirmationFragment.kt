package io.horizontalsystems.bankwallet.modules.swap.confirmation

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.ethereum.EthereumFeeViewModel
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.swap.SwapModule
import io.horizontalsystems.bankwallet.modules.swap.SwapViewModel
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.snackbar.CustomSnackbar
import io.horizontalsystems.snackbar.SnackbarDuration
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_confirmation_swap.*
import kotlinx.android.synthetic.main.view_holder_swap_confirmation_additional_info_item.*
import kotlinx.android.synthetic.main.view_holder_swap_confirmation_button.*
import kotlinx.android.synthetic.main.view_holder_swap_confirmation_input.*

class SwapConfirmationFragment : BaseFragment(), SwapConfirmationButtonAdapter.Listener {

    private val mainViewModel by navGraphViewModels<SwapViewModel>(R.id.swapFragment)
    private val feeViewModel by navGraphViewModels<EthereumFeeViewModel>(R.id.swapFragment)

    private val vmFactory by lazy { SwapConfirmationModule.Factory(mainViewModel.service, mainViewModel.tradeService, feeViewModel.transactionService) }
    private val viewModel by viewModels<SwapConfirmationViewModel>{ vmFactory }

    private var snackbarInProcess: CustomSnackbar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_confirmation_swap, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuCancel -> {
                    findNavController().popBackStack()
                    true
                }
                else -> false
            }
        }

        val inputAdapter = SwapConfirmationInputAdapter()
        val additionalInfoItemsAdapter = SwapConfirmationAdditionalInfoAdapter()
        val swapButtonAdapter = SwapConfirmationButtonAdapter(this)
        concatRecyclerView.adapter = ConcatAdapter(inputAdapter, additionalInfoItemsAdapter, swapButtonAdapter)

        viewModel.additionalLiveData().observe(viewLifecycleOwner, Observer {
            additionalInfoItemsAdapter.items = it
            additionalInfoItemsAdapter.notifyDataSetChanged()
        })

        viewModel.amountLiveData().observe(viewLifecycleOwner, Observer {
            inputAdapter.item = it
            inputAdapter.notifyDataSetChanged()
        })

        viewModel.errorLiveEvent().observe(viewLifecycleOwner, Observer { errorText ->
            errorText?.let{
                HudHelper.showErrorMessage(requireActivity().findViewById(android.R.id.content), it)
            }
            findNavController().popBackStack()
        })

        viewModel.loadingLiveData().observe(viewLifecycleOwner, Observer {
            snackbarInProcess = HudHelper.showInProcessMessage(requireView(), R.string.Swap_Swapping, SnackbarDuration.INDEFINITE)
        })

        viewModel.completedLiveData().observe(viewLifecycleOwner, Observer {
            HudHelper.showSuccessMessage(requireActivity().findViewById(android.R.id.content), R.string.Hud_Text_Success)
            Handler(Looper.getMainLooper()).postDelayed({
                findNavController().popBackStack(R.id.swapFragment, true)
            }, 1200)
        })

    }

    override fun onDestroyView() {
        snackbarInProcess?.dismiss()
        super.onDestroyView()
    }

    override fun onButtonClick() {
        viewModel.swap()
    }
}

class SwapConfirmationInputAdapter : RecyclerView.Adapter<SwapConfirmationInputAdapter.InputViewHolder>() {

    var item: SwapModule.ConfirmationAmountViewItem? = null

    override fun getItemCount() = if (item == null) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InputViewHolder {
        return InputViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: InputViewHolder, position: Int) {
        holder.bind(item)
    }

    class InputViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(item: SwapModule.ConfirmationAmountViewItem?) {
            payTitle.text = item?.payTitle
            payValue.text = item?.payValue
            getTitle.text = item?.getTitle
            getValue.text = item?.getValue
        }

        companion object {
            const val layout = R.layout.view_holder_swap_confirmation_input

            fun create(parent: ViewGroup) = InputViewHolder(inflate(parent, layout, false))
        }

    }
}

class SwapConfirmationAdditionalInfoAdapter : RecyclerView.Adapter<SwapConfirmationAdditionalInfoAdapter.ItemViewHolder>() {

    var items = listOf<SwapModule.ConfirmationAdditionalViewItem>()

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class ItemViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(item: SwapModule.ConfirmationAdditionalViewItem) {
            additionalTitle.text = item.title
            additionalValue.text = item.value
        }

        companion object {
            const val layout = R.layout.view_holder_swap_confirmation_additional_info_item

            fun create(parent: ViewGroup) = ItemViewHolder(inflate(parent, layout, false))
        }

    }
}

class SwapConfirmationButtonAdapter(private var listener: Listener) : RecyclerView.Adapter<SwapConfirmationButtonAdapter.ButtonViewHolder>() {

    interface Listener {
        fun onButtonClick()
    }

    private var enabled = true

    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder {
        return ButtonViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
        holder.bind(enabled)
        holder.swapButton.setOnSingleClickListener {
            listener.onButtonClick()
            enabled = false
            notifyDataSetChanged()
        }
    }

    class ButtonViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(enabled: Boolean) {
            swapButton.isEnabled = enabled
        }

        companion object {
            const val layout = R.layout.view_holder_swap_confirmation_button

            fun create(parent: ViewGroup) = ButtonViewHolder(inflate(parent, layout, false))
        }

    }
}
