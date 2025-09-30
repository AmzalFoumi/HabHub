package com.amzal.habhub.ui.habits

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HabitsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is habits Fragment"
    }
    val text: LiveData<String> = _text
}