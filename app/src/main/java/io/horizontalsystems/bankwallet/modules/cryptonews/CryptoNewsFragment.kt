package io.horizontalsystems.bankwallet.modules.cryptonews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.fragment_crypto_news.*

class CryptoNewsFragment(private val coinCode: String) : Fragment() {

    private lateinit var presenter: CryptoNewsPresenter
    private lateinit var cryptoNewsView: CryptoNewsView
    private lateinit var adapter: CryptoNewsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_crypto_news, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        presenter = ViewModelProvider(this, CryptoNewsModule.Factory(coinCode)).get(CryptoNewsPresenter::class.java)
        cryptoNewsView = presenter.view as CryptoNewsView
        adapter = CryptoNewsAdapter()

        newsRecyclerView.adapter = adapter
        observeEvents()

        presenter.onLoad()
    }

    private fun observeEvents() {
        cryptoNewsView.showNews.observe(this, Observer { items ->
            adapter.submitList(items)
        })

        cryptoNewsView.showSpinner.observe(this, Observer { show ->
            spinner.visibility = if (show) View.VISIBLE else View.GONE
            notAvailable.visibility = View.GONE
        })

        cryptoNewsView.showError.observe(this, Observer {
            notAvailable.visibility = View.VISIBLE
        })
    }
}
