package com.taxnest.app.receipts

import com.taxnest.app.data.today
import com.taxnest.app.data.parseMoney
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Heuristics supply suggestions for user review; they never assert tax eligibility. */
data class ReceiptDraft(
    val merchant: String = "",
    val amount: String = "",
    val date: String = today(),
    val category: String = "Shopping",
    val photoUri: String = "",
    val rawText: String = ""
)
object ReceiptParser {
    private val amountRegex = Regex("(?i)\\b(?:grand\\s+total|total\\s+due|amount\\s+due|total)\\b[^\\d\\n]{0,20}(\\d{1,6}[.,]\\d{2})")
    private val dateRegex = Regex("\\b(0?[1-9]|1[0-2])[/-](0?[1-9]|[12][0-9]|3[01])[/-](20\\d{2}|\\d{2})\\b")
    fun fromOcr(text: String, uri: String):ReceiptDraft {
        val lines=text.lines().map{it.trim()}.filter{it.length>2}
        val merchant=lines.firstOrNull{it.any(Char::isLetter)}?.take(55) ?: "Unknown store"
        val totals=amountRegex.findAll(text).mapNotNull { it.groupValues.getOrNull(1) }.toList()
        val best=totals.lastOrNull() ?: ""
        val foundDate=dateRegex.find(text)?.value?.let { raw ->
            try {
                val parts=raw.split('/','-'); var year=parts[2].toInt()
                if(year<100) year+=2000
                LocalDate.of(year,parts[0].toInt(),parts[1].toInt()).toString()
            } catch (_:Exception){null}
        } ?: today()
        return ReceiptDraft(merchant=merchant,amount=best,date=foundDate,photoUri=uri,rawText=text)
    }
}
