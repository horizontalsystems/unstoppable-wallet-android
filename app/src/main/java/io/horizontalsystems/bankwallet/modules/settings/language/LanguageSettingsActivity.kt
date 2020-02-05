package io.horizontalsystems.bankwallet.modules.settings.language

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.main.MainActivity
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.views.LayoutHelper
import io.horizontalsystems.views.TopMenuItem
import io.horizontalsystems.views.ViewHolderProgressbar
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_language_settings.*
import kotlinx.android.synthetic.main.view_holder_item_with_checkmark.*

class LanguageSettingsActivity : BaseActivity(), LanguageSettingsAdapter.Listener {

    private lateinit var presenter: LanguageSettingsPresenter
    private var adapter: LanguageSettingsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = ViewModelProvider(this, LanguageSettingsModule.Factory()).get(LanguageSettingsPresenter::class.java)

        val presenterView = presenter.view as LanguageSettingsView
        val presenterRouter = presenter.router as LanguageSettingsRouter

        setContentView(R.layout.activity_language_settings)

        shadowlessToolbar.bind(
                title = getString(R.string.SettingsLanguage_Title),
                leftBtnItem = TopMenuItem(R.drawable.ic_back, onClick = { onBackPressed() })
        )

        adapter = LanguageSettingsAdapter(this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        presenterView.languageItems.observe(this, Observer { items ->
            items?.let {
                adapter?.items = it
                adapter?.notifyDataSetChanged()
            }
        })

        presenterRouter.reloadAppLiveEvent.observe(this, Observer {
            MainModule.startAsNewTask(this, MainActivity.SETTINGS_TAB_POSITION)
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

class LanguageSettingsAdapter(private var listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
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

        containerView.setOnSingleClickListener { onClick.invoke() }
        image.setImageResource(LayoutHelper.getLangDrawableResource(containerView.context, item.language))
        title.text = item.nativeName
        subtitle.text = item.name
        checkmarkIcon.visibility = if (item.current) View.VISIBLE else View.GONE
    }

    companion object {
        val layoutResourceId: Int
            get() = R.layout.view_holder_item_with_checkmark
    }
}
