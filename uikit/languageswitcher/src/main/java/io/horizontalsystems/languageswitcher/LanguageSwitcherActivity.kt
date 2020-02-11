package io.horizontalsystems.languageswitcher

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.core.CoreActivity
import io.horizontalsystems.core.setOnSingleClickListener
import io.horizontalsystems.views.TopMenuItem
import io.horizontalsystems.views.ViewHolderProgressbar
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_language_settings.*

class LanguageSettingsActivity : CoreActivity(), LanguageSwitcherAdapter.Listener {

    private lateinit var presenter: LanguageSwitcherPresenter
    private var adapter: LanguageSwitcherAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = ViewModelProvider(this, LanguageSwitcherModule.Factory()).get(LanguageSwitcherPresenter::class.java)

        val presenterView = presenter.view as LanguageSwitcherView
        val presenterRouter = presenter.router as LanguageSwitcherRouter

        setContentView(R.layout.activity_language_settings)

        shadowlessToolbar.bind(
                title = getString(R.string.SettingsLanguage_Title),
                leftBtnItem = TopMenuItem(R.drawable.ic_back, onClick = { onBackPressed() })
        )

        adapter = LanguageSwitcherAdapter(this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        presenterView.languageItems.observe(this, Observer { items ->
            items?.let {
                adapter?.items = it
                adapter?.notifyDataSetChanged()
            }
        })

        presenterRouter.reloadAppLiveEvent.observe(this, Observer {
            // MainModule.startAsNewTask(this, MainActivity.SETTINGS_TAB_POSITION)
            setResult(LanguageSwitcherModule.LANGUAGE_CHANGED)
            finish()
        })

        presenterRouter.closeLiveEvent.observe(this, Observer {
            finish()
        })

        presenter.viewDidLoad()
    }

    override fun onItemClick(position: Int) {
        presenter.didSelect(position)
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

        title.text = item.nativeName
        subtitle.text = item.name
        checkmarkIcon.visibility = if (item.current) View.VISIBLE else View.GONE
    }

    private fun getLangDrawableResource(context: Context, langCode: String): Int {
        return context.resources.getIdentifier("lang_$langCode", "drawable", context.packageName)
    }

    companion object {
        val layoutResourceId: Int
            get() = R.layout.view_holder_item_with_checkmark
    }
}
