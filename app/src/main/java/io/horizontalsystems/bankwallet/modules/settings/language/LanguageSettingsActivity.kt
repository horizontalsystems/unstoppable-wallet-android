package io.horizontalsystems.bankwallet.modules.settings.language

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.main.MainActivity
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.ui.view.ViewHolderProgressbar
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_language_settings.*
import kotlinx.android.synthetic.main.view_holder_language_item.*

class LanguageSettingsActivity: BaseActivity(), LanguageSettingsAdapter.Listener {

    private lateinit var viewModel: LanguageSettingsViewModel
    private var adapter: LanguageSettingsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(LanguageSettingsViewModel::class.java)
        viewModel.init()

        setContentView(R.layout.activity_language_settings)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.back)

        adapter = LanguageSettingsAdapter(this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.languageItems.observe(this, Observer { items ->
            items?.let {
                adapter?.items = it
                adapter?.notifyDataSetChanged()
            }
        })

        viewModel.reloadAppLiveEvent.observe(this, Observer {
            MainModule.startAsNewTask(this, MainActivity.SETTINGS_TAB_POSITION)
        })

        viewModel.titleLiveDate.observe(this, Observer { title ->
            title?.let {
                supportActionBar?.title = getString(it)
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemClick(item: LanguageItem) {
        viewModel.delegate.didSelect(item)
    }
}

class LanguageSettingsAdapter(private var listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val VIEW_TYPE_ITEM = 1
    private val VIEW_TYPE_LOADING = 2

    interface Listener {
        fun onItemClick(item: LanguageItem)
    }

    var items = listOf<LanguageItem>()

    override fun getItemCount() = if (items.isEmpty()) 1 else items.size

    override fun getItemViewType(position: Int): Int = if(items.isEmpty()) {
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
            is ViewHolderLanguageItem -> holder.bind(items[position]) { listener.onItemClick(items[position]) }
        }
    }

}

class ViewHolderLanguageItem(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: LanguageItem, onClick: () -> (Unit)) {

        containerView.setOnSingleClickListener { onClick.invoke() }
        title.text = item.locale.getDisplayLanguage(item.locale).capitalize()
        subtitle.text = item.locale.displayName.capitalize()
        checkmarkIcon.visibility = if (item.current) View.VISIBLE else View.GONE
    }

    companion object {
        val layoutResourceId: Int
            get() = R.layout.view_holder_language_item
    }
}
