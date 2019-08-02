# AndroidWebrtcDemo
Android webrtc，一对一视频。

#### install server
- 安装node.js
- cd /server目录
- npm install 安装一些三方包
- npm start启动服务


其中使用[Socket.io](https://github.com/socketio/socket.io)做连接
默认端口为3000，在Android的```string.xml```中需要设置成自己启动服务的机器ip。


#### 感谢
[ProjectRTC](https://github.com/pchab/ProjectRTC)