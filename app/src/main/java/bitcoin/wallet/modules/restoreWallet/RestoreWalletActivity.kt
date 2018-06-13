package bitcoin.wallet.modules.restoreWallet

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import bitcoin.wallet.R
import bitcoin.wallet.lib.EditTextViewHolder
import bitcoin.wallet.lib.WordsInputAdapter
import bitcoin.wallet.modules.dashboard.DashboardModule
import kotlinx.android.synthetic.main.activity_restore_wallet.*

class RestoreWalletActivity : AppCompatActivity() {

    private lateinit var viewModel: RestoreWalletViewModel

    private val words = MutableList(12, {""})

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore_wallet)

        setSupportActionBar(toolbar)

        supportActionBar?.title = getString(R.string.enter_paper_keys_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProviders.of(this).get(RestoreWalletViewModel::class.java)
        viewModel.init()

        viewModel.errorLiveData.observe(this, Observer { errorId ->
            if (errorId == null) {
                textError.visibility = View.GONE
            } else {
                textError.visibility = View.VISIBLE
                textError.text = getString(errorId)
            }
        })

        viewModel.navigateToMainScreenLiveEvent.observe(this, Observer {
            DashboardModule.start(this)
        })

        recyclerInputs.adapter = WordsInputAdapter(object : EditTextViewHolder.WordsChangedListener {
            override fun set(position: Int, value: String) {
                words[position] = value
            }
        })
        recyclerInputs.layoutManager = GridLayoutManager(this, 2)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.restore_wallet, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_done -> {
            Log.e("AAA", "Words: ${words.joinToString()}")
            viewModel.presenter.onRestoreButtonClick(words)
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

}
