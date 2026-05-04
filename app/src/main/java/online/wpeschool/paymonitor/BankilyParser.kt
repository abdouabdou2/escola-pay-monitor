package online.wpeschool.paymonitor

data class BankilyTransaction(
    val transactionId: String,
    val senderName: String,
    val amount: String,
    val phone: String
)

object BankilyParser {

    // Bankily transaction IDs are 10+ consecutive digits
    private val TXN_ID_RE = Regex("""\b(\d{10,})\b""")

    // Amounts in MRU / UM / أوقية
    private val AMOUNT_RE = Regex(
        """(\d[\d\s,.']*\d|\d+)\s*(?:MRU|UM|أوقية|ouguiya)""",
        RegexOption.IGNORE_CASE
    )

    // Mauritanian mobile numbers: starts with 2, 3, or 4, 8 digits total, optional +222/00222 prefix
    private val PHONE_RE = Regex("""\b(?:\+?222|00222)?([234]\d{7})\b""")

    fun parse(title: String, body: String): BankilyTransaction? {
        val text = "$title $body"

        val transactionId = TXN_ID_RE.find(text)?.groupValues?.get(1) ?: return null
        val amount = AMOUNT_RE.find(text)?.groupValues?.get(1)?.trim() ?: ""
        val phone = PHONE_RE.find(text)?.groupValues?.get(1) ?: ""
        val senderName = extractSenderName(text, phone)

        return BankilyTransaction(
            transactionId = transactionId,
            senderName = senderName,
            amount = amount,
            phone = phone
        )
    }

    private fun extractSenderName(text: String, phone: String): String {
        // Arabic pattern: مبلغ ... من <name> <phone|txn>
        Regex("""من\s+([^\d\n]{2,40?}?)(?=\s*\d|\s*${'$'})""").find(text)
            ?.groupValues?.get(1)?.trim()
            ?.takeIf { it.length >= 2 }
            ?.let { return it }

        // French/English pattern: de <name> / from <name>
        Regex("""(?:de|from)\s+([A-Za-z؀-ۿ ]{2,40?})(?=\s*[\d\-]|\s*${'$'})""", RegexOption.IGNORE_CASE)
            .find(text)
            ?.groupValues?.get(1)?.trim()
            ?.takeIf { it.isNotBlank() && it != phone }
            ?.let { return it }

        return ""
    }
}
