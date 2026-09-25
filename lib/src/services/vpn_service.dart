import 'dart:async';
import 'package:flutter/services.dart';

/// VPN Service that communicates with native platform code
/// Provides cross-platform VPN control for content blocking
class VpnService {
  static const MethodChannel _methodChannel =
      MethodChannel('com.eraser.recovery/vpn');

  static const EventChannel _statusChannel =
      EventChannel('com.eraser.recovery/vpn_status');

  static const EventChannel _blockedContentChannel =
      EventChannel('com.eraser.recovery/blocked_content');

  Stream<bool>? _vpnStatusStream;
  Stream<BlockedContent>? _blockedContentStream;

  /// Get stream of VPN status updates
  Stream<bool> get vpnStatusStream {
    _vpnStatusStream ??= _statusChannel
        .receiveBroadcastStream()
        .map((dynamic event) => event as bool);
    return _vpnStatusStream!;
  }

  /// Get stream of blocked content notifications
  Stream<BlockedContent> get blockedContentStream {
    _blockedContentStream ??= _blockedContentChannel
        .receiveBroadcastStream()
        .map((dynamic event) {
      final map = Map<String, dynamic>.from(event as Map);
      return BlockedContent(
        domain: map['domain'] as String,
        url: map['url'] as String,
        timestamp: DateTime.fromMillisecondsSinceEpoch(
          map['timestamp'] as int,
        ),
      );
    });
    return _blockedContentStream!;
  }

  /// Check if VPN permission is granted
  Future<bool> isVpnPermissionGranted() async {
    try {
      final result =
          await _methodChannel.invokeMethod<bool>('isVpnPermissionGranted');
      return result ?? false;
    } on PlatformException catch (e) {
      throw VpnException('Failed to check VPN permission: ${e.message}');
    }
  }

  /// Request VPN permission from user
  Future<bool> requestVpnPermission() async {
    try {
      final result =
          await _methodChannel.invokeMethod<bool>('requestVpnPermission');
      return result ?? false;
    } on PlatformException catch (e) {
      throw VpnException('Failed to request VPN permission: ${e.message}');
    }
  }

  /// Start VPN service
  Future<void> startVpn() async {
    try {
      await _methodChannel.invokeMethod<void>('startVpn');
    } on PlatformException catch (e) {
      throw VpnException('Failed to start VPN: ${e.message}');
    }
  }

  /// Stop VPN service
  Future<void> stopVpn() async {
    try {
      await _methodChannel.invokeMethod<void>('stopVpn');
    } on PlatformException catch (e) {
      throw VpnException('Failed to stop VPN: ${e.message}');
    }
  }

  /// Check if VPN is currently running
  Future<bool> isVpnRunning() async {
    try {
      final result = await _methodChannel.invokeMethod<bool>('isVpnRunning');
      return result ?? false;
    } on PlatformException catch (e) {
      throw VpnException('Failed to check VPN status: ${e.message}');
    }
  }

  /// Get VPN statistics
  Future<VpnStatistics> getStatistics() async {
    try {
      final result = await _methodChannel.invokeMethod<Map>('getVpnStatistics');
      final map = Map<String, dynamic>.from(result!);

      return VpnStatistics(
        totalBlocked: map['totalBlocked'] as int? ?? 0,
        todayBlocked: map['todayBlocked'] as int? ?? 0,
        weekBlocked: map['weekBlocked'] as int? ?? 0,
      );
    } on PlatformException catch (e) {
      throw VpnException('Failed to get statistics: ${e.message}');
    }
  }

  /// Update blocklist
  Future<void> updateBlocklist() async {
    try {
      await _methodChannel.invokeMethod<void>('updateBlocklist');
    } on PlatformException catch (e) {
      throw VpnException('Failed to update blocklist: ${e.message}');
    }
  }
}

/// Blocked content information
class BlockedContent {
  final String domain;
  final String url;
  final DateTime timestamp;

  const BlockedContent({
    required this.domain,
    required this.url,
    required this.timestamp,
  });

  @override
  String toString() =>
      'BlockedContent(domain: $domain, url: $url, timestamp: $timestamp)';

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;

    return other is BlockedContent &&
        other.domain == domain &&
        other.url == url &&
        other.timestamp == timestamp;
  }

  @override
  int get hashCode => domain.hashCode ^ url.hashCode ^ timestamp.hashCode;
}

/// VPN statistics
class VpnStatistics {
  final int totalBlocked;
  final int todayBlocked;
  final int weekBlocked;

  const VpnStatistics({
    required this.totalBlocked,
    required this.todayBlocked,
    required this.weekBlocked,
  });

  @override
  String toString() =>
      'VpnStatistics(total: $totalBlocked, today: $todayBlocked, week: $weekBlocked)';

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;

    return other is VpnStatistics &&
        other.totalBlocked == totalBlocked &&
        other.todayBlocked == todayBlocked &&
        other.weekBlocked == weekBlocked;
  }

  @override
  int get hashCode =>
      totalBlocked.hashCode ^ todayBlocked.hashCode ^ weekBlocked.hashCode;
}

/// VPN related exceptions
class VpnException implements Exception {
  final String message;

  const VpnException(this.message);

  @override
  String toString() => 'VpnException: $message';
}
