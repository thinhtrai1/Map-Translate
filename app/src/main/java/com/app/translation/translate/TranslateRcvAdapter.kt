package com.app.translation.translate

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.translation.databinding.ItemTranslateBinding
import java.util.*
import kotlin.collections.ArrayList

class TranslateRcvAdapter(private val onPlay: (String, Locale) -> Unit) : RecyclerView.Adapter<TranslateRcvAdapter.ViewHolder>() {
    private val list = ArrayList<TranslateMessage>()

    fun add(message: TranslateMessage): Int {
        list.add(message)
        return list.lastIndex.also {
            notifyItemInserted(it)
        }
    }

    override fun getItemCount() = list.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemTranslateBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder.binding) {
            list[position].let {
                tvSource.text = it.source
                tvTarget.text = it.target
                tvChina.text = it.china
                tvJapan.text = it.japan
                tvSource.setOnClickListener { _ ->
                    onPlay(it.source, it.sourceLocale)
                }
                tvTarget.setOnClickListener { _ ->
                    onPlay(it.target, it.targetLocale)
                }
                tvChina.setOnClickListener { _ ->
                    onPlay(it.china, Locale.CHINA)
                }
                tvJapan.setOnClickListener { _ ->
                    onPlay(it.japan, Locale.JAPAN)
                }
            }
        }
    }

    class ViewHolder(val binding: ItemTranslateBinding) : RecyclerView.ViewHolder(binding.root)
}