import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_im_udp/flutter_im_udp.dart';
import 'package:flutter_im_udp/flutter_im_udp_platform_interface.dart';
import 'package:flutter_im_udp/flutter_im_udp_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockFlutterImUdpPlatform
    with MockPlatformInterfaceMixin
    implements FlutterImUdpPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final FlutterImUdpPlatform initialPlatform = FlutterImUdpPlatform.instance;

  test('$MethodChannelFlutterImUdp is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelFlutterImUdp>());
  });

  test('getPlatformVersion', () async {
    FlutterImUdp flutterImUdpPlugin = FlutterImUdp();
    MockFlutterImUdpPlatform fakePlatform = MockFlutterImUdpPlatform();
    FlutterImUdpPlatform.instance = fakePlatform;

    expect(await flutterImUdpPlugin.getPlatformVersion(), '42');
  });
}
