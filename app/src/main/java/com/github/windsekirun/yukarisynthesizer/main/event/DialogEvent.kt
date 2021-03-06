package com.github.windsekirun.yukarisynthesizer.main.event

import com.github.windsekirun.yukarisynthesizer.core.define.VoiceEngine
import com.github.windsekirun.yukarisynthesizer.core.item.PhonomeItem
import com.github.windsekirun.yukarisynthesizer.core.item.PresetItem
import com.github.windsekirun.yukarisynthesizer.core.item.VoiceItem

class ShowBreakDialogEvent(val param: VoiceItem, val callback: (VoiceItem) -> Unit)

class ShowHistoryDialogEvent(val callback: (VoiceItem) -> Unit)

class ShowVoiceRecognitionEvent(val callback: (String) -> Unit)

class ShowPresetDialogEvent(val param: VoiceEngine, val callback: (PresetItem) -> Unit)

class ShowPhonomeHistoryEvent( val callback: (PhonomeItem) -> Unit)