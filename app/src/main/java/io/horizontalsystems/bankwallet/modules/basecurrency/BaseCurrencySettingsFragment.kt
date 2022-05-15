package io.horizontalsystems.bankwallet.modules.basecurrency

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.databinding.FragmentBaseCurrencySettingsBinding
import io.horizontalsystems.bankwallet.databinding.ViewHolderSectionHeaderBinding
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.views.databinding.ViewHolderItemWithCheckmarkBinding
import io.horizontalsystems.views.helpers.LayoutHelper
import java.util.*

class BaseCurrencySettingsFragment : BaseFragment(), RVAdapter.ViewHolder.Listener {

    private val viewModel by viewModels<BaseCurrencySettingsViewModel> { BaseCurrencySettingsModule.Factory() }

    private var _binding: FragmentBaseCurrencySettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBaseCurrencySettingsBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener {
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

        binding.recyclerView.adapter = ConcatAdapter(
            adapterPopularItems,
            RVAdapterSectionHeader(getString(R.string.SettingsCurrency_Other)),
            adapterOtherItems
        )
    }

    override fun onSelectItem(item: CurrencyViewItemWrapper) {
        viewModel.setBaseCurrency(item.currency)
    }
}

class RVAdapterSectionHeader(val title: String) :
    RecyclerView.Adapter<RVAdapterSectionHeader.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ViewHolderSectionHeaderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(title)
    override fun getItemCount() = 1

    class ViewHolder(private val binding: ViewHolderSectionHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(title: String) {
            binding.title.text = title
        }
    }
}

class RVAdapter(val items: List<CurrencyViewItemWrapper>, val listener: ViewHolder.Listener) :
    RecyclerView.Adapter<RVAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            ViewHolderItemWithCheckmarkBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ), listener
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class ViewHolder(
        private val binding: ViewHolderItemWithCheckmarkBinding,
        private val listener: Listener
    ) :
        RecyclerView.ViewHolder(binding.root) {
        private var item: CurrencyViewItemWrapper? = null

        interface Listener {
            fun onSelectItem(item: CurrencyViewItemWrapper)
        }

        init {
            binding.wrapper.setOnSingleClickListener {
                item?.let {
                    listener.onSelectItem(it)
                }
            }
        }

        fun bind(item: CurrencyViewItemWrapper) {
            this.item = item

            binding.image.setImageResource(
                LayoutHelper.getCurrencyDrawableResource(
                    binding.wrapper.context,
                    item.currency.code.toLowerCase(Locale.ENGLISH)
                )
            )
            binding.title.text = item.currency.code
            binding.subtitle.text = item.currency.symbol
            binding.checkmarkIcon.isVisible = item.selected

            binding.wrapper.setBackgroundResource(item.listPosition.getBackground())
        }

    }
}
