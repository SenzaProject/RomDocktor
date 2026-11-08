package com.romdoctor.core.model

/**
 * Hasil setiap pemeriksaan. Tiga status + Error, tidak ada "aman" palsu:
 * jika tidak bisa diperiksa, hasilnya Unavailable, bukan Ok.
 */
sealed interface CheckResult {
    data class Ok(val evidence: Evidence? = null) : CheckResult
    data class Problem(val evidence: Evidence) : CheckResult
    data class Unavailable(val reason: String) : CheckResult
    data class Error(val throwable: Throwable) : CheckResult
}
