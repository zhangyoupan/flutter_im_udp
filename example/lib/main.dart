import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_im_udp/flutter_im_udp.dart';
import 'package:flutter_im_udp/method.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  final _flutterImUdpPlugin = FlutterImUdp();
  StreamController<FlutterMobileIMSDKMethod> streamController =
  StreamController<FlutterMobileIMSDKMethod>.broadcast();
  @override
  void initState() {
    super.initState();
    initPlatformState();
    initIM();
  }

  Future<void> initIM() async {

// 初始化
    var mobileIMSDKResult = await _flutterImUdpPlugin.initMobileIMSDK(
      serverIP: "",
      serverPort: 7901,
      isDebug: true,
    );

// 登录
    var mobileIMSDKResult2 = await _flutterImUdpPlugin.login(
      loginUserId: "",
      token: "",
    );

// 发送消息
//     await _flutterImUdpPlugin.sendMessage(
//       content: "测试消息",
//       toUserId: "user456",
//     );

// 设置回调
    _flutterImUdpPlugin.setMethodCallHandler((call) async {
      streamController.sink.add(call);
    });
   var _streamSubscription = streamController.stream.listen((event) {
      try {
        MobileIMSDKMethodType type = event.type;

        switch (type) {
          case MobileIMSDKMethodType.loginSuccess:
            if (event is MobileIMSDKLoginSuccess) {
              print('IMSDK 登录成功${event.info?.userId}');

            }
            break;
          case MobileIMSDKMethodType.loginFail:
            if (event is MobileIMSDKLoginFail) {
              // errorCode 服务端反馈的登录结果：0 表示登陆成功，1028则表示token校验不通过（要做其他设备登录的操作）
              print('IMSDK IM服务器登录/连接失败,code=${event.errorCode}');

            }
            break;
          case MobileIMSDKMethodType.linkClose:
            if (event is MobileIMSDKLinkClose) {
              print('IMSDK 与IM服务器的连接已断开, 自动登陆/重连将启动!,code=${event.errorCode}');
            }
            break;
          case MobileIMSDKMethodType.kickout:
            if (event is MobileIMSDKKickout) {
              print(
                  'IMSDK 与IM服务器的连接已断开, code=${event.info!.errorCode},msg=${event.info!.errorMsg}');

            }
            break;
          case MobileIMSDKMethodType.onRecieveMessage:
            if (event is MobileIMSDKRecieveMessage) {
              if (event.info?.dataContent != null) {

              }
            }
            break;
          case MobileIMSDKMethodType.onErrorResponse:
            if (event is MobileIMSDKErrorResponse) {
              if (event.info?.isUnlogin == true) {
                print('服务端会话已失效，自动登陆/重连将启动! ,code=${event.info?.errorCode}');
              } else {
                print(
                    '服务端会话已失效，自动登陆/重连将启动! ,Server反馈错误码：code=${event.info?.errorCode},errorMsg=${event.info?.errorMsg}');
              }
            }
            break;
          case MobileIMSDKMethodType.qosMessagesLost:
            if (event is MobileIMSDKMessagesLost) {
              print(
                  'IMSDK [消息未成功送达]共${event.protocalList?.length}条!(网络状况不佳或对方id不存在)');
            }
            break;
          case MobileIMSDKMethodType.qosMessagesBeReceived:
            if (event is MobileIMSDKMessagesBeReceived) {
              print('IMSDK 收到应答${event.fingerPrint}');
            }
            break;
          case MobileIMSDKMethodType.autoReLoginDaemonObserver:
            if (event is MobileIMSDKDaemonOberber) {
              int status = event.status == 1 || event.status == 2 ? 1 : 0;
              print('IMSDK 线程动态：>>>>>$status');
            }
            break;
          case MobileIMSDKMethodType.keepAliveDaemonObserver:
            if (event is MobileIMSDKDaemonOberber) {
              int status = event.status == 1 || event.status == 2 ? 1 : 0;
              print('IMSDK 重连状态>>>>>$status');
            }
            break;
          case MobileIMSDKMethodType.qoS4SendDaemonObserver:
            if (event is MobileIMSDKDaemonOberber) {
              int status = event.status == 1 || event.status == 2 ? 1 : 0;
              print('IMSDK Qos(发)>>>>>$status');
            }
            break;
          case MobileIMSDKMethodType.qoS4ReciveDaemonObserver:
            if (event is MobileIMSDKDaemonOberber) {
              int status = event.status == 1 || event.status == 2 ? 1 : 0;
              print('IMSDK Qos(收)>>>>>$status');
            }
            break;
        }
      } catch (e) {
        print('IMSDK streamController>>>$e');
      }
    });
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    // We also handle the message potentially returning null.
    try {
      platformVersion =
          await _flutterImUdpPlugin.getPlatformVersion() ?? 'Unknown platform version';
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }
@override
  void dispose() {
    super.dispose();
    streamController.close();
  }
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Text('Running on: $_platformVersion\n'),
        ),
      ),
    );
  }
}
