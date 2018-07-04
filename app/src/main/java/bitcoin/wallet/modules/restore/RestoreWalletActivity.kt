package bitcoin.wallet.modules.restore

import android.app.Activity
import android.app.KeyguardManager
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import bitcoin.wallet.R
import bitcoin.wallet.lib.EditTextViewHolder
import bitcoin.wallet.lib.ErrorDialog
import bitcoin.wallet.lib.WordsInputAdapter
import bitcoin.wallet.modules.main.MainModule
import bitcoin.wallet.viewHelpers.LayoutHelper
import kotlinx.android.synthetic.main.activity_restore_wallet.*

class RestoreWalletActivity : AppCompatActivity() {

    private lateinit var viewModel: RestoreViewModel

    private val words = MutableList(12, { "" })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore_wallet)

        setSupportActionBar(toolbar)

        supportActionBar?.title = getString(R.string.enter_paper_keys_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProviders.of(this).get(RestoreViewModel::class.java)
        viewModel.init()

        viewModel.errorLiveData.observe(this, Observer { errorId ->
            errorId?.let {
                ErrorDialog(this, errorId).show()
            }
        })

        viewModel.navigateToMainScreenLiveEvent.observe(this, Observer {
            MainModule.start(this)
        })

        viewModel.authenticateToRestoreWallet.observe(this, Observer {
            val mKeyguardManager = this.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            val intent: Intent? = mKeyguardManager.createConfirmDeviceCredentialIntent("Authorization required", "")
            startActivityForResult(intent, AUTHENTICATE_TO_RESTORE_WALLET)
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
        menuInflater.inflate(R.menu.menu_restore_wallet, menu)
        LayoutHelper.tintMenuIcons(menu, ContextCompat.getColor(this, R.color.yellow))

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_done -> {
            Log.e("AAA", "Words: ${words.joinToString()}")
            viewModel.delegate.restoreDidClick(words)
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == AUTHENTICATE_TO_RESTORE_WALLET) {
                viewModel.delegate.restoreDidClick(words)
            }
        }
    }

    companion object {
        const val AUTHENTICATE_TO_RESTORE_WALLET = 1
    }

}
