import 'package:flutter/services.dart';

class CallStateService {
  static const MethodChannel _channel = MethodChannel('call_state_channel');

  void initialize({
    required void Function(String number) onIncoming,
    void Function()? onAccepted,
    void Function()? onRejected,
  }) {
    _channel.setMethodCallHandler((call) async {
      if (call.method == 'incomingCall') {
        final number = (call.arguments as String?) ?? 'Unknown';
        onIncoming(number);
      } else if (call.method == 'callAccepted') {
        onAccepted?.call();
      } else if (call.method == 'callRejected') {
        onRejected?.call();
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

  Future<bool> canWriteSettings() async {
    try {
      final allowed = await _channel.invokeMethod<bool>('canWriteSettings');
      return allowed ?? false;
    } catch (_) {
      return false;
    }
  }

  Future<void> openWriteSettings() async {
    try {
      await _channel.invokeMethod('openWriteSettings');
    } catch (_) {
      // Intentionally ignored; caller handles user messaging/state.
    }
  }
}
