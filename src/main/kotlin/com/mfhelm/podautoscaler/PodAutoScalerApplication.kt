package com.mfhelm.podautoscaler

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class PodAutoScalerApplication {}

fun main(args: Array<String>){
    runApplication<PodAutoScalerApplication>(*args)
}
