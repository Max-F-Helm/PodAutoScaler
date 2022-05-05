package com.mfhelm.podautoscaler.scaler.config.ruleset

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.mfhelm.podautoscaler.scaler.config.CountValueDeserializer
import java.util.Objects

internal class LimitRuleset(rules: List<LimitRule>) : Ruleset {

    companion object {
        const val TYPE = "limit"
    }

    private val limits: Array<LimitRule>

    init {
        var limits = rules.toTypedArray()
        limits.sortBy {
            it.minMessageCount
        }

        // if there is no rule with limit 0 then create one with the podCount of the smallest limit
        if (limits[0].minMessageCount != 0) {
            val smallestPodCount = limits[0].podCount
            limits = arrayOf(LimitRule(0, smallestPodCount), *limits)
        }

        this.limits = limits
    }

    override fun computePodCount(messageCount: Int, currentPodCount: Int): Int {
        // find rule with largest minMessageCount where minMessageCount <= messageCount
        for (i in 0..limits.size - 2) {
            if (limits[i + 1].minMessageCount > messageCount) {
                return limits[i].podCount
            }
        }

        return limits[limits.size - 1].podCount
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other === null) {
            return false
        }

        if (other !is LimitRuleset) {
            return false
        }

        return other.limits.contentEquals(limits)
    }

    override fun hashCode(): Int {
        return Objects.hash(LimitRuleset::class.qualifiedName, limits.contentHashCode())
    }

    override fun toString(): String {
        return "LimitRuleset: ${limits.contentToString()}"
    }
}

internal data class LimitRule(
    @JsonDeserialize(using = CountValueDeserializer::class) internal val minMessageCount: Int,
    internal val podCount: Int
)
