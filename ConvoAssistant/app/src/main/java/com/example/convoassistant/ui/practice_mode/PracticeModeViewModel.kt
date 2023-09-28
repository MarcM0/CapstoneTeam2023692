package com.example.convoassistant.ui.practice_mode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PracticeModeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is practice_mode Fragment"
    }
    val text: LiveData<String> = _text
}