package com.eschool24.paymonitor

data class BankilyTransaction(
    val transactionId: String,
    val senderName: String,
    val amount: String,
    val phone: String
)

object BankilyParser {

    // Standalone 15+ digit sequence — Bankily transaction IDs
    private val TXN_ID_RE = Regex("""\b(\d{15,})\b""")

    // "Montant : 10 MRU" or "Montant: 10 MRU" (space before colon optional)
    private val AMOUNT_RE = Regex(
        """montant\s*:\s*([\d.,]+(?:\s[\d.,]+)*)\s*MRU""",
        RegexOption.IGNORE_CASE
    )

    // "Expediteur : HASSAN JAIFI,41911809" — name may wrap across lines
    private val EXPEDITEUR_RE = Regex(
        """expediteur\s*:\s*([\s\S]+?),\s*(\d+)""",
        RegexOption.IGNORE_CASE
    )

    fun parse(title: String, body: String): BankilyTransaction? {
        val text = "$title\n$body"

        val transactionId = TXN_ID_RE.find(text)?.groupValues?.get(1) ?: return null

        val amount = AMOUNT_RE.find(text)?.groupValues?.get(1)?.trim() ?: ""

        val expMatch = EXPEDITEUR_RE.find(text)
        val senderName = expMatch?.groupValues?.get(1)
            ?.replace(Regex("""\s+"""), " ")?.trim() ?: ""
        val phone = expMatch?.groupValues?.get(2)?.trim() ?: ""

        return BankilyTransaction(
            transactionId = transactionId,
            senderName = senderName,
            amount = amount,
            phone = phone
        )
    }
}
