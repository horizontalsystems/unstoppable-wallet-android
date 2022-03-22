package io.horizontalsystems.bankwallet.modules.settings.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.databinding.FragmentThemeSwitcherBinding
import io.horizontalsystems.bankwallet.databinding.ViewHolderThemeSwitchItemBinding
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.setNavigationResult

class ThemeSwitchFragment : BaseFragment() {

    private val viewModel by viewModels<ThemeSwitchViewModel> { ThemeSwitchModule.Factory() }

    private var _binding: FragmentThemeSwitcherBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentThemeSwitcherBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            closeFragment()
        }

        binding.toolbar.setNavigationOnClickListener {
            closeFragment()
        }

        val adapter = ThemeSwitchAdapter(::onItemClick)
        binding.recyclerView.adapter = adapter

        viewModel.itemsLiveData.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        viewModel.changeThemeEvent.observe(viewLifecycleOwner, { themeType ->
            val nightMode = when (themeType) {
                ThemeType.Light -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeType.Dark -> AppCompatDelegate.MODE_NIGHT_YES
                ThemeType.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                else -> return@observe
            }

            AppCompatDelegate.setDefaultNightMode(nightMode)
        })
    }

    private fun closeFragment() {
        setNavigationResult(THEME_CHANGE, bundleOf())
        findNavController().popBackStack()
    }

    fun onItemClick(item: ThemeViewItem) {
        viewModel.onThemeClick(item)
    }

    companion object{
        const val THEME_CHANGE = "theme_change"
    }

}

class ThemeSwitchAdapter(private val onItemClick: (ThemeViewItem) -> Unit) :
    ListAdapter<ThemeViewItem, ThemeSwitchAdapter.ViewHolder>(diffCallback) {

    class ViewHolder(
        private val binding: ViewHolderThemeSwitchItemBinding,
        onItemClick: (ThemeViewItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private var item: ThemeViewItem? = null

        init {
            itemView.setOnSingleClickListener {
                item?.let {
                    onItemClick(it)
                }
            }
        }

        fun bind(item: ThemeViewItem) {
            this.item = item

            binding.icon.setImageResource(item.themeType.getIcon())

            binding.title.setText(item.themeType.getTitle())

            binding.checkmarkIcon.isVisible = item.checked

            binding.wrapper.setBackgroundResource(item.listPosition.getBackground())
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ViewHolderThemeSwitchItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ), onItemClick
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<ThemeViewItem>() {
            override fun areItemsTheSame(oldItem: ThemeViewItem, newItem: ThemeViewItem): Boolean {
                return oldItem.themeType == newItem.themeType
            }

            override fun areContentsTheSame(
                oldItem: ThemeViewItem,
                newItem: ThemeViewItem
            ): Boolean {
                return oldItem.checked == newItem.checked
            }
        }
    }
}
