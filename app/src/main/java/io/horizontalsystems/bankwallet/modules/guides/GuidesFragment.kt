package io.horizontalsystems.bankwallet.modules.guides

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_guides.*
import kotlinx.android.synthetic.main.view_holder_guide_preview.*


class GuidesFragment : Fragment() {

    private lateinit var presenter: GuidesPresenter
    private lateinit var adapter: GuidesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_guides, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter = ViewModelProvider(this, GuidesModule.Factory()).get(GuidesPresenter::class.java)
        presenter.onLoad()
        adapter = GuidesAdapter()

        adapter.items = listOf(GuideItem("Libra and its hidden secrets", "Guide", 30),
                GuideItem("Bitcoin full truth guide", "Guide", 15),
                GuideItem("Ethereum full truth guide, and some more details", "Guide", 20),
                GuideItem("Ethereum full truth guide", "Guide", 20),
                GuideItem("Ethereum full truth guide", "Guide", 20),
                GuideItem("Monero full guide", "Guide", 3))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = GridLayoutManager(context, 1)

        recyclerGuides.layoutManager = layoutManager
        recyclerGuides.adapter = adapter

        observeLiveData()
    }

    private fun observeLiveData() {

    }

}

class GuidesAdapter : RecyclerView.Adapter<ViewHolderNews>() {

    var items = listOf<GuideItem>()

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderNews {
        return ViewHolderNews(inflate(parent, R.layout.view_holder_guide_preview))
    }

    override fun onBindViewHolder(holder: ViewHolderNews, position: Int) {
        holder.bind(items[position])
    }
}

class ViewHolderNews(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    fun bind(item: GuideItem) {
        titleText.text = item.title
        categoryText.text = item.category
        timeText.text = "${item.readTimeMinutes} m"

        containerView.setOnClickListener {

        }
    }

}

data class GuideItem(
        val title: String,
        val category: String,
        val readTimeMinutes: Long,
        val imageUrl: String? = null
)
