package io.horizontalsystems.bankwallet.modules.settings.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_theme_switcher.*
import kotlinx.android.synthetic.main.view_holder_theme_switch_item.*

class ThemeSwitchFragment : BaseFragment() {

    private val viewModel by viewModels<ThemeSwitchViewModel> { ThemeSwitchModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_theme_switcher, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val adapter = ThemeSwitchAdapter(::onItemClick)
        recyclerView.adapter = adapter

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

    fun onItemClick(item: ThemeViewItem) {
        viewModel.onThemeClick(item)
    }

}

class ThemeSwitchAdapter(private val onItemClick: (ThemeViewItem) -> Unit) : ListAdapter<ThemeViewItem, ThemeSwitchAdapter.ViewHolder>(diffCallback) {

    class ViewHolder(override val containerView: View, onItemClick: (ThemeViewItem) -> Unit) : RecyclerView.ViewHolder(containerView), LayoutContainer {
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

            icon.setImageResource(item.themeType.getIcon())

            title.setText(item.themeType.getTitle())

            checkmarkIcon.isVisible = item.checked

            containerView.setBackgroundResource(item.listPosition.getBackground())
        }

        companion object {
            fun create(parent: ViewGroup, onItemClick: (ThemeViewItem) -> Unit) = ViewHolder(inflate(parent, R.layout.view_holder_theme_switch_item), onItemClick)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder.create(parent, onItemClick)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<ThemeViewItem>() {
            override fun areItemsTheSame(oldItem: ThemeViewItem, newItem: ThemeViewItem): Boolean {
                return oldItem.themeType == newItem.themeType
            }

            override fun areContentsTheSame(oldItem: ThemeViewItem, newItem: ThemeViewItem): Boolean {
                return oldItem.checked == newItem.checked
            }
        }
    }
}
