package com.mfhelm.podautoscaler.test

import com.mfhelm.podautoscaler.scaler.Scaler
import com.mfhelm.podautoscaler.scaler.ScalerFactory
import com.mfhelm.podautoscaler.scaler.Scheduler
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import org.springframework.test.context.TestPropertySource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@TestPropertySource(properties = ["config = "])
class SchedulerTest {

    @Autowired
    private lateinit var scalerFactory: ScalerFactory

    @Autowired
    private lateinit var configLoader: ConfigLoaderBinding

    private val taskRegistrar = mockk<ScheduledTaskRegistrar>(relaxed = true)

    private fun readConfig(name: String): String {
        val res = ConfigLoaderTest::class.java.getResource("$name.yaml")
        if (res === null) {
            throw AssertionError("test-resource $name not found")
        }
        return res.readText()
    }

    @Test
    fun schedulerInit() {
        configLoader.setConfig(configLoader.loadConfig(readConfig("SchedulerConfig")))
        val scheduler = Scheduler(scalerFactory, configLoader)

        // just check labels and intervals
        val expected = listOf(
            Pair("A", 10000L),
            Pair("A-2", 60000L)
        )

        val actual: ArrayList<Pair<String, Long>> = ArrayList()
        val taskArg = slot<Runnable>()
        val intervalArg = slot<Long>()
        every { taskRegistrar.addFixedRateTask(capture(taskArg), capture(intervalArg)) } answers {
            assertTrue(taskArg.isCaptured)
            assertTrue(intervalArg.isCaptured)

            val scalerArg = taskArg.captured
            if (scalerArg !is Scaler) {
                assertTrue(false, "task-arg is not a Scaler")
            }

            val scaler = scalerArg as Scaler
            actual.add(Pair(scaler.config.label, intervalArg.captured))
        }

        scheduler.configureTasks(taskRegistrar)

        assertEquals(expected, actual)
    }
}
