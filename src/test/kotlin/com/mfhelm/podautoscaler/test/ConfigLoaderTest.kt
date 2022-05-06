package com.mfhelm.podautoscaler.test

import com.mfhelm.podautoscaler.scaler.config.ConfigLoader
import com.mfhelm.podautoscaler.scaler.config.QueueConfig
import com.mfhelm.podautoscaler.scaler.config.ScalerConfig
import com.mfhelm.podautoscaler.scaler.config.ruleset.LimitRule
import com.mfhelm.podautoscaler.scaler.config.ruleset.LimitRuleset
import com.mfhelm.podautoscaler.scaler.config.ruleset.LinearScaleRule
import com.mfhelm.podautoscaler.scaler.config.ruleset.LinearScaleRuleset
import com.mfhelm.podautoscaler.scaler.config.ruleset.LogarithmicScaleRule
import com.mfhelm.podautoscaler.scaler.config.ruleset.LogarithmicScaleRuleset
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Component
import org.springframework.test.context.TestPropertySource
import kotlin.test.assertEquals

@SpringBootTest
@TestPropertySource(properties = ["config = "])
class ConfigLoaderTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun setupProperties() {
            System.setProperty("namespace", "nsp")
        }
    }

    @Autowired
    private lateinit var loader: ConfigLoaderBinding

    private fun readConfig(name: String): String {
        val res = ConfigLoaderTest::class.java.getResource("$name.yaml")
        if (res === null) {
            throw AssertionError("test-resource $name not found")
        }
        return res.readText()
    }

    @Test
    fun loadConfigWithLimitRuleset() {
        val configString = readConfig("LimitRuleset")

        val config = loader.loadConfig(configString)
        val expectedConfig = ArrayList<ScalerConfig>().apply {
            add(
                ScalerConfig(
                    "A", "NS-A", "P-A", 10,
                    setOf(
                        QueueConfig(
                            "VH-A", "Q-A",
                            LimitRuleset(
                                ArrayList<LimitRule>().apply {
                                    add(LimitRule(0, 1))
                                    add(LimitRule(100, 2))
                                    add(LimitRule(200, 3))
                                }
                            )
                        ),
                        QueueConfig(
                            "VH-A2", "Q-A2",
                            LimitRuleset(
                                ArrayList<LimitRule>().apply {
                                    add(LimitRule(0, 1))
                                    add(LimitRule(100, 2))
                                    add(LimitRule(200, 3))
                                }
                            )
                        )
                    )
                )
            )
            add(
                ScalerConfig(
                    "_unnamed_", "nsp", "P-B", 60,
                    setOf(
                        QueueConfig(
                            "/", "Q-B",
                            LimitRuleset(
                                ArrayList<LimitRule>().apply {
                                    add(LimitRule(0, 1))
                                    add(LimitRule(200, 2))
                                    add(LimitRule(500, 4))
                                }
                            )
                        )
                    )
                )
            )
        }

        assertEquals(expectedConfig, config)
    }

    @Test
    fun loadConfigWithLinearScaleRuleset() {
        val configString = readConfig("LinearScaleRuleset")

        val config = loader.loadConfig(configString)
        val expectedConfig = ArrayList<ScalerConfig>().apply {
            add(
                ScalerConfig(
                    "A", "NS-A", "P-A", 10,
                    setOf(
                        QueueConfig(
                            "VH-A", "Q-A",
                            LinearScaleRuleset(
                                LinearScaleRule(0.1, 2, 2, 20)
                            )
                        )
                    )
                )
            )
            add(
                ScalerConfig(
                    "_unnamed_", "NS-B", "P-B", 60,
                    setOf(
                        QueueConfig(
                            "/", "Q-B",
                            LinearScaleRuleset(
                                LinearScaleRule(1.0, 1, 1, 10)
                            )
                        )
                    )
                )
            )
        }
        assertEquals(expectedConfig, config)
    }

    @Test
    fun loadConfigWithLogarithmicScaleRuleset() {
        val configString = readConfig("LogarithmicScaleRuleset")

        val config = loader.loadConfig(configString)
        val expectedConfig = ArrayList<ScalerConfig>().apply {
            add(
                ScalerConfig(
                    "A", "NS-A", "P-A", 10,
                    setOf(
                        QueueConfig(
                            "VH-A", "Q-A",
                            LogarithmicScaleRuleset(
                                LogarithmicScaleRule(10.0, 2, 2, 20)
                            )
                        )
                    )
                )
            )
            add(
                ScalerConfig(
                    "_unnamed_", "NS-B", "P-B", 60,
                    setOf(
                        QueueConfig(
                            "/", "Q-B",
                            LogarithmicScaleRuleset(
                                LogarithmicScaleRule(10.0, 1, 1, 10)
                            )
                        )
                    )
                )
            )
            add(
                ScalerConfig(
                    "offset", "NS-C", "P-C", 60,
                    setOf(
                        QueueConfig(
                            "/", "Q-C",
                            LogarithmicScaleRuleset(
                                LogarithmicScaleRule(10.0, 1, 1, 10, -2)
                            )
                        )
                    )
                )
            )
        }
        assertEquals(expectedConfig, config)
    }

    @Test
    fun loadConfigWithUnits() {
        val configString = readConfig("UnitsConfig")

        val config = loader.loadConfig(configString)
        val expectedConfig = ArrayList<ScalerConfig>().apply {
            add(
                ScalerConfig(
                    "A", "NS-A", "P-A",
                    10,
                    setOf(
                        QueueConfig(
                            "VH-A", "Q-A",
                            LimitRuleset(
                                ArrayList<LimitRule>().apply {
                                    add(LimitRule(0, 1))
                                }
                            )
                        )
                    )
                )
            )
            add(
                ScalerConfig(
                    "B", "NS-A", "P-A",
                    10 * 60,
                    setOf(
                        QueueConfig(
                            "VH-A", "Q-A",
                            LimitRuleset(
                                ArrayList<LimitRule>().apply {
                                    add(LimitRule(1000, 1))
                                }
                            )
                        )
                    )
                )
            )
            add(
                ScalerConfig(
                    "C", "NS-A", "P-A",
                    10 * 60 * 60 * 24,
                    setOf(
                        QueueConfig(
                            "VH-A", "Q-A",
                            LimitRuleset(
                                ArrayList<LimitRule>().apply {
                                    add(LimitRule(1000000, 1))
                                }
                            )
                        )
                    )
                )
            )
        }
        assertEquals(expectedConfig, config)
    }
}

@Component
internal class ConfigLoaderBinding(
    @Value("\${config}") private val configString: String
) : ConfigLoader(configString) {

    public override fun loadConfig(src: String): List<ScalerConfig> {
        return super.loadConfig(src)
    }

    fun setConfig(conf: List<ScalerConfig>) {
        configEntries = conf
    }
}
