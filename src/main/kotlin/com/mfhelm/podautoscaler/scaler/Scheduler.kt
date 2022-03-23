package com.mfhelm.podautoscaler.scaler

import com.mfhelm.podautoscaler.scaler.config.ConfigLoader
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Component
internal class Scheduler(
    private val scalerFactory: ScalerFactory,
    private val configLoader: ConfigLoader
) : SchedulingConfigurer {

    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        val executor = Executors.newScheduledThreadPool(1)
        taskRegistrar.setScheduler(executor)

        // schedule each scaler
        configLoader.configEntries.forEach {
            val scaler = scalerFactory.newScaler(it)
            taskRegistrar.addFixedRateTask(scaler, TimeUnit.SECONDS.toMillis(it.interval))
        }
    }
}
