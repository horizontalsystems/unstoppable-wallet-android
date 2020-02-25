package io.horizontalsystems.bankwallet.modules.torpage

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.CompoundButton
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.core.CoreActivity
import kotlinx.android.synthetic.main.activity_tor_page.*

class TorPageActivity: CoreActivity() {

    lateinit var presenter: TorPagePresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tor_page)
        setSupportActionBar(toolbar)

        presenter = ViewModelProvider(this, TorPageModule.Factory())
                .get(TorPagePresenter::class.java)

        presenter.viewDidLoad()

        torEnableSwitch.switchOnCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            presenter.onTorSwitch(isChecked)
        }

        (presenter.view as TorPageView).setTorSwitch.observe(this, Observer {
            torEnableSwitch.switchIsChecked = it
        })

        (presenter.router as TorPageRouter).closePage.observe(this, Observer {
            onBackPressed()
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.tor_page_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuClose -> {
                presenter.onClose()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
