package com.mfhelm.podautoscaler.scaler.config

import java.util.Objects
import kotlin.math.abs
import kotlin.math.roundToInt

internal class LinearScaleRuleset(override val type: String, internal val rule: LinearScaleRule) : Ruleset {

    companion object {
        const val TYPE = "linearScale"
    }

    override fun computePodCount(messageCount: Int, currentPodCount: Int): Int {
        val newCount = (messageCount * rule.factor).roundToInt().coerceIn(rule.minPodCount, rule.maxPodCount)
        val overThreshold = abs(currentPodCount - newCount) >= rule.stepThreshold
        return if (overThreshold) newCount else currentPodCount
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other === null) {
            return false
        }

        if (other !is LinearScaleRuleset) {
            return false
        }

        return other.type == type && other.rule == rule
    }

    override fun hashCode(): Int {
        return Objects.hash(LinearScaleRuleset::class.qualifiedName, type, rule)
    }

    override fun toString(): String {
        return "LinearScaleRuleset: $rule"
    }
}

internal data class LinearScaleRule(
    internal val factor: Double,
    internal val stepThreshold: Int,
    internal val minPodCount: Int,
    internal val maxPodCount: Int
)
