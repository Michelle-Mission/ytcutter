package app.ytcutter.core

data class CookieRecord(
    val domain: String,
    val name: String,
    val value: String,
    val expires: Long,
    val secure: Boolean = true
)

object CookieExport {

    const val HEADER = "# Netscape HTTP Cookie File\n"

    fun fromCookieHeader(header: String, domain: String, expires: Long): List<CookieRecord> =
        header.split(';')
            .mapNotNull { part ->
                val trimmed = part.trim()
                val idx = trimmed.indexOf('=')
                if (idx <= 0) return@mapNotNull null
                val name = trimmed.substring(0, idx).trim()
                val value = trimmed.substring(idx + 1).trim()
                if (name.isEmpty()) return@mapNotNull null
                CookieRecord(domain, name, value, expires, secure = true)
            }

    fun merge(records: Collection<CookieRecord>): List<CookieRecord> {
        val byKey = LinkedHashMap<String, CookieRecord>()
        records.forEach { byKey["${it.domain}|${it.name}"] = it }
        return byKey.values.toList()
    }

    fun toNetscape(records: Collection<CookieRecord>): String {
        val body = merge(records).joinToString("\n") { record ->
            val subdomains = if (record.domain.startsWith(".")) "TRUE" else "FALSE"
            val secure = if (record.secure) "TRUE" else "FALSE"
            "${record.domain}\t$subdomains\t/\t$secure\t${record.expires}\t${record.name}\t${record.value}"
        }
        return HEADER + body + "\n"
    }
}