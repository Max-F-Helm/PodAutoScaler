package com.mfhelm.podautoscaler.scaler.config

import java.util.*

internal class LimitRuleset(override val type: String, rules: List<LimitRule>) : Ruleset {

    companion object{
        const val TYPE = "limit"
    }

    private val limits: Array<LimitRule>

    init{
        var limits = rules.toTypedArray()
        // sort rules by limit for binary search in computePodCount
        limits.sortBy {
            it.minMessageCount
        }

        // if there is no rule with limit 0 then create one with the podCount of the smallest limit
        if(limits[0].minMessageCount != 0){
            val smallestPodCount = limits[0].podCount
            limits = arrayOf(LimitRule(0, smallestPodCount), *limits)
        }

        this.limits = limits
    }

    override fun computePodCount(messageCount: Int, currentPodCount: Int): Int{
        // find rule with largest minMessageCount where minMessageCount >= messageCount
        // use binary search as long as direction stays the same
        var newCount = -1
        var l = 0
        var r = limits.size
        while(l <= r){
            val mid = (l + r) / 2
            val limit = limits[mid]

            if(limit.minMessageCount <= messageCount){
                if(mid == limits.size - 1){
                    newCount = limit.podCount
                    break
                }
                if(mid < limits.size - 1){
                    val nextCount = limits[mid + 1].minMessageCount
                    if(nextCount > messageCount){
                        newCount = limit.podCount
                        break
                    }
                }
            }

            if(limit.minMessageCount > messageCount) {
                r = mid - 1
            }else if(limit.minMessageCount < messageCount) {
                l = mid + 1
            }else{
                break
            }
        }

        if(newCount == -1)
            throw AssertionError("this algorithm is broken")

        return if(newCount != currentPodCount) newCount else -1
    }

    override fun equals(other: Any?): Boolean {
        if(other === this) return true
        if(other === null) return false

        if(other !is LimitRuleset)
            return false

        return other.type == type && other.limits.contentEquals(limits)
    }

    override fun hashCode(): Int {
        return Objects.hash(LimitRuleset::class.qualifiedName, type, limits.contentHashCode())
    }

    override fun toString(): String {
        return "LimitRuleset: ${limits.contentToString()}"
    }
}

internal data class LimitRule(internal val minMessageCount: Int, internal val podCount: Int)
