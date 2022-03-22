package com.mfhelm.podautoscaler.scaler.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.yaml.snakeyaml.Yaml
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

internal data class ScalerConfig(
    internal val label: String,
    internal val deploymentNamespace: String,
    internal val deployment: String,
    internal val interval: Long,
    internal val queueSet: Set<QueueConfig>
)

@Suppress("UNCHECKED_CAST")
@Component
internal class ConfigLoader(
    @Value("\${config}") private val configString: String,
    @Value("\${defaults.minPodCount}") private val defaultMinPodCount: Int,
    @Value("\${defaults.maxPodCount}") private val defaultMaxPodCount: Int,
    @Value("\${NAMESPACE:}") private val defaultNamespace: String
) {

    final lateinit var configEntries: List<ScalerConfig>
        protected set// protected for tests

    @PostConstruct
    private fun init() {
        configEntries = loadConfig(configString)
    }

    // protected for tests
    protected fun loadConfig(src: String): List<ScalerConfig> {
        if (src.isEmpty())
            return listOf()

        val yaml = Yaml()
        val entries: List<Map<String, Any>> = yaml.load(src)

        try {
            return entries.map {
                val label: String = it.getOrDefault("label", "_unnamed_") as String
                val deploymentNamespace = it["deploymentNamespace"] as String? ?: defaultNamespace
                if (deploymentNamespace.isEmpty()) {
                    throw InvalidConfigException("missing parameter 'deploymentNamespace' and no default is present")
                }
                val deployment = it["deployment"] as String?
                    ?: throw InvalidConfigException("missing parameter 'deployment'")

                val intervalStr = (it["interval"]
                    ?: throw InvalidConfigException("missing parameter 'interval'")).toString()
                val interval = parseTimeValue(intervalStr, "'interval'")

                val queues = it["queues"] as List<Map<String, *>>?
                    ?: throw InvalidConfigException("missing parameter 'queues'")
                val queueSet = queues.map {
                    val virtualHost = it["virtualHost"] as String? ?: "/"
                    val name = it["name"] as String?
                        ?: throw InvalidConfigException("missing parameter 'queues.name'")

                    val ruleset = it["ruleset"] as Map<String, *>?
                        ?: throw InvalidConfigException("missing parameter 'ruleset'")
                    val rulesType = ruleset["type"] as String?
                        ?: throw InvalidConfigException("missing parameter 'ruleset.type'")
                    val rules: Ruleset = when (rulesType) {
                        LimitRuleset.TYPE -> loadLimitRuleset(ruleset)
                        LinearScaleRuleset.TYPE -> loadLinearScaleRuleset(ruleset)
                        LogarithmicScaleRuleset.TYPE -> loadLogarithmicScaleRuleset(ruleset)
                        else -> throw InvalidConfigException("invalid value for 'ruleset.type'")
                    }

                    QueueConfig(virtualHost, name, rules)
                }.toSet()

                if (queueSet.isEmpty()) {
                    throw InvalidConfigException("'queues' must have at least one entry")
                }

                ScalerConfig(label, deploymentNamespace, deployment, interval, queueSet)
            }
        } catch (e: ClassCastException) {
            throw InvalidConfigException("parameter-value had wrong type", e)
        } catch (e: NullPointerException) {
            throw InvalidConfigException("missing value", e)
        }
    }

    private fun loadLimitRuleset(data: Map<String, *>): LimitRuleset {
        try {
            val type = data["type"] as String?
                ?: throw InvalidConfigException("missing parameter 'ruleset.type'")
            if (type != LimitRuleset.TYPE) {
                throw InvalidConfigException("wrong type for LimitRuleSet")
            }

            val rules: List<LimitRule>
            val rulesData = data["rules"] as List<Map<String, *>>?
                ?: throw InvalidConfigException("missing parameter 'ruleset.rules'")

            rules = rulesData.map {
                val minMessageCountStr = (it["minMessageCount"]
                    ?: throw InvalidConfigException("missing parameter 'ruleset.rules.minMessageCount'")).toString()
                val minMessageCount = parseCountValue(minMessageCountStr, "'ruleset.rules.minMessageCount'")

                val podCount = (it["podCount"] as Number?
                    ?: throw InvalidConfigException("missing parameter 'ruleset.rules.podCount'")).toInt()

                return@map LimitRule(minMessageCount, podCount)
            }

            if (rules.isEmpty()) {
                throw InvalidConfigException("'ruleset.rules' must contain at least one rule of type = '${LimitRuleset.TYPE}'")
            }

            return LimitRuleset(type, rules)
        } catch (e: ClassCastException) {
            throw InvalidConfigException("parameter-value had wrong type", e)
        } catch (e: NullPointerException) {
            throw InvalidConfigException("missing value", e)
        }
    }

    private fun loadLinearScaleRuleset(data: Map<String, *>): LinearScaleRuleset {
        try {
            val type = data["type"] as String?
                ?: throw InvalidConfigException("missing parameter 'ruleset.type'")
            if (type != LinearScaleRuleset.TYPE) {
                throw InvalidConfigException("wrong type for LinearScaleRuleset")
            }

            val rules: List<LinearScaleRule>

            val rulesData = data["rules"] as List<Map<String, *>>?
                ?: throw InvalidConfigException("missing parameter 'ruleset.rules'")

            rules = rulesData.map {
                val factor = (it["factor"] as Number?
                    ?: throw InvalidConfigException("missing parameter 'ruleset.rules.factor'")).toDouble()
                val stepThreshold = (it["stepThreshold"] as Number? ?: 1).toInt()
                val minPodCount = (it["minPodCount"] as Number? ?: defaultMinPodCount).toInt()
                val maxPodCount = (it["maxPodCount"] as Number? ?: defaultMaxPodCount).toInt()

                return@map LinearScaleRule(factor, stepThreshold, minPodCount, maxPodCount)
            }

            if (rules.size != 1) {
                throw InvalidConfigException("'ruleset.rules' must contain exactly one element if type = '${LinearScaleRuleset.TYPE}'")
            }

            return LinearScaleRuleset(type, rules[0])
        } catch (e: ClassCastException) {
            throw InvalidConfigException("parameter-value had wrong type", e)
        } catch (e: NullPointerException) {
            throw InvalidConfigException("missing value", e)
        }
    }

    private fun loadLogarithmicScaleRuleset(data: Map<String, *>): LogarithmicScaleRuleset {
        try {
            val type = data["type"] as String?
                ?: throw InvalidConfigException("missing parameter 'ruleset.type'")
            if (type != LogarithmicScaleRuleset.TYPE) {
                throw InvalidConfigException("wrong type for LogarithmicScaleRuleset")
            }

            val rules: List<LogarithmicScaleRule>

            val rulesData = data["rules"] as List<Map<String, *>>?
                ?: throw InvalidConfigException("missing parameter 'ruleset.rules'")

            rules = rulesData.map {
                val base = (it["base"] as Number?
                    ?: throw InvalidConfigException("missing parameter 'ruleset.rules.base'")).toDouble()
                val stepThreshold = (it["stepThreshold"] as Number? ?: 1).toInt()
                val minPodCount = (it["minPodCount"] as Number? ?: defaultMinPodCount).toInt()
                val maxPodCount = (it["maxPodCount"] as Number? ?: defaultMaxPodCount).toInt()

                return@map LogarithmicScaleRule(base, stepThreshold, minPodCount, maxPodCount)
            }

            if (rules.size != 1) {
                throw InvalidConfigException("'ruleset.rules' must contain exactly one element if type = '${LogarithmicScaleRuleset.TYPE}'")
            }

            return LogarithmicScaleRuleset(type, rules[0])
        } catch (e: ClassCastException) {
            throw InvalidConfigException("parameter-value had wrong type", e)
        } catch (e: NullPointerException) {
            throw InvalidConfigException("missing value", e)
        }
    }

    /**
     * Parses values like 5h to seconds.
     * Supported units are: s (seconds, default), m (minutes), h (hours) and d (days)
     * @param str the input to parse
     * @param configPos position in the config (to include in error messages)
     * @throw InvalidConfigException
     */
    private fun parseTimeValue(str: String, configPos: String): Long {
        if (str.isEmpty()) {
            throw InvalidConfigException("expected value for $configPos")
        }

        try {
            when (str[str.length - 1]) {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                    return str.toLong()
                }
                's' -> {// for consistency
                    return str.substring(0, str.length - 1).toLong()
                }
                'm' -> {
                    return TimeUnit.MINUTES.toSeconds(str.substring(0, str.length - 1).toLong())
                }
                'h' -> {
                    return TimeUnit.HOURS.toSeconds(str.substring(0, str.length - 1).toLong())
                }
                'd' -> {
                    return TimeUnit.DAYS.toSeconds(str.substring(0, str.length - 1).toLong())
                }
                else -> throw InvalidConfigException("invalid unit of value for $configPos")
            }
        } catch (e: NumberFormatException) {
            throw InvalidConfigException("invalid value for $configPos", e)
        }
    }

    /**
     * Parses values like 5m to number.
     * Supported units are: k (thousand), m (million)
     * @param str the input to parse
     * @param configPos position in the config (to include in error messages)
     * @throw InvalidConfigException
     */
    private fun parseCountValue(str: String, configPos: String): Int {
        if (str.isEmpty()) {
            throw InvalidConfigException("expected value for $configPos")
        }

        try {
            when (str[str.length - 1]) {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                    return str.toInt()
                }
                'k' -> {
                    return (str.substring(0, str.length - 1).toDouble() * 1000).toInt()
                }
                'm' -> {
                    return (str.substring(0, str.length - 1).toDouble() * 1000000).toInt()
                }
                else -> throw InvalidConfigException("invalid unit of value for $configPos")
            }
        } catch (e: NumberFormatException) {
            throw InvalidConfigException("invalid value for $configPos", e)
        }
    }
}
