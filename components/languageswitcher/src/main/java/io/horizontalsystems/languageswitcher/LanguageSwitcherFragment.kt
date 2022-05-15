package io.horizontalsystems.languageswitcher

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.setNavigationResult
import io.horizontalsystems.core.setOnSingleClickListener
import io.horizontalsystems.languageswitcher.databinding.FragmentLanguageSettingsBinding
import io.horizontalsystems.views.ViewHolderProgressbar
import io.horizontalsystems.views.databinding.ViewHolderItemWithCheckmarkBinding
import io.horizontalsystems.views.databinding.ViewHolderProgressbarItemBinding

class LanguageSettingsFragment : Fragment(), LanguageSwitcherAdapter.Listener {

    private lateinit var presenter: LanguageSwitcherPresenter

    private var _binding: FragmentLanguageSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLanguageSettingsBinding.inflate(inflater, container, false)
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

        presenter = ViewModelProvider(
            this,
            LanguageSwitcherModule.Factory()
        ).get(LanguageSwitcherPresenter::class.java)

        val presenterView = presenter.view as LanguageSwitcherView
        val presenterRouter = presenter.router as LanguageSwitcherRouter

        val adapter = LanguageSwitcherAdapter(this)

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        presenterView.languageItems.observe(viewLifecycleOwner, Observer {
            adapter.items = it
            adapter.notifyDataSetChanged()
        })

        presenterRouter.reloadAppLiveEvent.observe(viewLifecycleOwner, Observer {
            setNavigationResult(LANGUAGE_CHANGE, bundleOf())

            findNavController().popBackStack()
        })

        presenterRouter.closeLiveEvent.observe(viewLifecycleOwner, Observer {
            activity?.onBackPressed()
        })

        presenter.viewDidLoad()
    }

    override fun onItemClick(position: Int) {
        presenter.didSelect(position)
    }

    companion object {
        const val LANGUAGE_CHANGE = "language_change"
    }
}

class LanguageSwitcherAdapter(private var listener: Listener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val VIEW_TYPE_ITEM = 1
    private val VIEW_TYPE_LOADING = 2

    interface Listener {
        fun onItemClick(position: Int)
    }

    var items = listOf<LanguageViewItem>()

    override fun getItemCount() = if (items.isEmpty()) 1 else items.size

    override fun getItemViewType(position: Int): Int = if (items.isEmpty()) {
        VIEW_TYPE_LOADING
    } else {
        VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ITEM -> ViewHolderLanguageItem(
                ViewHolderItemWithCheckmarkBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
            else -> ViewHolderProgressbar(
                ViewHolderProgressbarItemBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderLanguageItem -> holder.bind(items[position]) {
                listener.onItemClick(
                    position
                )
            }
        }
    }

}

class ViewHolderLanguageItem(private val binding: ViewHolderItemWithCheckmarkBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: LanguageViewItem, onClick: () -> (Unit)) {
        binding.wrapper.setOnSingleClickListener { onClick.invoke() }
        binding.image.setImageResource(
            getLangDrawableResource(
                binding.wrapper.context,
                item.language
            )
        )

        binding.title.text = item.name
        binding.subtitle.text = item.nativeName
        binding.checkmarkIcon.isVisible = item.current
        binding.wrapper.setBackgroundResource(item.listPosition.getBackground())
    }

    private fun getLangDrawableResource(context: Context, langCode: String): Int {
        return context.resources.getIdentifier("lang_$langCode", "drawable", context.packageName)
    }

}
