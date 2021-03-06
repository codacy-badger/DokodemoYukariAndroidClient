package com.github.windsekirun.yukarisynthesizer.main.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.github.windsekirun.baseapp.module.recycler.BaseRecyclerAdapter
import com.github.windsekirun.yukarisynthesizer.R
import com.github.windsekirun.yukarisynthesizer.core.item.PhonomeItem
import com.github.windsekirun.yukarisynthesizer.databinding.PhonomeListItemBinding
import com.github.windsekirun.yukarisynthesizer.main.event.ClickPhonomeItem

/**
 * DokodemoYukariAndroidClient
 * Class: StoryItemAdapter
 * Created by Pyxis on 2018-11-27.
 *
 *
 * Description:
 */
class PhonomeListAdapter : BaseRecyclerAdapter<PhonomeItem, PhonomeListItemBinding>() {
    var phonomeClickAdapter: OnPhonomeClickAdapter? = null

    override fun bind(binding: PhonomeListItemBinding, item: PhonomeItem, position: Int) {
        binding.item = item
    }

    override fun onClickedItem(binding: PhonomeListItemBinding, item: PhonomeItem, position: Int) {
        if (phonomeClickAdapter != null) {
            phonomeClickAdapter?.onClick(item)
        } else {
            postEvent(ClickPhonomeItem(item))
        }
    }

    override fun onLongClickedItem(binding: PhonomeListItemBinding, item: PhonomeItem, position: Int): Boolean {
        return false
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup): PhonomeListItemBinding {
        return DataBindingUtil.inflate(inflater, R.layout.phonome_list_item, parent, false)
    }

    interface OnPhonomeClickAdapter {
        fun onClick(phonomeItem: PhonomeItem)
    }
}