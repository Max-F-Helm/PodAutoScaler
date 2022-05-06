package com.mfhelm.podautoscaler.scaler.config.ruleset

import com.mfhelm.podautoscaler.scaler.config.DEFAULT_LOG_OFFSET
import com.mfhelm.podautoscaler.scaler.config.DEFAULT_MAX_POD_COUNT
import com.mfhelm.podautoscaler.scaler.config.DEFAULT_MIN_POD_COUNT
import com.mfhelm.podautoscaler.scaler.config.DEFAULT_STEP_THRESHOLD
import java.util.Objects
import kotlin.math.abs
import kotlin.math.log
import kotlin.math.roundToInt

internal class LogarithmicScaleRuleset(internal val rule: LogarithmicScaleRule) : Ruleset {

    companion object {
        const val TYPE = "logScale"
    }

    override fun computePodCount(messageCount: Int, currentPodCount: Int): Int {
        val newCount = calcLog(messageCount)
            .coerceIn(rule.minPodCount, rule.maxPodCount)

        val overThreshold = abs(currentPodCount - newCount) >= rule.stepThreshold
        return if (overThreshold) newCount else currentPodCount
    }

    private fun calcLog(messageCount: Int): Int {
        return (log(messageCount.coerceAtLeast(1).toDouble(), rule.base) + rule.offset)
            .roundToInt()
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other === null) {
            return false
        }

        if (other !is LogarithmicScaleRuleset) {
            return false
        }

        return other.rule == rule
    }

    override fun hashCode(): Int {
        return Objects.hash(LogarithmicScaleRuleset::class.qualifiedName, rule)
    }

    override fun toString(): String {
        return "LogarithmicScaleRuleset: $rule"
    }
}

internal data class LogarithmicScaleRule(
    internal val base: Double,
    internal val stepThreshold: Int = DEFAULT_STEP_THRESHOLD,
    internal val minPodCount: Int = DEFAULT_MIN_POD_COUNT,
    internal val maxPodCount: Int = DEFAULT_MAX_POD_COUNT,
    internal val offset: Int = DEFAULT_LOG_OFFSET
)
