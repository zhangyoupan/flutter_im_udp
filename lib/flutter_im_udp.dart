
import 'package:flutter/services.dart';

import 'flutter_im_udp_platform_interface.dart';
import 'method.dart';
import 'model/imsdk_result_model.dart';

class FlutterImUdp {
  Future<String?> getPlatformVersion() {
    return FlutterImUdpPlatform.instance.getPlatformVersion();
  }

  /// 初始化 IM
  Future<MobileIMSDKResult?> initMobileIMSDK({
    required String serverIP,
    required int serverPort,
    bool isDebug = false,
  }) {
    return FlutterImUdpPlatform.instance
        .initMobileIMSDK(serverIP: serverIP, serverPort: serverPort, debug: isDebug);
  }

  /// 登录
  Future<MobileIMSDKResult?> login({
    required String loginUserId,
    required String token,
  }) {
    return FlutterImUdpPlatform.instance
        .login(loginUserId: loginUserId, loginToken: token);
  }

  /// 发送消息
  Future<MobileIMSDKResult?> sendMessage({
    required String dataContent,
    required String toUserId,
  }) {
    return FlutterImUdpPlatform.instance
        .sendMessage(dataContent: dataContent, toUserId: toUserId);
  }

  /// 设置原生回调事件
  void setMethodCallHandler(ValueChanged<FlutterMobileIMSDKMethod>? handler) {
    FlutterImUdpPlatform.instance.setMethodCallHandler(handler);

  }
}
