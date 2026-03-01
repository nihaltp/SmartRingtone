import 'package:flutter/services.dart';

class CallStateService {
  static const MethodChannel _channel = MethodChannel('call_state_channel');

  void initialize({
    required Function() onAccepted,
    required Function() onRejected,
  }) {
    _channel.setMethodCallHandler((call) async {
      if (call.method == "callAccepted") {
        onAccepted();
      } else if (call.method == "callRejected") {
        onRejected();
      }
    });
  }

  Future<void> startListening() async {
    try {
      await _channel.invokeMethod('startListening');
    } catch (e) {
      // TODO: Better error handling
      print("Failed to start listening: $e");
    }
  }
}
