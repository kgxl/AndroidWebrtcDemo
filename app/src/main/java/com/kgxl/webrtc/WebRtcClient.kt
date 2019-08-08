package com.kgxl.webrtc

import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.PeerConnection
import java.util.*
import kotlin.collections.ArrayList
import org.webrtc.MediaConstraints
import kotlin.collections.HashMap
import org.webrtc.MediaStream
import android.R.attr.orientation
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT
import android.media.AudioManager
import android.view.Surface
import android.view.Surface.ROTATION_0
import android.view.Surface.ROTATION_270
import android.view.Surface.ROTATION_180
import android.view.Surface.ROTATION_90
import androidx.core.view.ViewCompat.getRotation
import androidx.core.content.ContextCompat.getSystemService
import android.view.WindowManager
import androidx.core.content.getSystemService


/***
 * Create by kgxl on 2019/7/23
 */
class WebRtcClient private constructor() {
    private val VIDEO_TRACK_ID = "ARDAMSv0"
    private val AUDIO_TRACK_ID = "ARDAMSa0"
    private var uuid = UUID.randomUUID().toString()
    private val TAG = "WebRtcClient"
    private val VIDEO_RESOLUTION_WIDTH = 1280
    private val VIDEO_RESOLUTION_HEIGHT = 720
    private val VIDEO_FPS = 30
    private var serviceGenerateId = false
    private var isCall = false
    private val iceServers = LinkedList<PeerConnection.IceServer>()
    private val peers: HashMap<String, RealPeer> = hashMapOf()
    private var defaultMirror = true
    private var defaultMute = false

    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { WebRtcClient() }
    }

    private var context: Context? = null
    private var peerFactory: PeerConnectionFactory? = null
    private var mPeerConnection: PeerConnection? = null
    private var mVideoTrack: VideoTrack? = null
    private var mAudioTrack: AudioTrack? = null
    private var remoteSurface: SurfaceViewRenderer? = null
    private var localSurface: SurfaceViewRenderer? = null
    private var videoCapture: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoSource: VideoSource? = null
    private var mediaConstraints: MediaConstraints? = null
    private var localMS: MediaStream? = null
    private val commandMap: HashMap<String, Command> = hashMapOf()
    private val mPeerConnectionObserver: PeerConnection.Observer = object : PeerConnection.Observer {
        override fun onIceCandidate(candidate: IceCandidate) {
            try {
                val payload = JSONObject()
                payload.put("label", candidate.sdpMLineIndex)
                payload.put("id", candidate.sdpMid)
                payload.put("candidate", candidate.sdp)
                SocketManager.instance.sendMessage(uuid, "candidate", payload)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        override fun onDataChannel(p0: DataChannel?) {
        }

        override fun onIceConnectionReceivingChange(p0: Boolean) {
        }

        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
            if (state == PeerConnection.IceConnectionState.FAILED) {
                isCall = false
            }
            if (state == PeerConnection.IceConnectionState.CONNECTED) {
                isCall = true
            }
            if (state == PeerConnection.IceConnectionState.DISCONNECTED) {
                peers.keys.forEach {
                    if (!it.equals(uuid)) {
                        peers.remove(it)
                    }
                }
                isCall = false
            }
            Log.e("zjy", "onIceConnectionChange-->$state")
        }

        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
        }

        override fun onAddStream(p0: MediaStream?) {
            Log.e("zjy", "onAddStream")
        }

        override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
        }

        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
            mPeerConnection?.removeIceCandidates(candidates)
        }

        override fun onRemoveStream(p0: MediaStream?) {
            remoteSurface?.release()
            mPeerConnection?.close()
        }

        override fun onRenegotiationNeeded() {
        }

        override fun onAddTrack(rtpReceiver: RtpReceiver?, p1: Array<out MediaStream>?) {
            Log.e("zjy", "onAddVideoTrack")
            isCall = true
            val track = rtpReceiver?.track()
            if (track is VideoTrack) {
                val remoteVideoTrack = track
                remoteVideoTrack.setEnabled(true)
                val videoSink = ProxyVideoSink()
                remoteSurface?.let { videoSink.setTarget(it) }
                remoteVideoTrack.addSink(videoSink)
            }
        }
    }

    fun onPause() {
        videoCapture?.stopCapture()
    }

    fun onDestroy() {
        localSurface?.release()
        remoteSurface?.release()
        videoCapture?.dispose()
        surfaceTextureHelper?.dispose()
        PeerConnectionFactory.stopInternalTracingCapture()
        PeerConnectionFactory.shutdownInternalTracer()
    }

    fun onStop() {
        videoCapture?.stopCapture()
    }

    fun onResume() {
        videoCapture?.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, VIDEO_FPS)
    }

    fun connect(address: String) {
        commandMap["init"] = CreateOfferCommand()
        commandMap["offer"] = CreateAnswerCommand()
        commandMap["answer"] = SetRemoteSDPCommand()
        commandMap["candidate"] = AddIceCandidateCommand()
        SocketManager.instance.setOnConnectStateListener(object : SocketManager.onConnectStateListener {
            override fun connectSuccess() {
                Log.e("zjy", "connectSuccess")
            }

            override fun connectFailure(errorMsg: String) {
                Log.e("zjy", "connectFailure-->$errorMsg")

            }

            override fun disconnect() {
                Log.e("zjy", "disconnect")
                serviceGenerateId = false
            }

            override fun connecting() {
                Log.e("zjy", "connecting")
            }
        })
        SocketManager.instance.setOnReceiveMsgListener(object : SocketManager.onRtcListener {
            override fun userJoin(signal: String) {
                uuid = signal
                serviceGenerateId = true
                Log.e("zjy", "userJoin-->$signal")
            }

            override fun userLeave(signal: String) {
                Log.e("zjy", "userLeave-->$signal")
            }

            override fun result(msg: String) {
                Log.e("zjy", "result-->$msg")
            }

            override fun receiveMsg(msg: String) {
                val data = JSONObject(msg)
                try {
                    val from = data.optString("from")
                    val type = data.optString("type")
                    var payload: JSONObject? = null
                    if (type != "init") {
                        payload = data.getJSONObject("payload")
                    }
                    if (!peers.containsKey(from)) {
                        val pc = createPeerConnect()
                        pc.addStream(localMS)
                        val peer = RealPeer(from, pc)
                        peers[from] = peer
                    }
                    commandMap[type]?.execute(from, payload)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            }
        })
        SocketManager.instance.connectSocket(address)
    }

    /**
     * 初始化本地webrtc,并且初始化本地视频
     */
    fun init(
        context: Context,
        eglContext: EglBase.Context,
        localSurface: SurfaceViewRenderer,
        remoteSurface: SurfaceViewRenderer
    ) {
        this.remoteSurface = remoteSurface
        this.localSurface = localSurface
        this.context = context
        iceServers.add(PeerConnection.IceServer.builder("stun:23.21.150.121").createIceServer())
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        val sink = ProxyVideoSink()
        sink.setTarget(localSurface)
        //初始化peerConnectFactory
        peerFactory = createPeerFactory(context, eglContext)
        if (BuildConfig.DEBUG) {
            Logging.enableLogToDebugOutput(Logging.Severity.LS_VERBOSE)
        }

        videoSource = peerFactory?.createVideoSource(false)
        surfaceTextureHelper = SurfaceTextureHelper.create("videoCapture", eglContext)
//        surfaceTextureHelper?.setFrameRotation(getRotationDegree())
        videoCapture = createVideoCapture()
        videoCapture?.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
        //视频
        mVideoTrack = peerFactory?.createVideoTrack(VIDEO_TRACK_ID, videoSource)
        mVideoTrack?.setEnabled(true)
        mVideoTrack?.addSink(sink)

        //音频
        val audioSource = peerFactory?.createAudioSource(createAudioConstraints())
        mAudioTrack = peerFactory?.createAudioTrack(AUDIO_TRACK_ID, audioSource)
        mAudioTrack?.setEnabled(true)

        mediaConstraints = MediaConstraints()
        mediaConstraints?.apply {
            this.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            this.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            this.optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        }
        initLocalStream()
    }

    private fun initLocalStream() {
        localMS = peerFactory?.createLocalMediaStream("ARDAMS")
        localMS?.addTrack(mVideoTrack)
        localMS?.addTrack(mAudioTrack)
    }

    private fun createAudioConstraints(): MediaConstraints {
        val audioConstraints = MediaConstraints()
        //回声消除
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
        //自动增益
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
        //高音过滤
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
        //噪音处理
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        return audioConstraints
    }

    fun startCall() {
        if (!serviceGenerateId || TextUtils.isEmpty(uuid)) {
            Toast.makeText(context, "连接服务器失败", Toast.LENGTH_SHORT).show()
            return
        }
        if (serviceGenerateId && !getIsCall()) {
            SocketManager.instance.sendMessage(uuid, "init", null)
        }
    }

    private fun createPeerConnect(): PeerConnection {
        val configuration = PeerConnection.RTCConfiguration(iceServers)
        val connection =
            peerFactory?.createPeerConnection(configuration, mPeerConnectionObserver)
        connection!!.addTrack(mVideoTrack)
        connection!!.addTrack(mAudioTrack)
        Log.i("createPeerConnect", "Create PeerConnection ...${connection.toString()}")
        return connection
    }

    private fun createPeerFactory(context: Context, eglContext: EglBase.Context): PeerConnectionFactory {
        val encoderFactory: VideoEncoderFactory
        val decoderFactory: VideoDecoderFactory

        encoderFactory = DefaultVideoEncoderFactory(
            eglContext, false, true
        )
        decoderFactory = DefaultVideoDecoderFactory(eglContext)
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
        val builder = PeerConnectionFactory.builder().setVideoDecoderFactory(decoderFactory)
            .setVideoEncoderFactory(encoderFactory)
        builder.setOptions(null)
        return builder.createPeerConnectionFactory()
    }

    private fun createVideoCapture(): VideoCapturer? {
        return if (Camera2Enumerator.isSupported(context)) {
            createCameraCapture(Camera2Enumerator(context))
        } else {
            createCameraCapture(Camera1Enumerator(true))
        }
    }

    private fun createCameraCapture(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames

        // First, try to find front facing camera
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val videoCapturer = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }

        // Front facing camera not found, try something else
        for (deviceName in deviceNames) {
            if (enumerator.isBackFacing(deviceName)) {
                val videoCapturer = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        return null
    }


    class ProxyVideoSink : VideoSink {
        private var mTarget: VideoSink? = null
        @Synchronized
        override fun onFrame(frame: VideoFrame) {
            if (mTarget == null) {
                Log.d("webRtcClient", "Dropping frame in proxy because target is null.")
                return
            }
            mTarget!!.onFrame(frame)
        }

        @Synchronized
        internal fun setTarget(target: VideoSink) {
            this.mTarget = target
        }
    }

    private interface Command {
        @Throws(JSONException::class)
        fun execute(peerId: String, payload: JSONObject?)
    }

    private inner class CreateOfferCommand : Command {

        @Throws(JSONException::class)
        override fun execute(peerId: String, payload: JSONObject?) {
            Log.d(TAG, "CreateOfferCommand")
            val peer = peers[peerId]
            peer?.pc?.createOffer(peer, mediaConstraints)
        }
    }

    private inner class CreateAnswerCommand : Command {
        @Throws(JSONException::class)
        override fun execute(peerId: String, payload: JSONObject?) {
            Log.d(TAG, "CreateAnswerCommand")
            val peer = peers[peerId]
            val sdp = SessionDescription(
                SessionDescription.Type.fromCanonicalForm(payload?.getString("type")),
                payload?.getString("sdp")
            )
            peer?.pc?.setRemoteDescription(peer, sdp)
            peer?.pc?.createAnswer(peer, mediaConstraints)
        }
    }

    private inner class SetRemoteSDPCommand : Command {
        @Throws(JSONException::class)
        override fun execute(peerId: String, payload: JSONObject?) {
            Log.d(TAG, "SetRemoteSDPCommand")
            val peer = peers[peerId]
            val sdp = SessionDescription(
                SessionDescription.Type.fromCanonicalForm(payload?.getString("type")),
                payload?.getString("sdp")
            )
            peer?.pc?.setRemoteDescription(peer, sdp)
        }
    }

    private inner class AddIceCandidateCommand : Command {
        @Throws(JSONException::class)
        override fun execute(peerId: String, payload: JSONObject?) {
            Log.d(TAG, "AddIceCandidateCommand")
            val pc = peers[peerId]?.pc
            if (pc?.getRemoteDescription() != null) {
                val candidate = IceCandidate(
                    payload?.getString("id"),
                    payload?.getInt("label") ?: 0,
                    payload?.getString("candidate")
                )
                pc?.addIceCandidate(candidate)
            }
        }
    }

    fun switchCamera() {
        defaultMirror = !defaultMirror
        localSurface?.setMirror(defaultMirror)
        (videoCapture as CameraVideoCapturer).switchCamera(null)
    }

    fun getIsCall(): Boolean {
        return isCall
    }

    fun switchAudioMute() {
        defaultMute = !defaultMute
        mAudioTrack?.setEnabled(!defaultMute)
    }

    fun switchAudioMode() {
        val am = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        println("before audio mode--->" + am.isWiredHeadsetOn + "---" + am.mode + "----" + am.isSpeakerphoneOn)
        if (!am.isWiredHeadsetOn) {
            am.isSpeakerphoneOn = !am.isSpeakerphoneOn
            am.mode = if (am.mode == AudioManager.MODE_NORMAL) AudioManager.MODE_IN_COMMUNICATION else AudioManager.MODE_NORMAL
        }
        println("after audio mode--->" + am.isWiredHeadsetOn + "---" + am.mode + "----" + am.isSpeakerphoneOn)
    }
}