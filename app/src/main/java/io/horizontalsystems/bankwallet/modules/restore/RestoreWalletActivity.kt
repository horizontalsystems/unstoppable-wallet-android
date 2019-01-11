package io.horizontalsystems.bankwallet.modules.restore

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.GridLayoutManager
import android.view.Menu
import android.view.MenuItem
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Utils
import io.horizontalsystems.bankwallet.lib.EditTextViewHolder
import io.horizontalsystems.bankwallet.lib.WordsInputAdapter
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import io.horizontalsystems.bankwallet.ui.dialogs.BottomConfirmAlert
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import kotlinx.android.synthetic.main.activity_restore_wallet.*

class RestoreWalletActivity : BaseActivity(), BottomConfirmAlert.Listener {

    private lateinit var viewModel: RestoreViewModel

    private val words = MutableList(12, { "" })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore_wallet)

        if (Utils.isUsingCustomKeyboard(this) ) {
            showCustomKeyboardAlert()
        }

        setSupportActionBar(toolbar)

        supportActionBar?.title = getString(R.string.Restore_Title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.back)

        viewModel = ViewModelProviders.of(this).get(RestoreViewModel::class.java)
        viewModel.init()

        viewModel.errorLiveData.observe(this, Observer { errorId ->
            errorId?.let {
                HudHelper.showErrorMessage(it)
            }
        })

        viewModel.navigateToSetPinLiveEvent.observe(this, Observer {
            PinModule.startForSetPin(this)
        })

        viewModel.keyStoreSafeExecute.observe(this, Observer { triple ->
            triple?.let {
                val (action, onSuccess, onFailure) = it
                safeExecuteWithKeystore(action, onSuccess, onFailure)
            }
        })

        viewModel.showConfirmationDialogLiveEvent.observe(this, Observer {
            val confirmationList = mutableListOf(
                    R.string.Backup_Confirmation_Understand,
                    R.string.Backup_Confirmation_DeleteAppWarn,
                    R.string.Backup_Confirmation_LockAppWarn
            )
            BottomConfirmAlert.show(this, confirmationList, this)
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
        LayoutHelper.tintMenuIcons(menu, ContextCompat.getColor(this, R.color.yellow_crypto))

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_done -> {
            viewModel.delegate.restoreDidClick(words)
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onConfirmationSuccess() {
        viewModel.delegate.didConfirm(words)
    }
}
