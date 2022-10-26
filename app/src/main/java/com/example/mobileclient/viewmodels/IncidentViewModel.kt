package com.example.mobileclient.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobileclient.api.Repository
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Response
import java.net.ConnectException

class IncidentViewModel:ViewModel() {
    private var repository: Repository = Repository
    var postCallResponseBody: MutableLiveData<Response<ResponseBody>> = MutableLiveData()

}