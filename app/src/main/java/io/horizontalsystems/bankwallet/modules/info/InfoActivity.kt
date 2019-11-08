package io.horizontalsystems.bankwallet.modules.info

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.activity_app_status.toolbar
import kotlinx.android.synthetic.main.activity_info.*

class InfoActivity : BaseActivity() {
    private lateinit var presenter: InfoPresenter
    private lateinit var view: InfoView
    private lateinit var router: InfoRouter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_info)
        setSupportActionBar(toolbar)

        val infoParams = intent.getParcelableExtra(InfoModule.KEY_INFO_PARAMETERS) as? InfoModule.InfoParameters
                ?: run { finish(); return }

        presenter = ViewModelProvider(this, InfoModule.Factory()).get(InfoPresenter::class.java)
        view = presenter.view as InfoView
        router = presenter.router as InfoRouter

        view.titleLiveEvent.observe(this, Observer { title ->
            collapsingToolbar.title = title
        })

        view.descriptionLiveEvent.observe(this, Observer { description ->
            textDescription.text = description
        })

        router.goBackLiveEvent.observe(this, Observer {
            onBackPressed()
        })

        presenter.onLoad(infoParams)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.info_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menuCopy -> {
                presenter.onClickClose()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}
