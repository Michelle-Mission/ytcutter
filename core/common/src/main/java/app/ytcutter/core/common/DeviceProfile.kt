package app.ytcutter.core.common

import android.app.ActivityManager
import android.content.Context

/**
 * Penyesuaian ringan untuk kelas perangkat. Dipakai untuk memilih jumlah
 * koneksi unduhan, batas waktu pencarian info, dan fitur berat (poToken/aurora).
 */
object DeviceProfile {

    enum class Tier { LOW, MID, HIGH }

    data class Profile(
        val tier: Tier,
        val lowRam: Boolean,
        val cores: Int,
        val heapMb: Int
    ) {
        val fragments: Int = when (tier) {
            Tier.LOW -> 2
            Tier.MID -> 4
            Tier.HIGH -> 8
        }

        val infoTimeoutMs: Long = when (tier) {
            Tier.LOW -> 12_000
            Tier.MID, Tier.HIGH -> 8_000
        }

        val tokenTimeoutMs: Long = if (tier == Tier.LOW) 1_800 else 2_500

        val warmUpTokens: Boolean = tier != Tier.LOW

        val animateBackdrop: Boolean = tier != Tier.LOW
    }

    @Volatile
    private var cached: Profile? = null

    fun of(context: Context): Profile = cached ?: synchronized(this) {
        cached ?: measure(context.applicationContext).also { cached = it }
    }

    private fun measure(context: Context): Profile {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val lowRam = am?.isLowRamDevice == true
        val heapMb = am?.memoryClass ?: 128
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val tier = when {
            lowRam || heapMb < 192 || cores <= 4 -> Tier.LOW
            heapMb >= 512 && cores >= 8 -> Tier.HIGH
            else -> Tier.MID
        }
        return Profile(tier, lowRam, cores, heapMb)
    }
}
