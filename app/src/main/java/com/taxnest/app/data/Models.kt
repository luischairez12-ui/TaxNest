package com.taxnest.app.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

/** All amounts are stored in integer cents, never floating-point dollars. */
data class MoneyEntry(
    val id: Long,
    val date: String,
    val merchant: String,
    val amountCents: Long,
    val category: String,
    val kind: String, // expense or income
    val receiptUri: String?,
    val ocrText: String?,
    val source: String
)

data class Subscription(
    val id: Long,
    val name: String,
    val amountCents: Long,
    val frequency: String,
    val nextDueDate: String
)

data class OvertimeWeek(
    val id: Long,
    val weekOf: String,
    val hourlyRateCents: Long,
    val regularHours: Double,
    val overtimeHours: Double,
    val flsaEligible: Boolean
)

data class MileageTrip(
    val id: Long,
    val date: String,
    val miles: Double,
    val purpose: String,
    val reimbursed: Boolean
)

fun dollars(cents: Long): String = NumberFormat.getCurrencyInstance(Locale.US).format(cents / 100.0)
fun parseMoney(text: String): Long? = try {
    val normalized = text.replace("$", "").replace(",", "").trim()
    if (normalized.isBlank()) null
    else BigDecimal(normalized).multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()
} catch (_: Exception) { null }
fun today(): String = LocalDate.now().toString()
fun monthStart(): String = today().take(7)

/** Estimate only. The W-2 code TT / payroll record controls actual reported qualified overtime. */
fun OvertimeWeek.estimatedPremiumCents(): Long =
    if (!flsaEligible) 0L else (hourlyRateCents * 0.5 * overtimeHours).toLong()
