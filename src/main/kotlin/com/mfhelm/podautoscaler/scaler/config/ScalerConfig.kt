package com.mfhelm.podautoscaler.scaler.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.yaml.snakeyaml.Yaml
import javax.annotation.PostConstruct

internal data class ScalerConfig(
    internal val label: String,
    internal val queueVirtualHost: String,
    internal val queueName: String,
    internal val deploymentNamespace: String,
    internal val deployment: String,
    internal val interval: Long,
    internal val ruleset: Ruleset
)

@Component
internal open class ConfigLoader{

    internal final lateinit var configEntries: List<ScalerConfig>
        private set

    @Value("\${config}")
    private lateinit var configString: String
    @Value("\${defaults.minPodCount}")
    private var defaultMinPodCount: Int = 0
    @Value("\${defaults.maxPodCount}")
    private var defaultMaxPodCount: Int = 0
    @Value("\${NAMESPACE:}")
    private lateinit var defaultNamespace: String

    @PostConstruct
    private fun init(){
        configEntries = loadConfig(configString)
    }

    // protected for tests
    protected open fun loadConfig(src: String): List<ScalerConfig>{
        if(src.isEmpty())
            return listOf()

        val yaml = Yaml()
        val entries: List<Map<String, Any>> = yaml.load(src)

        try {
            return entries.map {
                val label: String = it.getOrDefault("label", "_unnamed_") as String
                val queueVirtualHost = it["queueVirtualHost"] as String? ?: "/"
                val queueName = it["queueName"] as String?
                    ?: throw InvalidConfigException("missing parameter 'queueName'")
                val deploymentNamespace = it["deploymentNamespace"] as String? ?: defaultNamespace
                if(deploymentNamespace.isEmpty())
                    throw InvalidConfigException("missing parameter 'deploymentNamespace' and no default is present")
                val deployment = it["deployment"] as String?
                    ?: throw InvalidConfigException("missing parameter 'deployment'")
                val interval = (it["interval"] as Number?
                    ?: throw InvalidConfigException("missing parameter 'interval'")).toLong()

                val ruleset = it["ruleset"] as Map<String, Any>?
                    ?: throw InvalidConfigException("missing parameter 'ruleset'")
                val rulesType = ruleset["type"] as String?
                    ?: throw InvalidConfigException("missing parameter 'ruleset.type'")
                val rules: Ruleset = when(rulesType){
                    LimitRuleset.TYPE -> loadLimitRuleset(ruleset)
                    LinearScaleRuleset.TYPE -> loadLinearScaleRuleset(ruleset)
                    LogarithmicScaleRuleset.TYPE -> loadLogarithmicScaleRuleset(ruleset)
                    else -> throw InvalidConfigException("invalid value for 'ruleset.type'")
                }

                return@map ScalerConfig(label, queueVirtualHost, queueName, deploymentNamespace, deployment, interval, rules)
            }
        }catch (e: ClassCastException){
            throw InvalidConfigException("parameter-value had wrong type", e)
        }catch (e: NullPointerException){
            throw InvalidConfigException("missing value", e)
        }
    }

    private fun loadLimitRuleset(data: Map<String, Any>): LimitRuleset {
        try{
            val type = data["type"] as String?
                ?: throw InvalidConfigException("missing parameter 'ruleset.type'")
            if(type != LimitRuleset.TYPE)
                throw InvalidConfigException("wrong type for LimitRuleSet")

            val rules: List<LimitRule>
            val rulesData = data["rules"] as List<Map<String, Any>>?
                ?: throw InvalidConfigException("missing parameter 'ruleset.rules'")

            rules = rulesData.map{
                val minMessageCount = (it["minMessageCount"] as Number?
                    ?: throw InvalidConfigException("missing parameter 'ruleset.rules.minMessageCount'")).toInt()
                val podCount = (it["podCount"] as Number?
                    ?: throw InvalidConfigException("missing parameter 'ruleset.rules.podCount'")).toInt()

                return@map LimitRule(minMessageCount, podCount)
            }

            if(rules.isEmpty())
                throw InvalidConfigException("'ruleset.rules' must contain at least one rule if type = '${LimitRuleset.TYPE}'")

            return LimitRuleset(type, rules)
        }catch (e: ClassCastException){
            throw InvalidConfigException("parameter-value had wrong type", e)
        }catch (e: NullPointerException){
            throw InvalidConfigException("missing value", e)
        }
    }

    private fun loadLinearScaleRuleset(data: Map<String, Any>): LinearScaleRuleset {
        try{
            val type = data["type"] as String?
                ?: throw InvalidConfigException("missing parameter 'ruleset.type'")
            if(type != LinearScaleRuleset.TYPE)
                throw InvalidConfigException("wrong type for LinearScaleRuleset")

            val rules: List<LinearScaleRule>

            val rulesData = data["rules"] as List<Map<String, Any>>?
                ?: throw InvalidConfigException("missing parameter 'ruleset.rules'")

            rules = rulesData.map{
                val factor = (it["factor"] as Number?
                    ?: throw InvalidConfigException("missing parameter 'ruleset.rules.factor'")).toDouble()
                val stepThreshold = (it["stepThreshold"] as Number? ?: 1).toInt()
                val minPodCount = (it["minPodCount"] as Number? ?: defaultMinPodCount).toInt()
                val maxPodCount = (it["maxPodCount"] as Number? ?: defaultMaxPodCount).toInt()

                return@map LinearScaleRule(factor, stepThreshold, minPodCount, maxPodCount)
            }

            if(rules.size != 1)
                throw InvalidConfigException("'ruleset.rules' must contain exactly one element if type = '${LinearScaleRuleset.TYPE}'")

            return LinearScaleRuleset(type, rules[0])
        }catch (e: ClassCastException){
            throw InvalidConfigException("parameter-value had wrong type", e)
        }catch (e: NullPointerException){
            throw InvalidConfigException("missing value", e)
        }
    }

    private fun loadLogarithmicScaleRuleset(data: Map<String, Any>): LogarithmicScaleRuleset {
        try{
            val type = data["type"] as String?
                ?: throw InvalidConfigException("missing parameter 'ruleset.type'")
            if(type != LogarithmicScaleRuleset.TYPE)
                throw InvalidConfigException("wrong type for LogarithmicScaleRuleset")

            val rules: List<LogarithmicScaleRule>

            val rulesData = data["rules"] as List<Map<String, Any>>?
                ?: throw InvalidConfigException("missing parameter 'ruleset.rules'")

            rules = rulesData.map{
                val base = (it["base"] as Number?
                    ?: throw InvalidConfigException("missing parameter 'ruleset.rules.base'")).toDouble()
                val stepThreshold = (it["stepThreshold"] as Number? ?: 1).toInt()
                val minPodCount = (it["minPodCount"] as Number? ?: defaultMinPodCount).toInt()
                val maxPodCount = (it["maxPodCount"] as Number? ?: defaultMaxPodCount).toInt()

                return@map LogarithmicScaleRule(base, stepThreshold, minPodCount, maxPodCount)
            }

            if(rules.size != 1)
                throw InvalidConfigException("'ruleset.rules' must contain exactly one element if type = '${LogarithmicScaleRuleset.TYPE}'")

            return LogarithmicScaleRuleset(type, rules[0])
        }catch (e: ClassCastException){
            throw InvalidConfigException("parameter-value had wrong type", e)
        }catch (e: NullPointerException){
            throw InvalidConfigException("missing value", e)
        }
    }
}
