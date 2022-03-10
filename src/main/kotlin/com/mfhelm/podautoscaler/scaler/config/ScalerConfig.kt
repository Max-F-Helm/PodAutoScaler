package com.mfhelm.podautoscaler.scaler.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.yaml.snakeyaml.Yaml
import javax.annotation.PostConstruct

internal data class ScalerConfig(
    internal val label: String,
    internal val queueVirtualHost: String,
    internal val queueName: String,
    internal val podNamespace: String,
    internal val pod: String,
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
                val podNamespace = it["podNamespace"] as String?
                    ?: throw InvalidConfigException("missing parameter 'podNamespace'")
                val pod = it["pod"] as String?
                    ?: throw InvalidConfigException("missing parameter 'pod'")
                val interval = (it["interval"] as Number?
                    ?: throw InvalidConfigException("missing parameter 'interval'")).toLong()

                val ruleset = it["ruleset"] as Map<String, Any>?
                    ?: throw InvalidConfigException("missing parameter 'ruleset'")
                val rulesType = ruleset["type"] as String?
                    ?: throw InvalidConfigException("missing parameter 'ruleset.type'")
                val rules: Ruleset = when(rulesType){
                    LimitRuleset.TYPE -> loadLimitRuleset(ruleset)
                    LinearScaleRuleset.TYPE -> loadLinearScaleRuleset(ruleset)
                    else -> throw InvalidConfigException("invalid value for 'ruleset.type'")
                }

                return@map ScalerConfig(label, queueVirtualHost, queueName, podNamespace, pod, interval, rules)
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
                throw InvalidConfigException("wring type for LimitRuleSet")

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
                throw InvalidConfigException("'ruleset.rules' must contain at least one rule if type = 'limit'")

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
                throw InvalidConfigException("wring type for LimitRuleSet")

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
                throw InvalidConfigException("'ruleset.rules' must contain exactly one element if type = 'linearScale'")

            return LinearScaleRuleset(type, rules[0])
        }catch (e: ClassCastException){
            throw InvalidConfigException("parameter-value had wrong type", e)
        }catch (e: NullPointerException){
            throw InvalidConfigException("missing value", e)
        }
    }
}
