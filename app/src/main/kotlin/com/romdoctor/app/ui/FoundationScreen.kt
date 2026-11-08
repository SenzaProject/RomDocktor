package com.romdoctor.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romdoctor.app.MainViewModel
import com.romdoctor.core.model.Capability
import com.romdoctor.core.model.CheckResult
import com.romdoctor.core.model.OperationMode

private val GreenOk = Color(0xFF66BB6A)
private val AmberWarn = Color(0xFFFFB74D)
private val RedBad = Color(0xFFEF5350)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoundationScreen(viewModel: MainViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val report = state.report

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ROM Doctor · Fondasi") },
                actions = {
                    TextButton(onClick = viewModel::refresh, enabled = !state.isScanning) {
                        Text("Deteksi ulang")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (report == null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator(Modifier.padding(2.dp))
                                Text("Menguji kemampuan perangkat…")
                            }
                        } else {
                            val rooted = report.mode == OperationMode.ROOT
                            Text(
                                if (rooted) "Mode: ROOT" else "Mode: NON-ROOT",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (rooted) GreenOk else AmberWarn,
                            )
                            val ok = report.results.values.count { it is CheckResult.Ok }
                            val total = report.results.size
                            Text("$ok dari $total kemampuan tersedia. Sisanya tidak dapat diperiksa di perangkat ini.")
                            if (state.isScanning) Text("Memuat ulang…")
                        }
                    }
                }
            }

            if (report != null && report.results[Capability.LOGCAT_FULL] !is CheckResult.Ok) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Logcat penuh tanpa root", fontWeight = FontWeight.Bold)
                            Text("Jalankan dari PC (atau adb wireless), lalu tekan Deteksi ulang:")
                            SelectionContainer {
                                Text(state.adbGrantCommand, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            if (report != null) {
                items(report.results.entries.toList(), key = { it.key.name }) { (cap, result) ->
                    CapabilityRow(cap, result)
                }
            }
        }
    }
}

@Composable
private fun CapabilityRow(capability: Capability, result: CheckResult) {
    var expanded by rememberSaveable(capability.name) { mutableStateOf(false) }
    val (statusText, color) = when (result) {
        is CheckResult.Ok -> "Tersedia" to GreenOk
        is CheckResult.Problem -> "Masalah" to RedBad
        is CheckResult.Unavailable -> "Tidak dapat diperiksa" to AmberWarn
        is CheckResult.Error -> "Error" to RedBad
    }
    Card(Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(capability.label, fontWeight = FontWeight.Bold)
                Text(statusText, color = color, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "Dipakai untuk: ${capability.usedFor}",
                style = MaterialTheme.typography.bodySmall,
            )
            if (expanded) {
                val detail = when (result) {
                    is CheckResult.Ok ->
                        result.evidence?.let {
                            "$ ${it.command}\n(exit ${it.exitCode ?: "-"})\n${it.output.ifEmpty { "(output kosong)" }}"
                        } ?: "(tanpa bukti)"
                    is CheckResult.Problem ->
                        "$ ${result.evidence.command}\n${result.evidence.output}"
                    is CheckResult.Unavailable -> result.reason
                    is CheckResult.Error ->
                        "${result.throwable::class.java.simpleName}: ${result.throwable.message}"
                }
                SelectionContainer {
                    Text(detail, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            } else {
                Text("Ketuk untuk melihat bukti mentah", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
