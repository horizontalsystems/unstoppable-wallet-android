package org.grouvi.wallet.modules.generateMnemonic

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_generate_mnemonic.*
import org.grouvi.wallet.R
import org.grouvi.wallet.modules.confirmMnemonic.ConfirmMnemonicModule

class GenerateMnemonicActivity : AppCompatActivity() {

    private lateinit var viewModel: GenerateMnemonicViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_mnemonic)

        val wordsAdapter = WordsAdapter(View.OnClickListener {
            viewModel.openMnemonicWordsConfirmation()
        })
        mnemonicWordsView.adapter = wordsAdapter
        mnemonicWordsView.layoutManager = LinearLayoutManager(this)


        viewModel = ViewModelProviders.of(this).get(GenerateMnemonicViewModel::class.java)
        viewModel.init()

        viewModel.mnemonicWords.observe(this, Observer { mnemonicWords: List<String>? ->
            mnemonicWords?.let {
                wordsAdapter.items = it
                wordsAdapter.notifyDataSetChanged()
            }
        })

        viewModel.openMnemonicWordsConfirmationLiveEvent.observe(this, Observer {
            ConfirmMnemonicModule.start(this)
        })
    }

}

class WordsAdapter(private val buttonClickListener: View.OnClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items: List<String> = listOf()

    companion object {
        const val viewTypeWord = 1
        const val viewTypeButton = 2
    }

    override fun getItemViewType(position: Int) = when {
        position < items.size -> viewTypeWord
        else -> viewTypeButton
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return if (viewType == viewTypeWord) {
            ViewHolderWord(inflater.inflate(android.R.layout.simple_list_item_1, parent, false) as TextView)
        } else {
            ViewHolderButton(inflater.inflate(R.layout.view_holder_button, parent, false) as Button, buttonClickListener)
        }
    }

    override fun getItemCount() = items.size + 1

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderWord -> holder.textView.text = "${position + 1}. ${items[position]}"
        }
    }

    class ViewHolderWord(val textView: TextView) : RecyclerView.ViewHolder(textView)
    class ViewHolderButton(button: Button, private val buttonClickListener: View.OnClickListener) : RecyclerView.ViewHolder(button) {
        init {
            button.setOnClickListener(buttonClickListener)
        }
    }

}