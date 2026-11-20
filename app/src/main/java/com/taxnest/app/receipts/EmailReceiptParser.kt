package com.taxnest.app.receipts

import com.taxnest.app.data.SmartInsights
import com.taxnest.app.data.today
import java.time.LocalDate

/** Parses text explicitly pasted/shared by the user. It never reads Gmail or any mailbox itself. */
object EmailReceiptParser {
    private val totalLine = Regex("(?i)(?:^|\\n)\\s*(?:order total|grand total|total paid|amount paid|total|payment total|charged)\\s*[:=\\-]?\\s*(?:USD\\s*)?\\$?\\s*(\\d{1,6}(?:,\\d{3})*\\.\\d{2})")
    private val chargedLine = Regex("(?i)(?:charged|paid|order total)[^\\n$]{0,45}\\$\\s*(\\d{1,6}(?:,\\d{3})*\\.\\d{2})")
    private val simpleDate = Regex("\\b(20\\d{2})-(\\d{2})-(\\d{2})\\b")
    private val merchantHeader = Regex("(?i)^(?:from|merchant|store|seller|vendor)\\s*[:=]\\s*(.{3,75})$")

    fun parse(text: String): ReceiptDraft {
        val lines=text.lines().map(String::trim).filter { it.isNotBlank() }
        val labelled=lines.firstNotNullOfOrNull{merchantHeader.find(it)?.groupValues?.getOrNull(1)}
        val merchant=labelled ?: lines.firstOrNull { it.length in 3..70 && it.any(Char::isLetter) && !it.contains('@') }
            ?: ""
        val sum=totalLine.findAll("\n$text").lastOrNull()?.groupValues?.get(1)
            ?: chargedLine.findAll(text).lastOrNull()?.groupValues?.get(1) ?: ""
        val date=simpleDate.find(text)?.value?.let { raw -> runCatching { LocalDate.parse(raw).toString() }.getOrNull() }
            ?: today()
        return ReceiptDraft(merchant=merchant.take(75),amount=sum.replace(",",""),date=date,
            category=SmartInsights.category(merchant,text),photoUri="",rawText=text)
    }
}
