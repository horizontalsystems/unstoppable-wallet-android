package org.grouvi.wallet.modules.generateMnemonic

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_generate_mnemonic.*
import org.grouvi.wallet.R

class GenerateMnemonicActivity : AppCompatActivity() {

    private lateinit var viewModel: GenerateMnemonicViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_mnemonic)

        val wordsAdapter = WordsAdapter()
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
    }

}

class WordsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var items: List<String> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val textView = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false) as TextView
        return ViewHolder(textView)

    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> holder.textView.text = "${position + 1}. ${items[position]}"
        }
    }

    class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

}