package com.example.hw5

import androidx.lifecycle.ViewModel

class SharedViewModel: ViewModel() {
    var locationItemList = mutableListOf(
        LocationItem("hi", "hi", "hi", ""))

    var address: String = ""
    var coordinates: String = ""
    var time: String = ""

}