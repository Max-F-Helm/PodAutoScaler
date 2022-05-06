package com.mfhelm.podautoscaler.test

import com.mfhelm.podautoscaler.scaler.config.ruleset.LogarithmicScaleRule
import com.mfhelm.podautoscaler.scaler.config.ruleset.LogarithmicScaleRuleset
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LogarithmicScaleRulesetTest {

    @Test
    fun testLogarithmicScale_base_10() {
        val ruleset = LogarithmicScaleRuleset(LogarithmicScaleRule(10.0, 1, 1, 50))
        assertEquals(1, ruleset.computePodCount(1, 9999))
        assertEquals(1, ruleset.computePodCount(12, 9999))
        assertEquals(2, ruleset.computePodCount(99, 9999))
        assertEquals(2, ruleset.computePodCount(100, 9999))
        assertEquals(2, ruleset.computePodCount(204, 9999))
        assertEquals(1, ruleset.computePodCount(0, 9999))
    }

    @Test
    fun testLogarithmicScale_baseClamped_1() {
        val ruleset = LogarithmicScaleRuleset(LogarithmicScaleRule(10.0, 1, 1, 3))
        assertEquals(1, ruleset.computePodCount(1, 9999))
        assertEquals(2, ruleset.computePodCount(100, 9999))
        assertEquals(3, ruleset.computePodCount(1001, 9999))
        assertEquals(3, ruleset.computePodCount(999990, 9999))
    }

    @Test
    fun testLogarithmicScale_baseClamped_2() {
        val ruleset = LogarithmicScaleRuleset(LogarithmicScaleRule(10.0, 1, 2, 3))
        assertEquals(2, ruleset.computePodCount(1, 9999))
        assertEquals(2, ruleset.computePodCount(100, 9999))
        assertEquals(3, ruleset.computePodCount(1001, 9999))
        assertEquals(3, ruleset.computePodCount(999990, 9999))
    }

    @Test
    fun testLogarithmicScale_baseStep_2() {
        val ruleset = LogarithmicScaleRuleset(LogarithmicScaleRule(5.0, 2, 1, 50))
        assertEquals(3, ruleset.computePodCount(200, 9999))
        assertEquals(4, ruleset.computePodCount(500, 2))
    }

    @Test
    fun testLogarithmicScale_baseStepClamped_5() {
        val ruleset = LogarithmicScaleRuleset(LogarithmicScaleRule(2.0, 5, 2, 10))
        assertEquals(2, ruleset.computePodCount(1, 9999))
        assertEquals(7, ruleset.computePodCount(100, 1))
        assertEquals(7, ruleset.computePodCount(100, 2))
        assertEquals(10, ruleset.computePodCount(4000, 9999))
        assertEquals(10, ruleset.computePodCount(4000, 5))
    }

    @Test
    fun testLogarithmicScale_noChange() {
        val ruleset1 = LogarithmicScaleRuleset(LogarithmicScaleRule(10.0, 1, 1, 50))
        assertEquals(1, ruleset1.computePodCount(1, 1))
        assertEquals(1, ruleset1.computePodCount(12, 1))
        assertEquals(2, ruleset1.computePodCount(101, 2))

        val ruleset2 = LogarithmicScaleRuleset(LogarithmicScaleRule(5.0, 2, 1, 50))
        assertEquals(3, ruleset2.computePodCount(500, 3))
    }

    @Test
    fun testLogarithmicScale_offset() {
        val ruleset1 = LogarithmicScaleRuleset(
            LogarithmicScaleRule(
                10.0, 1,
                1, 50,
                1
            )
        )
        assertEquals(1, ruleset1.computePodCount(1, 10))
        assertEquals(2, ruleset1.computePodCount(12, 10))
        assertEquals(3, ruleset1.computePodCount(101, 10))

        val ruleset2 = LogarithmicScaleRuleset(
            LogarithmicScaleRule(
                5.0, 2,
                1, 50,
                -2
            )
        )
        assertEquals(2, ruleset2.computePodCount(500, 10))
    }
}
