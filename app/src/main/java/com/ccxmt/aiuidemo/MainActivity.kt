package com.ccxmt.aiuidemo

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.alibaba.fastjson.JSONObject
import com.ccxmt.aiuidemo.databinding.ActivityMainBinding
import com.iflytek.aiui.*


class MainActivity : AppCompatActivity(), AIUIListener {
    val TAG = "MainActivity"
    lateinit var agent: AIUIAgent
    var currentState: Int = AIUIConstant.STATE_IDLE
    var isRecording: Boolean = false


    lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        createAgent()
        initUIElement()
        initPermissions()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initUIElement() {

        binding.button.setOnTouchListener { v, event ->

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isRecording = true
                    binding.voice.visibility = View.VISIBLE
                    var params = "data_type=audio,sample_rate=16000"
                    //流式识别
                    params += ",dwa=wpgs"
                    doSendMessage(
                        AIUIMessage(AIUIConstant.CMD_START_RECORD, 0, 0, params, null)
                    )

                }
                MotionEvent.ACTION_UP -> {
                    binding.voice.visibility = View.INVISIBLE
                    if (isRecording) {
                        doSendMessage(
                            AIUIMessage(
                                AIUIConstant.CMD_STOP_RECORD,
                                0,
                                0,
                                "data_type=audio,sample_rate=16000",
                                null
                            )
                        )
                        isRecording = false
                    }
                }
            }

            true
        }
    }

    /**
     * 想AIUI发送消息
     */
    private fun doSendMessage(aiuiMessage: AIUIMessage) {
        if (currentState != AIUIConstant.STATE_WORKING) {
            agent.sendMessage(AIUIMessage(AIUIConstant.CMD_WAKEUP, 0, 0, "", null))
        }
        agent.sendMessage(aiuiMessage)

    }

    private fun createAgent() {
        agent = AIUIAgent.createAgent(this, mApp.getAIUIParamas(), this)

    }

    private fun initPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.INTERNET
            ), 1
        )

    }

    /**
     * AIUI 回调监听
     */
    override fun onEvent(event: AIUIEvent?) {
        when (event?.eventType) {
            //唤醒事件
            AIUIConstant.EVENT_WAKEUP -> {
                Log.i(TAG, "唤醒事件")

            }
            //结果事件（包含听写，语义，离线语法结果）
            AIUIConstant.EVENT_RESULT -> {

                try {
                    Log.i(TAG, "结果事件（包含听写，语义，离线语法结果）")
                    val result = JSONObject.parseObject(event.info)
                    Log.i(TAG, "onEvent: result is ${event.info}")

                    val info: JSONObject = JSONObject.parseObject(event.info)
                    val data: JSONObject = info.getJSONArray("data").getJSONObject(0)
                    val params: JSONObject = data.getJSONObject("params")
                    val content: JSONObject = data.getJSONArray("content").getJSONObject(0)

                    if (content.containsKey("cnt_id")) {
                        val cnt_id = content.getString("cnt_id")
                        val cntJson =
                            JSONObject.parseObject(
                                event.data.getByteArray(cnt_id)?.let { String(it) })
                        val sub = params.getString("sub")
                        if ("nlp".equals(sub)) {
                            val resultString = cntJson.getString("intent")
                            Log.i(TAG, "onEvent: result is $resultString")
                        }
                    }
                } catch (e: Exception) {

                }


            }
            //休眠事件
            AIUIConstant.EVENT_SLEEP -> {
                Log.i(TAG, "休眠事件")
            }
            // 状态事件
            AIUIConstant.EVENT_STATE -> {

                val state = event.arg1
                currentState = event.arg1
                when (state) {
                    // 闲置状态，AIUI未开启
                    AIUIConstant.STATE_IDLE -> {
                        Log.i(TAG, "状态事件:闲置状态，AIUI未开启")


                    }
                    // AIUI已就绪，等待唤醒
                    AIUIConstant.STATE_READY -> {
                        binding.voice.visibility = View.INVISIBLE
                        Log.i(TAG, "onEvent: 状态事件:AIUI已就绪，等待唤醒")

                    }
                    // AIUI工作中，可进行交互
                    AIUIConstant.STATE_WORKING -> {
                        binding.voice.visibility = View.VISIBLE
                        Log.i(TAG, "状态事件: AIUI工作中，可进行交互")
                        val params =
                            "sample_rate=16000,data_type=audio,pers_param={\"uid\":\"\"},tag=audio-tag"
                        val startRecord =
                            AIUIMessage(AIUIConstant.CMD_START_RECORD, 0, 0, params, null)

                        agent.sendMessage(startRecord)
                    }
                }
            }
            //错误事件
            AIUIConstant.EVENT_ERROR -> {
                val errorCode = event.arg1
                if (errorCode == 20006) {
                    isRecording = false
                }
                Log.i(TAG, "错误事件: $errorCode")
                Log.i(TAG, "onEvent: ${event.info}")
            }
            AIUIConstant.EVENT_START_RECORD -> {
                Log.i(TAG, "onEvent: 开始录音")
            }
            AIUIConstant.EVENT_STOP_RECORD -> {
                Log.i(TAG, "onEvent: 结束录音")
            }

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.i(TAG, "onRequestPermissionsResult: 权限获取")
    }
}