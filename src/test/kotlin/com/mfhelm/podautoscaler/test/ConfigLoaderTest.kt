package com.mfhelm.podautoscaler.test

import com.mfhelm.podautoscaler.scaler.config.*
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Component
import org.springframework.test.context.TestPropertySource
import kotlin.test.assertEquals

@SpringBootTest
@TestPropertySource(properties = ["config = "])
class ConfigLoaderTest {

    @Autowired
    private lateinit var loader: ConfigLoaderBinding

    @Test
    fun loadConfigWithLimitRuleset(){
        val configString = """
            -
              label: A
              queueVirtualHost: VH-A
              queueName: Q-A
              podNamespace: NS-A
              pod: P-A
              interval: 10
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
              queueName: Q-B
              podNamespace: NS-B
              pod: P-B
              interval: 60
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
                ScalerConfig("A", "VH-A", "Q-A", "NS-A", "P-A", 10,
                LimitRuleset("limit", ArrayList<LimitRule>().apply{
                    add(LimitRule(0, 1))
                    add(LimitRule(100, 2))
                    add(LimitRule(200, 3))
                }))
            )
            add(
                ScalerConfig("_unnamed_", "/", "Q-B", "NS-B", "P-B", 60,
                LimitRuleset("limit", ArrayList<LimitRule>().apply{
                    add(LimitRule(0, 1))
                    add(LimitRule(200, 2))
                    add(LimitRule(500, 4))
                }))
            )
        }
        assertEquals(expectedConfig, config)
    }

    @Test
    fun loadConfigWithLinearScaleRuleset(){
        val configString = """
            -
              label: A
              queueVirtualHost: VH-A
              queueName: Q-A
              podNamespace: NS-A
              pod: P-A
              interval: 10
              ruleset:
                type: linearScale
                rules:
                  -
                    factor: 0.1
                    stepThreshold: 2
                    minPodCount: 2
                    maxPodCount: 20
            -
              queueName: Q-B
              podNamespace: NS-B
              pod: P-B
              interval: 60
              ruleset:
                type: linearScale
                rules:
                  -
                    factor: 1
        """.trimIndent()

        val config = loader.loadConfig(configString)
        val expectedConfig = ArrayList<ScalerConfig>().apply{
            add(
                ScalerConfig("A", "VH-A", "Q-A", "NS-A", "P-A", 10,
                LinearScaleRuleset("linearScale",
                    LinearScaleRule(0.1, 2, 2, 20)
                )
            )
            )
            add(
                ScalerConfig("_unnamed_", "/", "Q-B", "NS-B", "P-B", 60,
                LinearScaleRuleset("linearScale",
                    LinearScaleRule(1.0, 1, 1, 9999)
                )
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
