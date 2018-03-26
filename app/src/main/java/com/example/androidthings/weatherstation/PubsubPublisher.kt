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

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Base64
import android.util.Log

import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.pubsub.Pubsub
import com.google.api.services.pubsub.PubsubScopes
import com.google.api.services.pubsub.model.PublishRequest
import com.google.api.services.pubsub.model.PubsubMessage

import org.json.JSONException
import org.json.JSONObject

import java.io.IOException
import java.io.InputStream
import java.util.Collections
import java.util.concurrent.TimeUnit

// TODO(proppy): move to a service class.
internal class PubsubPublisher @Throws(IOException::class)
constructor(private val mContext: Context, private val mAppname: String, project: String, topic: String,
            credentialResourceId: Int) {
    private var mTopic: String? = null

    private var mPubsub: Pubsub? = null
    private var mHttpTransport: HttpTransport? = null

    private var mHandler: Handler? = null
    private var mHandlerThread: HandlerThread? = null

    private var mLastTemperature = java.lang.Float.NaN
    private var mLastPressure = java.lang.Float.NaN

    private val mPublishRunnable = object : Runnable {
        override fun run() {
            val connectivityManager = mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetworkInfo
            if (activeNetwork == null || !activeNetwork.isConnectedOrConnecting) {
                Log.e(TAG, "no active network")
                return
            }

            try {
                val messagePayload = createMessagePayload(mLastTemperature, mLastPressure)
                if (!messagePayload.has("data")) {
                    Log.d(TAG, "no sensor measurement to publish")
                    return
                }
                Log.d(TAG, "publishing message: " + messagePayload)
                val m = PubsubMessage()
                m.data = Base64.encodeToString(messagePayload.toString().toByteArray(),
                        Base64.NO_WRAP)
                val request = PublishRequest()
                request.messages = listOf<PubsubMessage>(m)
                mPubsub!!.projects().topics().publish(mTopic, request).execute()
            } catch (e: JSONException) {
                Log.e(TAG, "Error publishing message", e)
            } catch (e: IOException) {
                Log.e(TAG, "Error publishing message", e)
            } finally {
                mHandler!!.postDelayed(this, PUBLISH_INTERVAL_MS)
            }
        }

        @Throws(JSONException::class)
        private fun createMessagePayload(temperature: Float, pressure: Float): JSONObject {
            val sensorData = JSONObject()
            if (!java.lang.Float.isNaN(temperature)) {
                sensorData.put("temperature", temperature.toString())
            }
            if (!java.lang.Float.isNaN(pressure)) {
                sensorData.put("pressure", pressure.toString())
            }
            val messagePayload = JSONObject()
            messagePayload.put("deviceId", Build.DEVICE)
            messagePayload.put("channel", "pubsub")
            messagePayload.put("timestamp", System.currentTimeMillis())
            if (sensorData.has("temperature") || sensorData.has("pressure")) {
                messagePayload.put("data", sensorData)
            }
            return messagePayload
        }
    }

    val temperatureListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            mLastTemperature = event.values[0]
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    val pressureListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            mLastPressure = event.values[0]
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    init {
        mTopic = "projects/$project/topics/$topic"

        mHandlerThread = HandlerThread("pubsubPublisherThread")
        mHandlerThread?.start()
        mHandler = Handler(mHandlerThread!!.looper)

        val jsonCredentials = mContext.resources.openRawResource(credentialResourceId)
        val credentials: GoogleCredential
        try {
            credentials = GoogleCredential.fromStream(jsonCredentials).createScoped(
                    setOf<String>(PubsubScopes.PUBSUB))
        } finally {
            try {
                jsonCredentials.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing input stream", e)
            }

        }
        mHandler!!.post {
            mHttpTransport = AndroidHttp.newCompatibleTransport()
            val jsonFactory = JacksonFactory.getDefaultInstance()
            mPubsub = Pubsub.Builder(mHttpTransport!!, jsonFactory, credentials)
                    .setApplicationName(mAppname).build()
        }
    }

    fun start() {
        mHandler?.post(mPublishRunnable)
    }

    fun stop() {
        mHandler?.removeCallbacks(mPublishRunnable)
    }

    fun close() {
        mHandler?.removeCallbacks(mPublishRunnable)
        mHandler?.post {
            try {
                mHttpTransport!!.shutdown()
            } catch (e: IOException) {
                Log.d(TAG, "error destroying http transport")
            } finally {
                mHttpTransport = null
                mPubsub = null
            }
        }
        mHandlerThread!!.quitSafely()
    }

    companion object {
        private val TAG = PubsubPublisher::class.java.simpleName

        private val PUBLISH_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1)
    }
}