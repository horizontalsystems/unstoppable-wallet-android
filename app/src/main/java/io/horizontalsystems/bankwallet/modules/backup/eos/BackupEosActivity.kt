package io.horizontalsystems.bankwallet.modules.backup.eos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.views.TopMenuItem
import kotlinx.android.synthetic.main.activity_backup_eos.*

class BackupEosActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup_eos)
        shadowlessToolbar.bind(getString(R.string.Backup_DisplayTitle), TopMenuItem(R.drawable.ic_back, onClick = { onBackPressed() }))

        val account = intent.getStringExtra(ACCOUNT) ?: run { finish(); return }
        val activePrivateKey = intent.getStringExtra(ACTIVE_PRIVATE_KEY) ?: run { finish(); return }

        bind(account, activePrivateKey)
    }

    private fun bind(account: String, privateKey: String) {
        eosAccount.text = account
        eosAccount.bind { onCopy(account) }

        eosActivePrivateKey.text = privateKey
        eosActivePrivateKey.bind { onCopy(privateKey) }

        btnClose.setOnClickListener {
            setResult(BackupEosModule.RESULT_SHOW)
            finish()
        }

        imgQrCode.setImageBitmap(TextHelper.getQrCodeBitmap(privateKey, 120F))
    }

    private fun onCopy(text: String) {
        TextHelper.copyText(text)
        HudHelper.showSuccessMessage(this, R.string.Hud_Text_Copied)
    }

    companion object {
        const val ACCOUNT = "account"
        const val ACTIVE_PRIVATE_KEY = "active_private_key"

        fun start(context: AppCompatActivity, account: String, activePrivateKey: String) {
            val intent = Intent(context, BackupEosActivity::class.java).apply {
                putExtra(ACCOUNT, account)
                putExtra(ACTIVE_PRIVATE_KEY, activePrivateKey)
            }

            context.startActivityForResult(intent, ModuleCode.BACKUP_EOS)
        }
    }
}
