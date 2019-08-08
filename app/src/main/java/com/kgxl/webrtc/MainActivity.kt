package com.kgxl.webrtc

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

class MainActivity : AppCompatActivity() {
    private lateinit var mGlRemote: SurfaceViewRenderer
    private lateinit var mGlLocal: SurfaceViewRenderer
    private var eglBase: EglBase? = null
    private val rtcManager by lazy { RtcSdkManager.instance }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mGlRemote = findViewById(R.id.gl_big)
        mGlLocal = findViewById(R.id.gl_small)
        findViewById<Button>(R.id.btn_mute).setOnClickListener {
            rtcManager.switchAudioMute()
        }
        findViewById<Button>(R.id.btn_call).setOnClickListener {
            rtcManager.startCall()
        }
        findViewById<Button>(R.id.btn_change).setOnClickListener {
            rtcManager.switchCamera()
        }
        findViewById<Button>(R.id.btn_mode).setOnClickListener {
            rtcManager.switchAudioMode()
        }
        eglBase = EglBase.create()
        mGlLocal.init(eglBase?.eglBaseContext, null)
        mGlLocal.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        //镜像
        mGlLocal.setMirror(true)
        mGlLocal.setEnableHardwareScaler(false)
        mGlLocal.setZOrderMediaOverlay(true)

        mGlRemote.init(eglBase?.eglBaseContext, null)
        mGlRemote.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        mGlRemote.setMirror(false)
        mGlRemote.setEnableHardwareScaler(true)
        mGlRemote.setZOrderMediaOverlay(true)

        mGlLocal.setOnClickListener {
            runAnimation(mGlLocal, mGlRemote)
        }

        mGlRemote.setOnClickListener {
            runAnimation(mGlLocal, mGlRemote)
        }
        checkNeedPermission()
        
        rtcManager.connect("http://${getString(R.string.ip_address)}")
    }

    fun runAnimation(local: SurfaceView, remote: SurfaceView) {
        val viewGroup = local.parent as ViewGroup
        println("before----${viewGroup.indexOfChild(local)}----${viewGroup.indexOfChild(remote)}")
        if (local.layoutParams.width == ViewGroup.LayoutParams.MATCH_PARENT) {
            remote.layoutParams =
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            val layoutParams = FrameLayout.LayoutParams(dp2px(this,150f), dp2px(this,290f))
            layoutParams.gravity = Gravity.BOTTOM or Gravity.RIGHT
            local.layoutParams = layoutParams
            viewGroup.removeView(local)
            viewGroup.removeView(remote)
            viewGroup.addView(remote, 0)
            viewGroup.addView(local, 1)
        } else {
            local.layoutParams =
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            val layoutParams = FrameLayout.LayoutParams(dp2px(this,150f), dp2px(this,290f))
            layoutParams.gravity = Gravity.BOTTOM or Gravity.RIGHT
            remote.layoutParams = layoutParams
            viewGroup.removeView(remote)
            viewGroup.removeView(local)
            viewGroup.addView(local, 0)
            viewGroup.addView(remote, 1)
        }
        println("after---${viewGroup.indexOfChild(local)}----${viewGroup.indexOfChild(remote)}")
    }

    private fun checkNeedPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val checkSelfPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            if (checkSelfPermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 100)
            } else {
                init()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            init()
        }
    }

    private fun init() {
        eglBase?.eglBaseContext?.let {
            rtcManager.init(this, it, mGlLocal, mGlRemote)
        }
    }

    override fun onResume() {
        super.onResume()
        rtcManager.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        rtcManager.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        rtcManager.onPause()
    }


    fun dp2px(context: Context, dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}
