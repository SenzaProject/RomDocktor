package com.romdoctor.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romdoctor.core.model.CapabilityReport
import com.romdoctor.core.shell.CapabilityDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UiState(
    val isScanning: Boolean = true,
    val report: CapabilityReport? = null,
    val adbGrantCommand: String = "",
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val detector: CapabilityDetector,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        UiState(adbGrantCommand = "adb shell pm grant ${context.packageName} android.permission.READ_LOGS"),
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var job: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            val report = detector.detect()
            _uiState.update { it.copy(isScanning = false, report = report) }
        }
    }
}
