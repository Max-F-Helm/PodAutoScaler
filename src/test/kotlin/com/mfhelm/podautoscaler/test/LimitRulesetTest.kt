package com.mfhelm.podautoscaler.test

import com.mfhelm.podautoscaler.scaler.config.LimitRule
import com.mfhelm.podautoscaler.scaler.config.LimitRuleset
import org.junit.jupiter.api.*
import kotlin.test.assertEquals

class LimitRulesetTest {

    companion object {
        private lateinit var ruleSet: LimitRuleset

        @BeforeAll
        @JvmStatic
        fun init() {
            ruleSet = LimitRuleset("limit", ArrayList<LimitRule>().apply {
                add(LimitRule(50, 2))
                add(LimitRule(0, 1))
                add(LimitRule(800, 6))
                add(LimitRule(200, 3))
                add(LimitRule(400, 4))
            })
        }
    }

    @Test
    fun testLimit_one_l1() {
        assertEquals(1, ruleSet.computePodCount(0, 9999))
    }

    @Test
    fun testLimit_one_l2() {
        assertEquals(1, ruleSet.computePodCount(1, 9999))
    }

    @Test
    fun testLimit_one_l3() {
        assertEquals(1, ruleSet.computePodCount(49, 9999))
    }

    @Test
    fun testLimit_two_l1() {
        assertEquals(2, ruleSet.computePodCount(50, 9999))
    }

    @Test
    fun testLimit_two_l2() {
        assertEquals(2, ruleSet.computePodCount(51, 9999))
    }

    @Test
    fun testLimit_two_l3() {
        assertEquals(2, ruleSet.computePodCount(199, 9999))
    }

    @Test
    fun testLimit_three_l1() {
        assertEquals(3, ruleSet.computePodCount(200, 9999))
    }

    @Test
    fun testLimit_three_l2() {
        assertEquals(3, ruleSet.computePodCount(201, 9999))
    }

    @Test
    fun testLimit_three_l3() {
        assertEquals(3, ruleSet.computePodCount(399, 9999))
    }

    @Test
    fun testLimit_four_l1() {
        assertEquals(4, ruleSet.computePodCount(400, 9999))
    }

    @Test
    fun testLimit_four_l2() {
        assertEquals(4, ruleSet.computePodCount(401, 9999))
    }

    @Test
    fun testLimit_four_l3() {
        assertEquals(4, ruleSet.computePodCount(799, 9999))
    }

    @Test
    fun testLimit_six_l1() {
        assertEquals(6, ruleSet.computePodCount(800, 9999))
    }

    @Test
    fun testLimit_six_l2() {
        assertEquals(6, ruleSet.computePodCount(801, 9999))
    }

    @Test
    fun testLimit_six_l3() {
        assertEquals(6, ruleSet.computePodCount(1000, 9999))
    }

    @Test
    fun testLimit_noChange() {
        assertEquals(1, ruleSet.computePodCount(0, 1))
        assertEquals(1, ruleSet.computePodCount(12, 1))
        assertEquals(6, ruleSet.computePodCount(1000, 6))
    }

    @Test
    fun testLimit_noZero() {
        val ruleset = LimitRuleset("limit", ArrayList<LimitRule>().apply {
            add(LimitRule(30, 3))
            add(LimitRule(2, 2))
        })

        assertEquals(2, ruleset.computePodCount(0, 9999))
        assertEquals(2, ruleset.computePodCount(1, 9999))
        assertEquals(2, ruleset.computePodCount(2, 9999))
        assertEquals(3, ruleset.computePodCount(30, 9999))
    }
}
