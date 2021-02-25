package io.horizontalsystems.languageswitcher

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
import io.horizontalsystems.views.ListPosition
import io.horizontalsystems.views.ViewHolderProgressbar
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_language_settings.*

class LanguageSettingsFragment : Fragment(), LanguageSwitcherAdapter.Listener {

    private lateinit var presenter: LanguageSwitcherPresenter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_language_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        presenter = ViewModelProvider(this, LanguageSwitcherModule.Factory()).get(LanguageSwitcherPresenter::class.java)

        val presenterView = presenter.view as LanguageSwitcherView
        val presenterRouter = presenter.router as LanguageSwitcherRouter

        val adapter = LanguageSwitcherAdapter(this)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

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

class LanguageSwitcherAdapter(private var listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
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
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_ITEM -> ViewHolderLanguageItem(inflater.inflate(ViewHolderLanguageItem.layoutResourceId, parent, false))
            else -> ViewHolderProgressbar(inflater.inflate(ViewHolderProgressbar.layoutResourceId, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderLanguageItem -> holder.bind(items[position]) { listener.onItemClick(position) }
        }
    }

}

class ViewHolderLanguageItem(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: LanguageViewItem, onClick: () -> (Unit)) {
        val image = containerView.findViewById<ImageView>(R.id.image)
        val title = containerView.findViewById<TextView>(R.id.title)
        val subtitle = containerView.findViewById<TextView>(R.id.subtitle)
        val checkmarkIcon = containerView.findViewById<ImageView>(R.id.checkmarkIcon)

        containerView.setOnSingleClickListener { onClick.invoke() }
        image.setImageResource(getLangDrawableResource(containerView.context, item.language))

        title.text = item.name
        subtitle.text = item.nativeName
        checkmarkIcon.isVisible = item.current
        containerView.setBackgroundResource(item.listPosition.getBackground())
    }

    private fun getLangDrawableResource(context: Context, langCode: String): Int {
        return context.resources.getIdentifier("lang_$langCode", "drawable", context.packageName)
    }

    companion object {
        val layoutResourceId: Int
            get() = R.layout.view_holder_item_with_checkmark
    }
}
