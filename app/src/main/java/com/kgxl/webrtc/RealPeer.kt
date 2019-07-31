package com.kgxl.webrtc

import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*

/***
 * Create by kgxl on 2019/7/23
 */
class RealPeer : PeerConnection.Observer, SdpObserver {
    private var id: String = ""
    private var peerConnection: PeerConnection? = null
    override fun onIceCandidate(p0: IceCandidate?) {
    }

    override fun onDataChannel(p0: DataChannel?) {
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
    }

    override fun onAddStream(p0: MediaStream?) {
    }

    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
    }

    override fun onRemoveStream(p0: MediaStream?) {
    }

    override fun onRenegotiationNeeded() {
    }

    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
    }

    override fun onSetFailure(p0: String?) {
    }

    override fun onSetSuccess() {
    }

    override fun onCreateSuccess(sdp: SessionDescription) {
        Log.e("zjy", "sdp-->${sdp.type.canonicalForm()}")
        try {
            val payload = JSONObject()
            payload.put("type", sdp.type.canonicalForm())
            payload.put("sdp", sdp.description)
            SocketManager.instance.sendMessage(id, sdp.type.canonicalForm(), payload)
            peerConnection?.setLocalDescription(this, sdp)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun onCreateFailure(p0: String?) {
    }

}