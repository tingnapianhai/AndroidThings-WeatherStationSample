/*
 * Copyright 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.androidthings.weatherstation

import android.os.Build

import com.google.android.things.pio.PeripheralManagerService

import java.lang.reflect.Array
import java.util.ArrayList
import java.util.Arrays

class BoardDefaults {

    private fun sdfds() {
        val mm = ArrayList<String>()
        val a = intArrayOf(1, 2, 3)
        Arrays.sort(a)
    }

    companion object {
        private val DEVICE_EDISON_ARDUINO = "edison_arduino"
        private val DEVICE_EDISON = "edison"
        private val DEVICE_JOULE = "joule"
        private val DEVICE_RPI3 = "rpi3"
        private val DEVICE_IMX6UL_PICO = "imx6ul_pico"
        private val DEVICE_IMX6UL_VVDN = "imx6ul_iopb"
        private val DEVICE_IMX7D_PICO = "imx7d_pico"
        var sBoardVariant: String = ""

        val buttonGpioPin: String
            get() {
                when (boardVariant) {
                    DEVICE_EDISON_ARDUINO -> return "IO12"
                    DEVICE_EDISON -> return "GP44"
                    DEVICE_JOULE -> return "J7_71"
                    DEVICE_RPI3 -> return "BCM21"
                    DEVICE_IMX6UL_PICO -> return "GPIO2_IO03"
                    DEVICE_IMX6UL_VVDN -> return "GPIO3_IO01"
                    DEVICE_IMX7D_PICO -> return "GPIO_174"
                    else -> throw IllegalArgumentException("Unknown device: " + Build.DEVICE)
                }
            }

        val ledGpioPin: String
            get() {
                when (boardVariant) {
                    DEVICE_EDISON_ARDUINO -> return "IO13"
                    DEVICE_EDISON -> return "GP45"
                    DEVICE_JOULE -> return "J6_25"
                    DEVICE_RPI3 -> return "BCM6"
                    DEVICE_IMX6UL_PICO -> return "GPIO4_IO22"
                    DEVICE_IMX6UL_VVDN -> return "GPIO3_IO06"
                    DEVICE_IMX7D_PICO -> return "GPIO_34"
                    else -> throw IllegalArgumentException("Unknown device: " + Build.DEVICE)
                }
            }

        val i2cBus: String
            get() {
                when (boardVariant) {
                    DEVICE_EDISON_ARDUINO -> return "I2C6"
                    DEVICE_EDISON -> return "I2C1"
                    DEVICE_JOULE -> return "I2C0"
                    DEVICE_RPI3 -> return "I2C1"
                    DEVICE_IMX6UL_PICO -> return "I2C2"
                    DEVICE_IMX6UL_VVDN -> return "I2C4"
                    DEVICE_IMX7D_PICO -> return "I2C1"
                    else -> throw IllegalArgumentException("Unknown device: " + Build.DEVICE)
                }
            }

        val spiBus: String
            get() {
                when (boardVariant) {
                    DEVICE_EDISON_ARDUINO -> return "SPI1"
                    DEVICE_EDISON -> return "SPI2"
                    DEVICE_JOULE -> return "SPI0.0"
                    DEVICE_RPI3 -> return "SPI0.0"
                    DEVICE_IMX6UL_PICO -> return "SPI3.0"
                    DEVICE_IMX6UL_VVDN -> return "SPI1.0"
                    DEVICE_IMX7D_PICO -> return "SPI3.1"
                    else -> throw IllegalArgumentException("Unknown device: " + Build.DEVICE)
                }
            }

        val speakerPwmPin: String
            get() {
                when (boardVariant) {
                    DEVICE_EDISON_ARDUINO -> return "IO3"
                    DEVICE_EDISON -> return "GP13"
                    DEVICE_JOULE -> return "PWM_0"
                    DEVICE_RPI3 -> return "PWM1"
                    DEVICE_IMX6UL_PICO -> return "PWM8"
                    DEVICE_IMX6UL_VVDN -> return "PWM3"
                    DEVICE_IMX7D_PICO -> return "PWM2"
                    else -> throw IllegalArgumentException("Unknown device: " + Build.DEVICE)
                }
            }

        private// For the edison check the pin prefix
                // to always return Edison Breakout pin name when applicable.
        val boardVariant: String
            get() {
                if (sBoardVariant.isNotEmpty()) {
                    return sBoardVariant
                }
                sBoardVariant = Build.DEVICE
                if (sBoardVariant == DEVICE_EDISON) {
                    val pioService = PeripheralManagerService()
                    val gpioList = pioService.gpioList
                    if (gpioList.size != 0) {
                        val pin = gpioList[0]
                        if (pin.startsWith("IO")) {
                            sBoardVariant = DEVICE_EDISON_ARDUINO
                        }
                    }
                }
                return sBoardVariant
            }
    }
}
