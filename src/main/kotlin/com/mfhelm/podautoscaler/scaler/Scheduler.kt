package com.mfhelm.podautoscaler.scaler

import com.mfhelm.podautoscaler.scaler.config.ConfigLoader
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import org.springframework.stereotype.Component
import java.util.concurrent.Executors

@Component
internal class Scheduler(
    private val beanFactory: BeanFactory,
    private val configLoader: ConfigLoader
) : SchedulingConfigurer{

    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        val executor = Executors.newScheduledThreadPool(1)
        taskRegistrar.setScheduler(executor)

        // schedule each scaler
        configLoader.configEntries.forEach{
            val scaler: Scaler = beanFactory.getBean("Scaler") as Scaler
            scaler.config = it
            taskRegistrar.addFixedRateTask(scaler, it.interval * 1000)
        }
    }
}