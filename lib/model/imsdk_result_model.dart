class MobileIMSDKResult {
  late bool result;
  dynamic value;

  MobileIMSDKResult(this.result, this.value);

  MobileIMSDKResult.fromJson(Map<dynamic, dynamic>? json) {
    if (json == null) {
      result = false;
      return;
    }
    result = json['result'] == true;
    value = json['value'];
  }
}