package org.grouvi.wallet

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button_create_wallet).setOnClickListener {
            createNewWallet()
        }
    }

    private fun createNewWallet() {

    }


}
