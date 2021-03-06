package com.github.windsekirun.yukarisynthesizer.main.details

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.github.windsekirun.baseapp.base.BaseViewModel
import com.github.windsekirun.baseapp.module.activityresult.RxActivityResult
import com.github.windsekirun.baseapp.module.composer.EnsureMainThreadComposer
import com.github.windsekirun.baseapp.module.reference.ActivityReference
import com.github.windsekirun.baseapp.utils.ProgressDialogManager
import com.github.windsekirun.baseapp.utils.propertyChanges
import com.github.windsekirun.bindadapters.observable.ObservableString
import com.github.windsekirun.daggerautoinject.InjectViewModel
import com.github.windsekirun.yukarisynthesizer.MainApplication
import com.github.windsekirun.yukarisynthesizer.R
import com.github.windsekirun.yukarisynthesizer.core.YukariOperator
import com.github.windsekirun.yukarisynthesizer.core.define.VoiceEngine
import com.github.windsekirun.yukarisynthesizer.core.item.PhonomeItem
import com.github.windsekirun.yukarisynthesizer.core.item.StoryItem
import com.github.windsekirun.yukarisynthesizer.core.item.VoiceItem
import com.github.windsekirun.yukarisynthesizer.dialog.PlayDialog
import com.github.windsekirun.yukarisynthesizer.main.event.*
import com.github.windsekirun.yukarisynthesizer.swipe.SwipeOrderActivity
import com.github.windsekirun.yukarisynthesizer.swipe.SwipeOrderViewModel
import com.github.windsekirun.yukarisynthesizer.utils.subscribe
import com.github.windsekirun.yukarisynthesizer.voice.VoiceDetailActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import pyxis.uzuki.live.richutilskt.utils.RPermission
import pyxis.uzuki.live.richutilskt.utils.toFile
import javax.inject.Inject

/**
 * DokodemoYukariAndroidClient
 * Class: MainDetailsViewModel
 * Created by Pyxis on 2018-11-27.
 *
 *
 * Description:
 */

@InjectViewModel
class MainDetailsViewModel @Inject
constructor(application: MainApplication) : BaseViewModel(application) {
    val itemData: MutableLiveData<MutableList<VoiceItem>> = MutableLiveData()
    val title = ObservableString()

    @Inject
    lateinit var yukariOperator: YukariOperator

    private var changed: Boolean = false
    private lateinit var storyItem: StoryItem
    private val changeObserver = Observer<List<VoiceItem>> {
        changed = true
    }

    override fun onCleared() {
        super.onCleared()
        itemData.removeObserver(changeObserver)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        itemData.removeObserver(changeObserver)
    }

    fun loadData(storyItem: StoryItem?) {
        this.storyItem = storyItem ?: StoryItem()
        bindItems(storyItem == null)
    }

    fun onBackPressed() {
        if (!changed || (title.isEmpty && itemData.value!!.isEmpty())) {
            postEvent(CloseFragmentEvent())
        } else {
            save(autoClose = true)
        }
    }

    fun clickMenuItem(mode: ToolbarMenuClickEvent.Mode) {
        when (mode) {
            ToolbarMenuClickEvent.Mode.Play -> requestSynthesis()
            ToolbarMenuClickEvent.Mode.TopOrder -> save(swipeOrder = true)
            ToolbarMenuClickEvent.Mode.Remove -> removeStoryItem()

        }
    }

    fun clickSpeedDial(mode: SpeedDialClickEvent.Mode) {
        when (mode) {
            SpeedDialClickEvent.Mode.Voice -> addVoice()
            SpeedDialClickEvent.Mode.Break -> addBreak()
            SpeedDialClickEvent.Mode.History -> addVoiceHistory()
            SpeedDialClickEvent.Mode.STT -> addSTT()
        }
    }

    private fun bindItems(initial: Boolean) {
        if (initial) {
            itemData.value = mutableListOf()
            observeEvent()
            return
        }

        val disposable = yukariOperator.getVoiceListAssociatedStoryItem(storyItem)
            .compose(EnsureMainThreadComposer())
            .subscribe { data, error ->
                if (error != null) return@subscribe
                title.set(storyItem.title)
                itemData.value = data!!.toMutableList()

                observeEvent()
            }

        addDisposable(disposable)
    }

    private fun save(autoClose: Boolean = false, swipeOrder: Boolean = false) {
        if (itemData.value!!.isEmpty()) {
            postEvent(CloseFragmentEvent())
            return
        }

        storyItem.apply {
            this.title = this@MainDetailsViewModel.title.get()
            this.voiceEntries = itemData.value!!
        }

        val disposable = yukariOperator.addStoryItem(storyItem)
            .compose(EnsureMainThreadComposer())
            .subscribe { _, _ ->
                if (autoClose) postEvent(CloseFragmentEvent())
                if (swipeOrder) moveToSwipeOrderActivity()
            }

        addDisposable(disposable)
    }

    private fun observeEvent() {
        itemData.observeForever(changeObserver)

        val disposable =
            title.propertyChanges()
                .subscribe { _, _ -> changed = true }
        addDisposable(disposable)
    }

    private fun requestSynthesis() {
        if (itemData.value!!.isEmpty()) return

        val ids = itemData.value!!.map { it.id }
        val contentEqual = checkEqualContent(ids, storyItem.voicesIds)
        if (contentEqual && (storyItem.localPath.isNotEmpty() && storyItem.localPath.toFile().canRead())) {
            // if itemData and voiceIds is equal and localPath is valid, we don't need to synthesis this timing.
            // just play sounds.
            playVoices()
        }

        storyItem.apply {
            this.title = this@MainDetailsViewModel.title.get()
            this.voiceEntries = itemData.value!!
        }

        ProgressDialogManager.instance.show()

        val disposable = yukariOperator.addStoryItem(storyItem)
            .flatMapSingle { yukariOperator.requestSynthesis(storyItem) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { data, error ->
                ProgressDialogManager.instance.clear()
                if (error != null) {
                    showAlertDialog(error.message ?: "unknown error")
                    return@subscribe
                }

                storyItem = data!!
                playVoices()
            }

        addDisposable(disposable)
    }

    private fun playVoices() {
        val playDialog = PlayDialog(ActivityReference.getActivtyReference()!!)
        playDialog.show(listOf(storyItem))
    }

    private fun removeStoryItem() {
        ProgressDialogManager.instance.show()

        val disposable = yukariOperator.removeStoryItem(storyItem)
            .compose(EnsureMainThreadComposer())
            .subscribe { _, error ->
                ProgressDialogManager.instance.clear()
                if (error != null) return@subscribe

                showAlertDialog(getString(R.string.main_details_removed)) { _, _ ->
                    changed = false
                    onBackPressed()
                }
            }

        addDisposable(disposable)
    }

    private fun addBreak() {
        val event = ShowBreakDialogEvent(VoiceItem()) {
            val disposable = yukariOperator.addVoiceItem(it)
                .compose(EnsureMainThreadComposer())
                .subscribe { data, error ->
                    if (error != null) return@subscribe
                    val list = itemData.value!!
                    list.add(data!!)
                    itemData.value = list
                }

            addDisposable(disposable)
        }

        postEvent(event)
    }

    private fun addVoice() {
        RxActivityResult.result()
            .subscribe { data, error ->

            }.addTo(compositeDisposable)

        RxActivityResult.startActivityForResult(VoiceDetailActivity::class.java)
    }

    private fun addSTT() {
        val event = ShowVoiceRecognitionEvent { voiceResult ->
            val splitResult = voiceResult.split(" ").map { it.trim() }
            val phonomes = splitResult.map { PhonomeItem(it, "") }.toMutableList()

            val disposable = yukariOperator.getPresetList()
                .flatMap {
                    Observables.combineLatest(
                        yukariOperator.getDefaultPresetItem(VoiceEngine.Yukari),
                        yukariOperator.addPhonomeItems(phonomes)
                    )
                }
                .subscribe { data, _ ->
                    val preset = data!!.first
                    val phonomeIds = data.second

                    val voiceItem = VoiceItem().apply {
                        this.engine = preset.engine
                        this.preset = preset
                        this.breakTime = 0
                        this.phonomes = phonomes
                        this.phonomeIds = phonomeIds
                        bindContentOrigin()
                    }

                    val list = itemData.value!!
                    list.add(voiceItem)
                    itemData.value = list
                }

            addDisposable(disposable)
        }

        requestPermission({ code ->
            if (code == RPermission.PERMISSION_GRANTED) {
                postEvent(event)
            } else {
                showToast(getString(R.string.main_detail_record_permission))
            }
        }, arrayOf(Manifest.permission.RECORD_AUDIO))
    }

    private fun addVoiceHistory() {
        val event = ShowHistoryDialogEvent {
            val list = itemData.value!!
            list.add(it)
            itemData.value = list
        }

        postEvent(event)
    }

    private fun moveToSwipeOrderActivity() {
        val bundle = Bundle()
        bundle.putLong(SwipeOrderViewModel.EXTRA_STORY_ITEM_ID, storyItem.id)

        val disposable = RxActivityResult.result()
            .toObservable()
            .flatMap { yukariOperator.getStoryItem(storyItem.id) }
            .subscribe { data, error ->
                if (error != null) return@subscribe
                loadData(data)
            }

        RxActivityResult.startActivityForResult(
            SwipeOrderActivity::class.java,
            bundle,
            ActivityReference.getActivtyReference() as? AppCompatActivity
        )

        addDisposable(disposable)
    }

    private fun checkEqualContent(target: List<Long?>?, original: List<Long>): Boolean {
        val ids = target?.toTypedArray() ?: arrayOfNulls(storyItem.voicesIds.size)
        return ids contentEquals original.toTypedArray()
    }
}