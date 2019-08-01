package com.kgxl.webrtc

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

class MainActivity : AppCompatActivity() {
    lateinit var gl_big: SurfaceViewRenderer
    lateinit var gl_small: SurfaceViewRenderer
    private var eglBase: EglBase? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        gl_big = findViewById<SurfaceViewRenderer>(R.id.gl_big)
        gl_small = findViewById<SurfaceViewRenderer>(R.id.gl_small)
        findViewById<Button>(R.id.btn_call).setOnClickListener {
            WebRtcClient.instance.startCall()
        }
        findViewById<Button>(R.id.btn_change).setOnClickListener {
            WebRtcClient.instance.switchCamera()
        }
        eglBase = EglBase.create()
        gl_small.init(eglBase?.eglBaseContext, null)
        gl_small.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        gl_small.setMirror(true)
        gl_small.setEnableHardwareScaler(false)
        gl_small.setZOrderMediaOverlay(true)

        gl_big.init(eglBase?.eglBaseContext, null)
        gl_big.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        gl_big.setMirror(true)
        gl_big.setEnableHardwareScaler(true)
        gl_big.setZOrderMediaOverlay(true)

        checkNeedPermission()
        WebRtcClient.instance.connect("http://${getString(R.string.ip_address)}")

    }

    private fun checkNeedPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val checkSelfPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            if (checkSelfPermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO), 100)
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
            WebRtcClient.instance.init(this, it, gl_small, gl_big)
        }
    }

    override fun onResume() {
        super.onResume()
        WebRtcClient.instance.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        WebRtcClient.instance.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        WebRtcClient.instance.onPause()
    }
}
