package io.horizontalsystems.bankwallet.modules.settings.managekeys.views

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment

class ManageKeysDialog : BaseBottomSheetDialogFragment() {

    interface Listener {
        fun onClickBackupKey() {}
    }

    private lateinit var alertText: TextView
    private lateinit var primaryActionButton: Button
    private lateinit var secondaryActionButton: Button

    private var listener: Listener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setContentView(R.layout.fragment_bottom_manage_keys)

        val title = requireArguments().getString(TITLE_KEY)
        val subtitle = requireArguments().getString(SUBTITLE_KEY)
        val content = requireArguments().getString(CONTENT_KEY)

        setTitle(title)
        setSubtitle(subtitle)
        setHeaderIcon(R.drawable.ic_attention_red_24)

        alertText = view.findViewById(R.id.alertText)
        primaryActionButton = view.findViewById(R.id.createBtn)
        secondaryActionButton = view.findViewById(R.id.restoreBtn)

        alertText.text = content

        bindActions()
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    private fun bindActions() {
        secondaryActionButton.isVisible = false
        primaryActionButton.isVisible = true

        primaryActionButton.text = getString(R.string.ManageKeys_Backup)
        primaryActionButton.setOnClickListener {
            listener?.onClickBackupKey()
            dismiss()
        }
    }

    companion object {
        const val TITLE_KEY = "title_key"
        const val SUBTITLE_KEY = "subtitle_key"
        const val CONTENT_KEY = "content_key"


        fun show(fragmentManager: FragmentManager, title: String, subtitle: String, content: String) {
            val fragment = ManageKeysDialog().apply {
                arguments = Bundle(4).apply {
                    putString(TITLE_KEY, title)
                    putString(SUBTITLE_KEY, subtitle)
                    putString(CONTENT_KEY, content)
                }
            }

            val transaction = fragmentManager.beginTransaction()

            transaction.add(fragment, "bottom_create_key_dialog")
            transaction.commitAllowingStateLoss()
        }
    }
}
