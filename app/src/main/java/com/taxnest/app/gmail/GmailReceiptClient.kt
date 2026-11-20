package com.taxnest.app.gmail

import android.text.Html
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId

/** Only receipt candidates currently displayed in the review UI are kept in memory.
 *  Never logs OAuth tokens or email bodies; never automatically saves mail. */
data class GmailReceiptCandidate(
    val messageId: String,
    val merchant: String,
    val date: String,
    val subject: String,
    val preview: String,
    val reviewText: String
)

object GmailReceiptClient {
    const val READ_ONLY_SCOPE = "https://www.googleapis.com/auth/gmail.readonly"
    private const val BASE = "https://gmail.googleapis.com/gmail/v1/users/me/messages"
    private val purchaseSubject = Regex("(?i)(receipt|invoice|order confirmation|your order|purchase|payment received|payment confirmation|order placed|thanks for your order|transaction)")

    suspend fun findReceipts(token: String): List<GmailReceiptCandidate> = withContext(Dispatchers.IO) {
        require(token.isNotBlank()) { "Gmail authorization is missing. Reconnect Gmail." }
        val query = "newer_than:18m {receipt invoice \"order confirmation\" \"purchase confirmation\" \"payment received\"}"
        val list = getJson("$BASE?maxResults=30&q=${URLEncoder.encode(query, "UTF-8")}",token)
        val messages=list.optJSONArray("messages") ?: return@withContext emptyList()
        val found=mutableListOf<GmailReceiptCandidate>()
        for (i in 0 until messages.length()) {
            val id=messages.optJSONObject(i)?.optString("id")?.takeIf{it.isNotBlank()} ?: continue
            try {
                val msg=getJson("$BASE/${URLEncoder.encode(id,"UTF-8")}?format=full",token)
                val payload=msg.optJSONObject("payload") ?: continue
                val headers=payload.optJSONArray("headers")
                fun header(name:String):String {
                    if(headers==null)return ""
                    for(j in 0 until headers.length()) {
                        val h=headers.optJSONObject(j) ?: continue
                        if(h.optString("name").equals(name,true))return h.optString("value")
                    }
                    return ""
                }
                val subject=header("Subject").take(140)
                // Avoid collecting unrelated marketing mail even if Gmail search yielded it.
                if(!purchaseSubject.containsMatchIn(subject))continue
                val sender=header("From")
                val merchant=sender.substringBefore("<").trim().trim('"').ifBlank { sender.substringAfter('@').substringBefore('>') }.take(80)
                val date=msg.optString("internalDate").toLongOrNull()?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                } ?: java.time.LocalDate.now().toString()
                val text=extractText(payload).ifBlank{msg.optString("snippet")}.take(16000)
                if(text.isBlank())continue
                // Fields at front make them available to TaxNest's existing on-device review parser.
                val reviewText="Merchant: $merchant\nDate: $date\nSubject: $subject\n"+text
                found.add(GmailReceiptCandidate(id,merchant,date,subject,text.take(170).replace("\n"," "),reviewText))
            } catch(ex:Exception) {
                // A single malformed or stale Gmail message should not stop the whole import.
                if(ex.message?.startsWith("Gmail authorization") == true)throw ex
            }
        }
        found
    }

    private fun extractText(payload:JSONObject):String {
        var plain="";var html=""
        fun walk(part:JSONObject) {
            val mime=part.optString("mimeType")
            val body=part.optJSONObject("body")?.optString("data").orEmpty()
            if(body.isNotBlank() && (mime=="text/plain" || mime=="text/html")) {
                val decoded=runCatching{String(Base64.decode(body,Base64.URL_SAFE or Base64.NO_WRAP),Charsets.UTF_8)}.getOrDefault("")
                if(mime=="text/plain") plain+=decoded+"\n"
                else html+=Html.fromHtml(decoded,Html.FROM_HTML_MODE_COMPACT).toString()+"\n"
            }
            val parts=part.optJSONArray("parts")
            if(parts!=null) for(i in 0 until parts.length())parts.optJSONObject(i)?.let(::walk)
        }
        walk(payload)
        return plain.ifBlank{html}.trim()
    }

    private fun getJson(url:String,token:String):JSONObject {
        val conn=(URL(url).openConnection() as HttpURLConnection)
        conn.requestMethod="GET"
        conn.connectTimeout=12000
        conn.readTimeout=16000
        conn.setRequestProperty("Authorization","Bearer $token")
        conn.setRequestProperty("Accept","application/json")
        try {
            val status=conn.responseCode
            if(status==401 || status==403)error("Gmail authorization is unavailable ($status). Check consent and reconnect.")
            if(status !in 200..299)error("Gmail request failed ($status). Check that Gmail API is enabled in Google Cloud.")
            val raw=conn.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(raw)
        } finally { conn.disconnect() }
    }
}
