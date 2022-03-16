package com.mfhelm.podautoscaler.test

import com.mfhelm.podautoscaler.scaler.config.*
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Component
import org.springframework.test.context.TestPropertySource
import kotlin.test.assertEquals

@SpringBootTest
@TestPropertySource(properties = ["config = ", "NAMESPACE = nsp"])
class ConfigLoaderTest {

    @Autowired
    private lateinit var loader: ConfigLoaderBinding

    @Test
    fun loadConfigWithLimitRuleset(){
        val configString = """
            -
              label: A
              deploymentNamespace: NS-A
              deployment: P-A
              interval: 10
              queues:
                -
                  virtualHost: VH-A
                  name: Q-A
                  ruleset:
                    type: limit
                    rules:
                      -
                        minMessageCount: 0
                        podCount: 1
                      -
                        minMessageCount: 100
                        podCount: 2
                      -
                        minMessageCount: 200
                        podCount: 3
                -
                  virtualHost: VH-A2
                  name: Q-A2
                  ruleset:
                    type: limit
                    rules:
                      -
                        minMessageCount: 0
                        podCount: 1
                      -
                        minMessageCount: 100
                        podCount: 2
                      -
                        minMessageCount: 200
                        podCount: 3
            -
              deployment: P-B
              interval: 60
              queues:
                -
                  name: Q-B
                  ruleset:
                    type: limit
                    rules:
                      -
                        minMessageCount: 0
                        podCount: 1
                      -
                        minMessageCount: 200
                        podCount: 2
                      -
                        minMessageCount: 500
                        podCount: 4
        """.trimIndent()

        val config = loader.loadConfig(configString)
        val expectedConfig = ArrayList<ScalerConfig>().apply{
            add(
                ScalerConfig("A", "NS-A", "P-A", 10,
                    setOf(
                        QueueConfig("VH-A", "Q-A",
                            LimitRuleset("limit", ArrayList<LimitRule>().apply{
                                add(LimitRule(0, 1))
                                add(LimitRule(100, 2))
                                add(LimitRule(200, 3))
                            })
                        ),
                        QueueConfig("VH-A2", "Q-A2",
                            LimitRuleset("limit", ArrayList<LimitRule>().apply{
                                add(LimitRule(0, 1))
                                add(LimitRule(100, 2))
                                add(LimitRule(200, 3))
                            })
                        )
                    )
                )
            )
            add(
                ScalerConfig("_unnamed_", "nsp", "P-B", 60,
                    setOf(QueueConfig("/", "Q-B",
                        LimitRuleset("limit", ArrayList<LimitRule>().apply{
                            add(LimitRule(0, 1))
                            add(LimitRule(200, 2))
                            add(LimitRule(500, 4))
                }))))
            )
        }

        assertEquals(expectedConfig, config)
    }

    @Test
    fun loadConfigWithLinearScaleRuleset(){
        val configString = """
            -
              label: A
              deploymentNamespace: NS-A
              deployment: P-A
              interval: 10
              queues:
                -
                  virtualHost: VH-A
                  name: Q-A
                  ruleset:
                    type: linearScale
                    rules:
                      -
                        factor: 0.1
                        stepThreshold: 2
                        minPodCount: 2
                        maxPodCount: 20
            -
              deploymentNamespace: NS-B
              deployment: P-B
              interval: 60
              queues:
                -
                  name: Q-B
                  ruleset:
                    type: linearScale
                    rules:
                      -
                        factor: 1
        """.trimIndent()

        val config = loader.loadConfig(configString)
        val expectedConfig = ArrayList<ScalerConfig>().apply{
            add(
                ScalerConfig("A", "NS-A", "P-A", 10,
                    setOf(QueueConfig("VH-A", "Q-A",
                        LinearScaleRuleset("linearScale",
                            LinearScaleRule(0.1, 2, 2, 20)
                        )
                    ))
                )
            )
            add(
                ScalerConfig("_unnamed_", "NS-B", "P-B", 60,
                    setOf(QueueConfig("/", "Q-B",
                        LinearScaleRuleset("linearScale",
                            LinearScaleRule(1.0, 1, 1, 10)
                        )
                    ))
                )
            )
        }
        assertEquals(expectedConfig, config)
    }

    @Test
    fun loadConfigWithLogarithmicScaleRuleset(){
        val configString = """
            -
              label: A
              deploymentNamespace: NS-A
              deployment: P-A
              interval: 10
              queues:
                -
                  virtualHost: VH-A
                  name: Q-A
                  ruleset:
                    type: logScale
                    rules:
                      -
                        base: 10
                        stepThreshold: 2
                        minPodCount: 2
                        maxPodCount: 20
            -
              deploymentNamespace: NS-B
              deployment: P-B
              interval: 60
              queues:
                -
                  name: Q-B
                  ruleset:
                    type: logScale
                    rules:
                      -
                        base: 10
        """.trimIndent()

        val config = loader.loadConfig(configString)
        val expectedConfig = ArrayList<ScalerConfig>().apply{
            add(
                ScalerConfig("A", "NS-A", "P-A", 10,
                    setOf(QueueConfig("VH-A", "Q-A",
                        LogarithmicScaleRuleset("logScale",
                            LogarithmicScaleRule(10.0, 2, 2, 20)
                        )
                    ))
                )
            )
            add(
                ScalerConfig("_unnamed_", "NS-B", "P-B", 60,
                    setOf(QueueConfig("/", "Q-B",
                        LogarithmicScaleRuleset("logScale",
                            LogarithmicScaleRule(10.0, 1, 1, 10)
                        )
                    ))
                )
            )
        }
        assertEquals(expectedConfig, config)
    }

    @Test
    fun loadConfigWithUnits(){
        val configString = """
            -
              label: A
              deploymentNamespace: NS-A
              deployment: P-A
              interval: 10s
              queues:
                - virtualHost: VH-A
                  name: Q-A
                  ruleset:
                    type: limit
                    rules:
                      -
                        minMessageCount: 0
                        podCount: 1
            -
              label: B
              deploymentNamespace: NS-A
              deployment: P-A
              interval: 10m
              queues:
                - virtualHost: VH-A
                  name: Q-A
                  ruleset:
                    type: limit
                    rules:
                      -
                        minMessageCount: 1k
                        podCount: 1
            -
              label: C
              deploymentNamespace: NS-A
              deployment: P-A
              interval: 10d
              queues:
                - virtualHost: VH-A
                  name: Q-A
                  ruleset:
                    type: limit
                    rules:
                      -
                        minMessageCount: 1m
                        podCount: 1
        """.trimIndent()

        val config = loader.loadConfig(configString)
        val expectedConfig = ArrayList<ScalerConfig>().apply{
            add(
                ScalerConfig("A", "NS-A", "P-A",
                    10,
                    setOf(QueueConfig("VH-A", "Q-A",
                        LimitRuleset("limit", ArrayList<LimitRule>().apply {
                            add(LimitRule(0, 1))
                        })
                    ))
                )
            )
            add(
                ScalerConfig("B", "NS-A", "P-A",
                    10 * 60,
                    setOf(QueueConfig("VH-A", "Q-A",
                        LimitRuleset("limit", ArrayList<LimitRule>().apply {
                            add(LimitRule(1000, 1))
                        })
                    ))
                )
            )
            add(
                ScalerConfig("C", "NS-A", "P-A",
                    10 * 60 * 60 * 24,
                    setOf(QueueConfig("VH-A", "Q-A",
                        LimitRuleset("limit", ArrayList<LimitRule>().apply {
                            add(LimitRule(1000000, 1))
                        })
                    ))
                )
            )
        }
        assertEquals(expectedConfig, config)
    }
}

@Component
internal class ConfigLoaderBinding : ConfigLoader() {
    public override fun loadConfig(src: String): List<ScalerConfig> {
        return super.loadConfig(src)
    }
}
