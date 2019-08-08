package com.kgxl.webrtc

import android.content.Context
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer

/**
 * Create by kgxl on 2019-08-08
 */
class RtcSdkManager : RtcManager {
companion object{
    val instance:RtcSdkManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { RtcSdkManager() }
}
    override fun init(
        context: Context,
        eglContext: EglBase.Context,
        localSurface: SurfaceViewRenderer,
        remoteSurface: SurfaceViewRenderer
    ) {
        WebRtcClient.instance.init(context, eglContext, localSurface, remoteSurface)
    }

    override fun connect(address: String) {
        WebRtcClient.instance.connect(address)
    }

    override fun startCall() {
        WebRtcClient.instance.startCall()
    }

    override fun switchCamera() {
        WebRtcClient.instance.switchCamera()
    }

    override fun switchAudioMute() {
        WebRtcClient.instance.switchAudioMute()
    }

    override fun switchAudioMode() {
        WebRtcClient.instance.switchAudioMode()
    }

    override fun getIsCall(): Boolean {
        return WebRtcClient.instance.getIsCall()
    }

    override fun onPause() {
        WebRtcClient.instance.onPause()
    }

    override fun onResume() {
        WebRtcClient.instance.onResume()
    }

    override fun onDestroy() {
        WebRtcClient.instance.onDestroy()
    }

    override fun onStop() {
        WebRtcClient.instance.onStop()
    }
}