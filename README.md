# AndroidWebrtcDemo
Android webrtc，一对一视频。

- 主要功能
    - 一对一视频
    - 麦克风静音
    - 切换前后摄像头
    - 大小视频切换
    - 声音听筒和扬声器切换

#### install server
- 安装node.js
- cd /server目录
- npm install 安装一些三方包
- npm start启动服务


　　其中使用[Socket.io](https://github.com/socketio/socket.io)做连接。
　　默认端口为3000，在Android的```string.xml```中需要设置成自己启动服务的机器ip。
　　这个server要想部署到外网服务器,把server目录传到服务器，然后**打开3000端口**，执行跟上面install server一样的操作就好了。

#### 部署外网stun与turn
　　闲着没事搞了搞部署外网stun和turn，写了个踩坑记录：[部署stun与turn](https://blog.csdn.net/baidu_32207443/article/details/99620428)



#### 感谢
[ProjectRTC](https://github.com/pchab/ProjectRTC)