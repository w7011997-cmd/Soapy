package com.soapy.app.data

import android.content.Context
import java.util.Calendar

const val DAY_MS = 86_400_000L
const val HOUR_MS = 3_600_000L

class ProgressStats(
    val streakMs: Long,
    val longestMs: Long,
    val sinceStartMs: Long,
    val slipCount: Int
) {
    val streakDays: Int get() = (streakMs / DAY_MS).toInt()
    val streakHours: Int get() = ((streakMs % DAY_MS) / HOUR_MS).toInt()
    val longestDays: Int get() = (longestMs / DAY_MS).toInt()
    val sinceStartDays: Int get() = (sinceStartMs / DAY_MS).toInt()
}

object ProgressMath {
    val MILESTONES = listOf(1, 3, 7, 14, 30, 60, 90, 180, 365)

    /** The streak runs from the start (or the last slip) until now. */
    fun compute(startAt: Long, slips: List<Long>, now: Long): ProgressStats {
        val valid = slips.filter { it >= startAt && it <= now }.sorted()
        val points = listOf(startAt) + valid + now
        val gaps = points.zipWithNext { a, b -> (b - a).coerceAtLeast(0L) }
        return ProgressStats(
            streakMs = gaps.last(),
            longestMs = gaps.max(),
            sinceStartMs = (now - startAt).coerceAtLeast(0L),
            slipCount = valid.size
        )
    }

    fun nextMilestone(days: Int): Int? = MILESTONES.firstOrNull { it > days }

    fun previousMilestone(days: Int): Int = MILESTONES.lastOrNull { it <= days } ?: 0

    /** Local midnight, [daysBack] days before the day that contains [ts]. */
    fun startOfDay(ts: Long, daysBack: Int = 0): Long =
        Calendar.getInstance().apply {
            timeInMillis = ts
            add(Calendar.DAY_OF_YEAR, -daysBack)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}

/** When the streak started. Kept on the device only. */
class ProgressStore private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("soapy_progress", Context.MODE_PRIVATE)

    fun isSetupDone(): Boolean = prefs.getBoolean(KEY_DONE, false)

    fun startAt(): Long = prefs.getLong(KEY_START, 0L)

    /** [daysAlready] is how many days the user says they have already been clean. */
    fun begin(daysAlready: Int) {
        val start = System.currentTimeMillis() - daysAlready.coerceIn(0, 365) * DAY_MS
        prefs.edit().putLong(KEY_START, start).putBoolean(KEY_DONE, true).apply()
    }

    companion object {
        private const val KEY_START = "start_at"
        private const val KEY_DONE = "setup_done"

        @Volatile
        private var instance: ProgressStore? = null

        fun get(context: Context): ProgressStore =
            instance ?: synchronized(this) {
                instance ?: ProgressStore(context.applicationContext).also { instance = it }
            }
    }
}
