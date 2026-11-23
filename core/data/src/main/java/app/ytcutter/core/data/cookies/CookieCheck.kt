package app.ytcutter.core.data.cookies

import android.content.Context
import app.ytcutter.core.common.NetscapeCookies
import app.ytcutter.core.database.CookiesRepo
import app.ytcutter.core.model.CookieStatus
import app.ytcutter.core.network.CookieProbeApi

object CookieCheck {

    fun classify(finalUrl: String?, code: Int): CookieStatus =
        CookieProbeApi.classify(finalUrl, code)

    /** Jumlah cookie tersimpan di DB, termasuk HttpOnly. */
    fun count(context: Context): Int =
        CookiesRepo.get(context).buildEnabledText().lineSequence().count { it.isNotBlank() }

    suspend fun probe(context: Context): CookieStatus {
        val header = NetscapeCookies.header(
            CookiesRepo.get(context).buildEnabledText().lines()
        ) ?: return CookieStatus.UNKNOWN
        return CookieProbeApi.probe(header)
    }
}