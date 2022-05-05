package com.mfhelm.podautoscaler.scaler.config

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.JacksonYAMLParseException
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.mfhelm.podautoscaler.scaler.config.ruleset.LimitRule
import com.mfhelm.podautoscaler.scaler.config.ruleset.LimitRuleset
import com.mfhelm.podautoscaler.scaler.config.ruleset.LinearScaleRule
import com.mfhelm.podautoscaler.scaler.config.ruleset.LinearScaleRuleset
import com.mfhelm.podautoscaler.scaler.config.ruleset.LogarithmicScaleRule
import com.mfhelm.podautoscaler.scaler.config.ruleset.LogarithmicScaleRuleset
import com.mfhelm.podautoscaler.scaler.config.ruleset.Ruleset
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

@Suppress("ProtectedMemberInFinalClass")
@Component
internal class ConfigLoader(
    @Value("\${config}") private val configString: String
) {

    final lateinit var configEntries: List<ScalerConfig>
        protected set // protected for tests

    private val deserializer = initDeserializer()

    @Suppress("UnusedPrivateMember")
    @PostConstruct
    private fun init() {
        configEntries = loadConfig(configString)
    }

    // protected for tests
    protected fun loadConfig(src: String): List<ScalerConfig> {
        if (src.isEmpty())
            return emptyList()

        return deserializer.createParser(src).readValueAs(Array<ScalerConfig>::class.java).asList()
    }

    private fun initDeserializer(): ObjectMapper {
        return ObjectMapper(YAMLFactory()).apply {
            enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
            registerModule(kotlinModule())
        }
    }
}

internal class RulesetDeserializer : StdDeserializer<Ruleset>(Ruleset::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Ruleset {
        val fields = p.codec.readTree<ObjectNode>(p)

        return when (val typeValue = fields["type"].textValue()) {
            LimitRuleset.TYPE -> readLimitRuleset(fields, p)
            LinearScaleRuleset.TYPE -> readLinearScaleRuleset(fields, p)
            LogarithmicScaleRuleset.TYPE -> readLogarithmicScaleRuleset(fields, p)
            else -> throw JacksonYAMLParseException(p, "unsupported ruleset-type: '$typeValue'", null)
        }
    }

    private fun readLimitRuleset(fields: ObjectNode, p: JsonParser): LimitRuleset {
        val rules = p.codec.treeToValue(fields["rules"], Array<LimitRule>::class.java)
        if (rules.isEmpty()) {
            throw JacksonYAMLParseException(
                p,
                "'ruleset.rules' must contain at least one rule of type = '${LimitRuleset.TYPE}'",
                null
            )
        }

        return LimitRuleset(rules.asList())
    }

    private fun readLinearScaleRuleset(fields: ObjectNode, p: JsonParser): LinearScaleRuleset {
        val rules = p.codec.treeToValue(fields["rules"], Array<LinearScaleRule>::class.java)
        if (rules.size != 1) {
            throw JacksonYAMLParseException(
                p,
                "'ruleset.rules' must contain exactly one element if type = '${LinearScaleRuleset.TYPE}'",
                null
            )
        }

        return LinearScaleRuleset(rules[0])
    }

    private fun readLogarithmicScaleRuleset(fields: ObjectNode, p: JsonParser): LogarithmicScaleRuleset {
        val rules = p.codec.treeToValue(fields["rules"], Array<LogarithmicScaleRule>::class.java)
        if (rules.size != 1) {
            throw JacksonYAMLParseException(
                p,
                "'ruleset.rules' must contain exactly one element if type = '${LinearScaleRuleset.TYPE}'",
                null
            )
        }

        return LogarithmicScaleRuleset(rules[0])
    }
}

internal class IntervalValueDeserializer : StdDeserializer<Long>(Long::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Long {
        val value = p.text
        if (value === null) {
            throw JacksonYAMLParseException(p, "value must be a string", null)
        }
        if (value.isEmpty()) {
            throw JacksonYAMLParseException(p, "value must not be a valid number (with unit)", null)
        }

        try {
            return parseTimeValue(value)
        } catch (e: NumberFormatException) {
            throw JacksonYAMLParseException(p, "value is not a valid number", e)
        } catch (e: IllegalArgumentException) {
            throw JacksonYAMLParseException(p, "value has invalid unit", e)
        }
    }

    /**
     * Parses values like 5h to seconds.
     * Supported units are: s (seconds, default), m (minutes), h (hours) and d (days)
     * @param str the input to parse
     * @throw InvalidConfigException
     */
    private fun parseTimeValue(str: String): Long {
        return when (val unit = str[str.length - 1]) {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                str.toLong()
            }
            's' -> { // for consistency
                str.substring(0, str.length - 1).toLong()
            }
            'm' -> {
                TimeUnit.MINUTES.toSeconds(str.substring(0, str.length - 1).toLong())
            }
            'h' -> {
                TimeUnit.HOURS.toSeconds(str.substring(0, str.length - 1).toLong())
            }
            'd' -> {
                TimeUnit.DAYS.toSeconds(str.substring(0, str.length - 1).toLong())
            }
            else -> throw IllegalArgumentException("invalid unit: $unit")
        }
    }
}

internal class CountValueDeserializer : StdDeserializer<Int>(Int::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Int {
        val value = p.text
        if (value === null) {
            throw JacksonYAMLParseException(p, "value must be a string", null)
        }
        if (value.isEmpty()) {
            throw JacksonYAMLParseException(p, "value must not be a valid number (with unit)", null)
        }

        try {
            return parseCountValue(value)
        } catch (e: NumberFormatException) {
            throw JacksonYAMLParseException(p, "value is not a valid number", e)
        } catch (e: IllegalArgumentException) {
            throw JacksonYAMLParseException(p, "value has invalid unit", e)
        }
    }

    /**
     * Parses values like 5m to number.
     * Supported units are: k (thousand), m (million)
     * @param str the input to parse
     * @throw InvalidConfigException
     */
    @Suppress("MagicNumber")
    private fun parseCountValue(str: String): Int {
        return when (val unit = str[str.length - 1]) {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                str.toInt()
            }
            'k' -> {
                (str.substring(0, str.length - 1).toDouble() * 1000).toInt()
            }
            'm' -> {
                (str.substring(0, str.length - 1).toDouble() * 1000000).toInt()
            }
            else -> throw IllegalArgumentException("invalid unit: $unit")
        }
    }
}
