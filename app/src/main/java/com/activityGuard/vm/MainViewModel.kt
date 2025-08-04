package com.activityGuard.vm

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Created by DengLongFei
 * 2025/08/04
 */
@HiltViewModel
class MainViewModel  @Inject constructor(): ViewModel() {
    val  sss = "MainViewModel"
}