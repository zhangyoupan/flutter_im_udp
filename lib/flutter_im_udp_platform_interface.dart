import 'package:flutter/services.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'flutter_im_udp_method_channel.dart';
import 'method.dart';
import 'model/imsdk_result_model.dart';

abstract class FlutterImUdpPlatform extends PlatformInterface {
  /// Constructs a FlutterImUdpPlatform.
  FlutterImUdpPlatform() : super(token: _token);

  static final Object _token = Object();

  static FlutterImUdpPlatform _instance = MethodChannelFlutterImUdp();

  /// The default instance of [FlutterImUdpPlatform] to use.
  ///
  /// Defaults to [MethodChannelFlutterImUdp].
  static FlutterImUdpPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FlutterImUdpPlatform] when
  /// they register themselves.
  static set instance(FlutterImUdpPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  Future<MobileIMSDKResult?> initMobileIMSDK({
    required String serverIP,
    required int serverPort,
    bool debug = false,
  }) {
    throw UnimplementedError('initIM() has not been implemented.');
  }
  Future<MobileIMSDKResult?> login({
    required String loginUserId,
    required String loginToken,
  })  {
    throw UnimplementedError('login() has not been implemented.');
  }
  Future<MobileIMSDKResult?> sendMessage({
    required String dataContent,
    required String toUserId,
  })   {
    throw UnimplementedError('sendMessage() has not been implemented.');
  }
  void setMethodCallHandler(ValueChanged<FlutterMobileIMSDKMethod>? handler){
    throw UnimplementedError('setMethodCallHandler() has not been implemented.');
  }
}
