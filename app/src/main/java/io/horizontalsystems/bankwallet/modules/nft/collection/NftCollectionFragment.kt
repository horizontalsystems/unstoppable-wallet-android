package io.horizontalsystems.bankwallet.modules.nft.collection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.navigation.navGraphViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.databinding.FragmentCollectionBinding
import io.horizontalsystems.bankwallet.modules.nft.collection.assets.NftCollectionAssetsFragment
import io.horizontalsystems.bankwallet.modules.nft.collection.events.NftCollectionEventsFragment
import io.horizontalsystems.bankwallet.modules.nft.collection.overview.NftCollectionOverviewFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.bankwallet.ui.compose.components.Tabs
import io.horizontalsystems.core.findNavController

class NftCollectionFragment : BaseFragment() {

    private val viewModel by navGraphViewModels<NftCollectionViewModel>(R.id.nftCollectionFragment) {
        NftCollectionModule.Factory(requireArguments().getString(collectionUidKey)!!)
    }

    private var _binding: FragmentCollectionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuClose -> {
                    findNavController().popBackStack()
                    true
                }
                else -> false
            }
        }

        binding.viewPager.adapter = NftCollectionTabsAdapter(this.childFragmentManager, viewLifecycleOwner.lifecycle)
        binding.viewPager.isUserInputEnabled = false

        viewModel.selectedTabLiveData.observe(viewLifecycleOwner) { selectedTab ->
            binding.viewPager.setCurrentItem(viewModel.tabs.indexOf(selectedTab), false)
            setTabs(selectedTab)
        }

        binding.tabsCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )
    }

    private fun setTabs(selectedTab: NftCollectionModule.Tab) {
        val tabItems = viewModel.tabs.map {
            TabItem(getString(it.titleResId), it == selectedTab, it)
        }
        binding.tabsCompose.setContent {
            ComposeAppTheme {
                Tabs(tabItems) { item ->
                    viewModel.onSelect(item)
                }
            }
        }
    }

    companion object {
        private const val collectionUidKey = "collection_uid"

        fun prepareParams(collectionUid: String) = bundleOf(collectionUidKey to collectionUid)
    }
}

class NftCollectionTabsAdapter(fm: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fm, lifecycle) {

    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> NftCollectionOverviewFragment()
            1 -> NftCollectionAssetsFragment()
            2 -> NftCollectionEventsFragment()
            else -> throw IllegalStateException()
        }
    }

}
