package com.mfhelm.podautoscaler.scaler.config

import java.util.Objects
import kotlin.math.abs
import kotlin.math.log
import kotlin.math.roundToInt

internal class LogarithmicScaleRuleset(override val type: String, internal val rule: LogarithmicScaleRule) : Ruleset {

    companion object{
        const val TYPE = "logScale"
    }

    override fun computePodCount(messageCount: Int, currentPodCount: Int): Int {
        val newCount = log(messageCount.toDouble(), rule.base).roundToInt()
            .coerceIn(rule.minPodCount, rule.maxPodCount)
        val overThreshold = abs(currentPodCount - newCount) >= rule.stepThreshold
        return if(overThreshold) newCount else -1
    }

    override fun equals(other: Any?): Boolean {
        if(other === this) return true
        if(other === null) return false

        if(other !is LogarithmicScaleRuleset)
            return false

        return other.type == type && other.rule == rule
    }

    override fun hashCode(): Int {
        return Objects.hash(LogarithmicScaleRuleset::class.qualifiedName, type, rule)
    }

    override fun toString(): String {
        return "LogarithmicScaleRuleset: $rule"
    }
}

internal data class LogarithmicScaleRule(
    internal val base: Double,
    internal val stepThreshold: Int,
    internal val minPodCount: Int,
    internal val maxPodCount: Int
)
