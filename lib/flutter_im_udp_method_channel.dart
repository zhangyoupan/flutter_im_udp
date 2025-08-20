import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'flutter_im_udp_platform_interface.dart';
import 'method.dart';
import 'model/imsdk_result_model.dart';

/// An implementation of [FlutterImUdpPlatform] that uses method channels.
class MethodChannelFlutterImUdp extends FlutterImUdpPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('flutter_im_udp');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }


  /// 初始化 IM
  @override
  Future<MobileIMSDKResult?> initMobileIMSDK({
    required String serverIP,
    required int serverPort,
    bool debug = false,
  }) async {
    final result = await methodChannel.invokeMethod('initMobileIMSDK', {
      'serverIP': serverIP,
      'serverPort': serverPort,
      'debug': debug,
    });
    return MobileIMSDKResult.fromJson(result);
  }

  /// 登录
  @override
  Future<MobileIMSDKResult?> login({
    required String loginUserId,
    required String loginToken,
  }) async {
    final result = await methodChannel.invokeMethod('login', {
      'loginUserId': loginUserId,
      'loginToken': loginToken,
    });
    return MobileIMSDKResult.fromJson(result);
  }

  /// 发送消息
  @override
  Future<MobileIMSDKResult?> sendMessage({
    required String dataContent,
    required String toUserId,
  }) async {
    final result =
    await methodChannel.invokeMethod('sendMessage', {
      'dataContent': dataContent,
      'toUserId': toUserId,
    });
    return MobileIMSDKResult.fromJson(result);
  }

  /// 监听原生回调事件（登录/掉线/收到消息等）
  @override
  Future<void> setMethodCallHandler(ValueChanged<FlutterMobileIMSDKMethod>? handler) async {
    methodChannel.setMethodCallHandler((call) async {
      handler?.call(FlutterMobileIMSDKMethod.fromMethodCall(call));
    });
  }
}
