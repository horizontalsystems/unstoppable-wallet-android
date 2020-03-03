package io.horizontalsystems.bankwallet.modules.contact.appstatus

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.activity_app_status.*
import java.util.*

class AppStatusActivity : BaseActivity() {

    private lateinit var presenter: AppStatusPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_status)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        presenter = ViewModelProvider(this, AppStatusModule.Factory()).get(AppStatusPresenter::class.java)

        observeView(presenter.view as AppStatusView)

        presenter.viewDidLoad()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_status_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.menuCopy ->  {
                presenter.didTapCopy(textAppStatus.text.toString())
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun observeView(view: AppStatusView) {
        view.appStatusLiveData.observe(this, Observer { appStatusMap ->
            textAppStatus.text = formatMapToString(appStatusMap)
        })

        view.showCopiedLiveEvent.observe(this, Observer {
            HudHelper.showSuccessMessage(this, R.string.Hud_Text_Copied)
        })
    }

    private fun formatMapToString(status: Map<String, Any>?, indentation: String = "", bullet: String = "", level: Int = 0): String? {
        if (status == null)
            return null

        val sb = StringBuilder()
        status.toList().forEach { (key, value) ->
            val title = "$indentation$bullet$key"
            when (value) {
                is Date -> {
                    val date = DateHelper.formatDate(value, "MMM d, yyyy, HH:mm")
                    sb.appendln("$title: $date")
                }
                is Map<*, *> -> {
                    val formattedValue = formatMapToString(value as? Map<String, Any>, "\t\t$indentation", " - ", level + 1)
                    sb.append("$title:\n$formattedValue${if (level < 2) "\n" else ""}")
                }
                else -> {
                    sb.appendln("$title: $value")
                }
            }
        }

        val statusString = sb.trimEnd()

        return if (statusString.isEmpty()) "" else "$statusString\n"
    }

}
