package com.taxnest.app.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/** Offline-only, explainable hints. No tax or subscription classification is automatic. */
object SmartInsights {
    data class PossibleDuplicate(val entry: MoneyEntry, val reason: String)
    data class RecurringCandidate(
        val merchant: String, val amountCents: Long, val cadence: String,
        val nextDue: String, val occurrences: Int
    )

    fun normalizedMerchant(raw: String): String = raw.lowercase()
        .replace(Regex("(?i)\\b(inc|llc|store|online|purchase|payment|order|debit|credit|com)\\b"), " ")
        .replace(Regex("[^a-z0-9]+"), " ").trim()
        .split(" ").filter { it.isNotBlank() }.take(4).joinToString(" ")

    fun category(merchant: String, body: String = ""): String {
        val m = merchant.lowercase()
        val t = "$m ${body.take(400).lowercase()}"
        return when {
            listOf("autozone", "oreilly", "o'reilly", "napa auto", "advance auto", "discount tire", "tire kingdom", "ford service").any { it in m } -> "Truck & Auto"
            listOf("shell", "exxon", "chevron", "race trac", "racetrac", "pilot travel", "love's", "fuel", "diesel").any { it in m } -> "Fuel"
            listOf("netflix", "spotify", "youtube premium", "hulu", "disney+", "max.com", "apple.com/bill", "google one").any { it in m } -> "Subscriptions"
            listOf("publix", "aldi", "whole foods", "kroger", "winn dixie", "walmart grocery", "costco grocery").any { it in m } -> "Groceries"
            listOf("home depot", "lowe's", "harbor freight", "ace hardware").any { it in m } -> "Tools & Supplies"
            listOf("mcdonald", "taco bell", "chipotle", "subway", "starbucks", "restaurant", "pizza").any { it in m } -> "Dining"
            listOf("amazon", "target", "walmart", "ebay", "best buy").any { it in m } -> "Shopping"
            "insurance" in t -> "Insurance"
            "subscription" in t || "membership renewal" in t -> "Subscriptions"
            else -> "Shopping"
        }
    }

    fun duplicateMatches(
        entries: List<MoneyEntry>, date: String, merchant: String, cents: Long,
        kind: String = "expense"
    ): List<PossibleDuplicate> {
        val target = runCatching { LocalDate.parse(date) }.getOrNull() ?: return emptyList()
        val normalized = normalizedMerchant(merchant)
        if (normalized.isBlank() || cents <= 0) return emptyList()
        return entries.asSequence().filter { it.kind == kind && it.amountCents == cents }
            .mapNotNull { item ->
                val other = runCatching { LocalDate.parse(item.date) }.getOrNull() ?: return@mapNotNull null
                val gap = abs(ChronoUnit.DAYS.between(target, other))
                val seller = normalizedMerchant(item.merchant)
                if (gap <= 3 && (seller == normalized || seller.contains(normalized) || normalized.contains(seller)) && seller.isNotBlank())
                    PossibleDuplicate(item, if(gap==0L) "Same amount, merchant and date" else "Same amount and merchant within $gap days")
                else null
            }.take(4).toList()
    }

    fun recurringCandidates(entries: List<MoneyEntry>, existing: List<Subscription>): List<RecurringCandidate> {
        val excluded = existing.map { normalizedMerchant(it.name) }.toSet()
        val filtered = entries.filter { it.kind == "expense" && it.amountCents > 0 }
            .groupBy { normalizedMerchant(it.merchant) }
        return filtered.mapNotNull { (normalized, group) ->
            if(normalized.isBlank() || normalized in excluded || group.size < 2) return@mapNotNull null
            val sorted = group.mapNotNull { e -> runCatching { LocalDate.parse(e.date) }.getOrNull()?.let { it to e } }
                .sortedBy { it.first }
            if(sorted.size < 2) return@mapNotNull null
            val deltas = sorted.zipWithNext { a, b -> ChronoUnit.DAYS.between(a.first, b.first) }
                .filter { it >= 4 }
            if (deltas.isEmpty()) return@mapNotNull null
            val middle = deltas.sorted()[deltas.size / 2]
            val (frequency, advance) = when(middle) {
                in 6..9 -> "Weekly" to 7L
                in 25..37 -> "Monthly" to 30L
                in 340..390 -> "Yearly" to 365L
                else -> return@mapNotNull null
            }
            val amounts=sorted.map{it.second.amountCents}.sorted()
            val median=amounts[amounts.size/2]
            if (amounts.any { abs(it-median) > maxOf(100L,median/20L) }) return@mapNotNull null
            val recent=sorted.last()
            val next= if(frequency=="Monthly") recent.first.plusMonths(1)
                else if(frequency=="Yearly") recent.first.plusYears(1)
                else recent.first.plusDays(advance)
            RecurringCandidate(recent.second.merchant,median,frequency,next.toString(),sorted.size)
        }.sortedByDescending { it.occurrences }.take(12)
    }

    fun taxYearSummary(entries: List<MoneyEntry>, year: Int): Map<String,Long> = entries
        .filter { it.kind == "expense" && it.date.startsWith(year.toString()) }
        .groupBy { it.category }.mapValues { (_, values) -> values.sumOf { it.amountCents } }
}
