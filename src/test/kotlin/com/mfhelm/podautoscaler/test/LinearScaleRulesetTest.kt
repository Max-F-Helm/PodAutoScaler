package com.mfhelm.podautoscaler.test

import com.mfhelm.podautoscaler.scaler.config.ruleset.LinearScaleRule
import com.mfhelm.podautoscaler.scaler.config.ruleset.LinearScaleRuleset
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LinearScaleRulesetTest {

    @Test
    fun testLinearScale_scale_1() {
        val ruleset = LinearScaleRuleset(LinearScaleRule(0.1, 1, 1, 50))
        assertEquals(1, ruleset.computePodCount(1, 9999))
        assertEquals(1, ruleset.computePodCount(12, 9999))
        assertEquals(2, ruleset.computePodCount(15, 9999))
        assertEquals(20, ruleset.computePodCount(198, 9999))
        assertEquals(20, ruleset.computePodCount(204, 9999))
    }

    @Test
    fun testLinearScale_scaleClamped_1() {
        val ruleset = LinearScaleRuleset(LinearScaleRule(0.1, 1, 1, 20))
        assertEquals(1, ruleset.computePodCount(1, 9999))
        assertEquals(2, ruleset.computePodCount(15, 9999))
        assertEquals(20, ruleset.computePodCount(204, 9999))
        assertEquals(20, ruleset.computePodCount(590, 9999))
    }

    @Test
    fun testLinearScale_scaleClamped_2() {
        val ruleset = LinearScaleRuleset(LinearScaleRule(0.1, 1, 5, 20))
        assertEquals(5, ruleset.computePodCount(1, 9999))
        assertEquals(5, ruleset.computePodCount(15, 9999))
        assertEquals(7, ruleset.computePodCount(70, 9999))
        assertEquals(20, ruleset.computePodCount(204, 9999))
        assertEquals(20, ruleset.computePodCount(590, 9999))
    }

    @Test
    fun testLinearScale_scaleStep_5() {
        val ruleset = LinearScaleRuleset(LinearScaleRule(0.1, 5, 1, 50))
        assertEquals(1, ruleset.computePodCount(1, 9999))
        assertEquals(5, ruleset.computePodCount(48, 0))
        assertEquals(7, ruleset.computePodCount(70, 12))
        assertEquals(25, ruleset.computePodCount(252, 9999))
    }

    @Test
    fun testLinearScale_scaleStepClamped_5() {
        val ruleset = LinearScaleRuleset(LinearScaleRule(0.1, 5, 2, 10))
        assertEquals(2, ruleset.computePodCount(1, 9999))
        assertEquals(4, ruleset.computePodCount(35, 9))
        assertEquals(5, ruleset.computePodCount(48, 9999))
        assertEquals(10, ruleset.computePodCount(100, 9999))
        assertEquals(10, ruleset.computePodCount(252, 9999))
    }

    @Test
    fun testLinearScale_noChange() {
        val ruleset1 = LinearScaleRuleset(LinearScaleRule(0.1, 1, 1, 50))
        assertEquals(1, ruleset1.computePodCount(1, 1))
        assertEquals(1, ruleset1.computePodCount(12, 1))
        assertEquals(2, ruleset1.computePodCount(15, 2))
        assertEquals(20, ruleset1.computePodCount(198, 20))
        assertEquals(20, ruleset1.computePodCount(204, 20))

        val ruleset2 = LinearScaleRuleset(LinearScaleRule(0.1, 5, 1, 50))
        assertEquals(10, ruleset2.computePodCount(70, 10))
    }
}
