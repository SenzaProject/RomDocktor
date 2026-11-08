package com.romdoctor.core.model

/** Temuan masalah. Wajib membawa minimal satu Evidence (No Evidence, No Issue). */
data class Finding(
    val ruleId: String,
    val title: String,
    val severity: Severity,
    val evidence: List<Evidence>,
) {
    init {
        require(evidence.isNotEmpty()) { "Finding $ruleId harus punya Evidence" }
    }
}
