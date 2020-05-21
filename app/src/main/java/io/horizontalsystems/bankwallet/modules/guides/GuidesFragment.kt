package io.horizontalsystems.bankwallet.modules.guides

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.guideview.GuideViewerActivity
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_guides.*
import kotlinx.android.synthetic.main.view_holder_guide_preview.*


class GuidesFragment : Fragment(), GuidesAdapter.Listener {

    private lateinit var presenter: GuidesPresenter
    private lateinit var guidesView: GuidesView
    private lateinit var adapter: GuidesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_guides, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter = ViewModelProvider(this, GuidesModule.Factory()).get(GuidesPresenter::class.java)
        guidesView = presenter.view as GuidesView
        presenter.onLoad()
        adapter = GuidesAdapter(this)

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

    override fun onItemClick(item: GuideItem) {
        presenter.onGuideClick(item)
    }

    private fun observeLiveData() {
        guidesView.openGuide.observe(viewLifecycleOwner, Observer { guideItem ->
            startActivity(Intent(activity, GuideViewerActivity::class.java))
        })
    }

}

class GuidesAdapter(private var listener: Listener) : RecyclerView.Adapter<ViewHolderGuide>(), ViewHolderGuide.ClickListener {

    interface Listener {
        fun onItemClick(item: GuideItem)
    }

    var items = listOf<GuideItem>()

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderGuide {
        return ViewHolderGuide(inflate(parent, R.layout.view_holder_guide_preview), this)
    }

    override fun onBindViewHolder(holder: ViewHolderGuide, position: Int) {
        holder.bind(items[position])
    }

    override fun onClick(position: Int) {
        listener.onItemClick(items[position])
    }
}

class ViewHolderGuide(override val containerView: View, private val listener: ClickListener) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    interface ClickListener {
        fun onClick(position: Int)
    }

    init {
        guideWrapper.setOnSingleClickListener { listener.onClick(adapterPosition) }
    }

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
