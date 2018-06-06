package org.grouvi.wallet.modules.confirmMnemonic

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_confirm_mnemonic.*
import org.grouvi.wallet.R

class ConfirmMnemonicActivity : AppCompatActivity() {

    private lateinit var viewModel: ConfirmMnemonicViewModel
    private var wordPosition: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_mnemonic)

        buttonConfirm.setOnClickListener {
            wordPosition?.let {
                viewModel.presenter.submit(it, inputWord.text.toString())
            }
        }

        viewModel = ViewModelProviders.of(this).get(ConfirmMnemonicViewModel::class.java)
        viewModel.init()

        viewModel.wordPositionLiveData.observe(this, Observer { wordPosition ->
            wordPosition?.let {
                hint.text = "Enter word #${it + 1}:"
                this.wordPosition = it
            }
        })

        viewModel.errorLiveData.observe(this, Observer { errorId ->
            if (errorId != null) {
                textViewError.visibility = View.VISIBLE
                textViewError.text = getString(errorId)
            } else {
                textViewError.visibility = View.GONE
            }
        })

    }

}