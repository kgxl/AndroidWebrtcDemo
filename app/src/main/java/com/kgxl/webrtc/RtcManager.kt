package com.kgxl.webrtc

import android.content.Context
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer

/**
 * Create by kgxl on 2019-08-08
 */
interface RtcManager {
    fun init(
        context: Context,
        eglContext: EglBase.Context,
        localSurface: SurfaceViewRenderer,
        remoteSurface: SurfaceViewRenderer
    )

    fun connect(address: String)

    fun startCall()
    fun switchCamera()
    fun switchAudioMute()
    fun switchAudioMode()
    fun getIsCall(): Boolean
    fun onPause()
    fun onResume()
    fun onDestroy()
    fun onStop()
}