package io.horizontalsystems.bankwallet.modules.main.tor

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.activity_tor_connection.*

class TorConnectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tor_connection)

        btnRetry.setOnClickListener {
            restartTor()
        }

        txDisableTor.setOnClickListener {
            disableTor()
        }

        setStatus(false, "Starting Tor ... ")
    }

    private fun setStatus(isError: Boolean, statusText: String) {

        imgTorStatus.visibility = if(!isError) View.VISIBLE else View.GONE
        pgTorStatus.visibility = if(!isError) View.VISIBLE else View.GONE
        imgTorStatusError.visibility = if(isError) View.VISIBLE else View.GONE
        txTorStatus.text = statusText

        btnRetry.isEnabled = isError
    }

    private fun restartTor(){

    }

    private fun disableTor(){

    }
}