package io.horizontalsystems.bankwallet.modules.settings.appstatus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_app_status.*
import java.util.*

class AppStatusFragment : BaseFragment() {

    private val presenter by viewModels<AppStatusPresenter> { AppStatusModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_app_status, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuCopy -> {
                    presenter.didTapCopy(textAppStatus.text.toString())
                    true
                }
                else -> false
            }
        }

        observeView(presenter.view as AppStatusView)

        presenter.viewDidLoad()
    }

    private fun observeView(view: AppStatusView) {
        view.appStatusLiveData.observe(viewLifecycleOwner, Observer { appStatusMap ->
            textAppStatus.text = formatMapToString(appStatusMap)
        })

        view.showCopiedLiveEvent.observe(viewLifecycleOwner, Observer {
            activity?.let {
                HudHelper.showSuccessMessage(it.findViewById(android.R.id.content), R.string.Hud_Text_Copied)
            }
        })
    }

    @Suppress("UNCHECKED_CAST")
    private fun formatMapToString(status: Map<String, Any>?, indentation: String = "", bullet: String = "", level: Int = 0): String? {
        if (status == null)
            return null

        val sb = StringBuilder()
        status.toList().forEach { (key, value) ->
            val title = "$indentation$bullet$key"
            when (value) {
                is Date -> {
                    val date = DateHelper.formatDate(value, "MMM d, yyyy, HH:mm")
                    sb.appendLine("$title: $date")
                }
                is Map<*, *> -> {
                    val formattedValue = formatMapToString(value as? Map<String, Any>, "\t\t$indentation", " - ", level + 1)
                    sb.append("$title:\n$formattedValue${if (level < 2) "\n" else ""}")
                }
                else -> {
                    sb.appendLine("$title: $value")
                }
            }
        }

        val statusString = sb.trimEnd()

        return if (statusString.isEmpty()) "" else "$statusString\n"
    }
}
