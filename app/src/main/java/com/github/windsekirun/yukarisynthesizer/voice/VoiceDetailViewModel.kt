package com.github.windsekirun.yukarisynthesizer.voice

import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.github.windsekirun.baseapp.base.BaseViewModel
import com.github.windsekirun.baseapp.module.argsinjector.Extra
import com.github.windsekirun.bindadapters.observable.ObservableString
import com.github.windsekirun.daggerautoinject.InjectViewModel
import com.github.windsekirun.yukarisynthesizer.MainApplication
import com.github.windsekirun.yukarisynthesizer.core.YukariOperator
import com.github.windsekirun.yukarisynthesizer.core.define.VoiceEngine
import com.github.windsekirun.yukarisynthesizer.core.item.PhonomeItem
import com.github.windsekirun.yukarisynthesizer.core.item.PresetItem
import com.github.windsekirun.yukarisynthesizer.main.event.ShowPhonomeHistoryEvent
import com.github.windsekirun.yukarisynthesizer.main.event.ShowPresetDialogEvent
import com.github.windsekirun.yukarisynthesizer.utils.subscribe
import com.github.windsekirun.yukarisynthesizer.voice.event.RefreshLayoutEvent
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import javax.inject.Inject
import android.view.inputmethod.EditorInfo
import android.widget.TextView



@InjectViewModel
class VoiceDetailViewModel @Inject
constructor(application: MainApplication) : BaseViewModel(application) {
    val selectedEngine = ObservableField<VoiceEngine>()
    val selectedPreset = ObservableString()
    val selectedDeceptions = ObservableString()
    val selectedText = ObservableString()
    val voiceOriginLength = ObservableInt()

    val itemData: MutableLiveData<MutableList<PhonomeItem>> = MutableLiveData()
    var selectedPhonomesIndex = -1
    var selectedPhonomeItem: PhonomeItem? = null

    private val changeObserver = Observer<List<PhonomeItem>> { refreshFlexBox() }
    private var selectedPresetItem: PresetItem = PresetItem()

    @Inject
    lateinit var yukariOperator: YukariOperator
    @Extra(EXTRA_VOICE_ID)
    var voiceId: Long = 0

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        loadData()
        itemData.observeForever(changeObserver)
    }

    override fun onCleared() {
        super.onCleared()
        itemData.removeObserver(changeObserver)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        itemData.removeObserver(changeObserver)
    }

    fun onBackPressed() {
        // TODO: implement this
    }

    fun clickPresetSelect(view: View) {
        val event = ShowPresetDialogEvent(selectedEngine.get()!!) {
            selectedPresetItem = it
            selectedPreset.set(it.title)
        }

        postEvent(event)
    }

    fun clickYukari(view: View) {
        selectedEngine.set(VoiceEngine.Yukari)
    }

    fun clickMaki(view: View) {
        selectedEngine.set(VoiceEngine.Maki)
    }

    fun clickEnter(view: View) {
        if (selectedText.get().isEmpty()) return

        val list: MutableList<PhonomeItem> = if (itemData.value != null) itemData.value!! else mutableListOf()
        if (selectedPhonomesIndex != -1) {
            val phonomeItem = list[selectedPhonomesIndex].apply {
                this.origin = selectedText.get()
                this.phoneme = selectedDeceptions.get()
            }

            list[selectedPhonomesIndex] = phonomeItem
        } else {
            val phonomeItem = PhonomeItem(selectedText.get(), selectedDeceptions.get())
            list.add(phonomeItem)
        }

        selectedText.set("")
        selectedDeceptions.set("")

        itemData.value = list
    }


    fun clickHistory(view: View) {
        val list: MutableList<PhonomeItem> = if (itemData.value != null) itemData.value!! else mutableListOf()
        val event = ShowPhonomeHistoryEvent {
            list.add(it)
            itemData.value = list
        }

        postEvent(event)
    }

    fun clickItem(item: PhonomeItem) {
        selectedPhonomeItem = item
        selectedPhonomesIndex = itemData.value!!.indexOf(item)

        selectedText.set(item.origin)
        selectedDeceptions.set(item.phoneme)

        refreshFlexBox()
    }

    fun clickRemove(item: PhonomeItem) {
        selectedPhonomeItem = null
        selectedPhonomesIndex = -1
        selectedText.set("")
        selectedDeceptions.set("")

        val list = itemData.value!!
        list.remove(item)
        itemData.value = list

        refreshFlexBox()
    }

    fun handleEditorAction(view: TextView, actionId: Int?, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            clickEnter(view)
            return true
        }
        return false
    }

    private fun loadData() {
        if (voiceId == 0L) {
            selectedEngine.set(VoiceEngine.Yukari)
            return
        }

        val disposable = yukariOperator.getVoiceItem(voiceId)
            .flatMap {
                Observables.combineLatest(
                    yukariOperator.getPhonomeListAssociatedVoiceItem(it),
                    Observable.just(it)
                )
            }
            .subscribe { data, error ->
                if (error != null) {
                    Log.e(TAG, "ignore error", error)
                    return@subscribe
                }

                itemData.value = data!!.first.toMutableList()
                selectedEngine.set(data.second.engine)
                selectedPresetItem = data.second.preset

                selectedPreset.set(data.second.preset.title)
            }

        addDisposable(disposable)
    }

    private fun refreshFlexBox() {
        val sum = itemData.value!!.map { it.origin.length }.sum()

        voiceOriginLength.set(sum)
        postEvent(RefreshLayoutEvent())
    }

    companion object {
        const val EXTRA_VOICE_ID = "45c57318-1d67-4ace-800e-7e3257a79b8b"
        val TAG = VoiceDetailViewModel::class.java.simpleName
    }
}