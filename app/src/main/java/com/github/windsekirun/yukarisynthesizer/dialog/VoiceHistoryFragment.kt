package com.github.windsekirun.yukarisynthesizer.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ObservableArrayList
import com.github.windsekirun.baseapp.module.composer.EnsureMainThreadComposer
import com.github.windsekirun.baseapp.utils.propertyChanges
import com.github.windsekirun.baseapp.utils.subscribe
import com.github.windsekirun.bindadapters.observable.ObservableString
import com.github.windsekirun.yukarisynthesizer.MainApplication
import com.github.windsekirun.yukarisynthesizer.core.YukariOperator
import com.github.windsekirun.yukarisynthesizer.core.annotation.OrderType
import com.github.windsekirun.yukarisynthesizer.core.item.VoiceItem
import com.github.windsekirun.yukarisynthesizer.core.item.VoiceItem_
import com.github.windsekirun.yukarisynthesizer.databinding.VoiceHistoryFragmentBinding
import com.github.windsekirun.yukarisynthesizer.main.adapter.VoiceItemAdapter
import com.github.windsekirun.yukarisynthesizer.module.sheet.RoundedBottomSheetDialogFragment
import com.github.windsekirun.yukarisynthesizer.utils.safeDispose
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class VoiceHistoryFragment : RoundedBottomSheetDialogFragment<VoiceHistoryFragmentBinding>() {
    val searchTitle = ObservableString()
    val itemData = ObservableArrayList<VoiceItem>()

    @Inject
    lateinit var yukariOperator: YukariOperator
    lateinit var callback: (VoiceItem) -> Unit

    private var disposable: Disposable? = null

    override fun createView(inflater: LayoutInflater, container: ViewGroup?) =
        VoiceHistoryFragmentBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.fragment = this

        MainApplication.appComponent!!.inject(this)
        val voiceItemAdapter = initRecyclerView(binding.recyclerView, VoiceItemAdapter::class.java)
        voiceItemAdapter.voiceItemClickListener = object : VoiceItemAdapter.OnVoiceItemClickListener {
            override fun onClick(voiceItem: VoiceItem) {
                callback.invoke(voiceItem)
                dismiss()
            }
        }

        searchTitle.propertyChanges()
            .throttleLast(500, TimeUnit.MILLISECONDS)
            .subscribe { _, _ ->
                searchData()
            }.addTo(compositeDisposable)

        searchData()
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        disposable.safeDispose()
    }

    private fun searchData() {
        disposable.safeDispose()

        disposable = yukariOperator.getVoiceList(
            searchTitle = searchTitle.get(),
            orderBy = OrderType.OrderFlags.DESCENDING to VoiceItem_.regDate
        )
            .compose(EnsureMainThreadComposer())
            .subscribe { data, error ->
                if (error != null) return@subscribe
                itemData.clear()
                itemData.addAll(data!!)
            }
    }
}