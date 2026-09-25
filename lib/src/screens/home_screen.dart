import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../services/vpn_service.dart';
import 'safe_page_screen.dart';
import 'rewards_screen.dart';
import 'settings_screen.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  final VpnService _vpnService = VpnService();
  bool _isVpnActive = false;
  VpnStatistics? _statistics;

  @override
  void initState() {
    super.initState();
    _initializeVpn();
  }

  Future<void> _initializeVpn() async {
    try {
      final isRunning = await _vpnService.isVpnRunning();
      final stats = await _vpnService.getStatistics();

      if (mounted) {
        setState(() {
          _isVpnActive = isRunning;
          _statistics = stats;
        });
      }

      // Listen to VPN status changes
      _vpnService.vpnStatusStream.listen((isActive) {
        if (mounted) {
          setState(() {
            _isVpnActive = isActive;
          });
        }
      });

      // Listen to blocked content
      _vpnService.blockedContentStream.listen((blockedContent) {
        _navigateToSafePage(blockedContent);
      });
    } catch (e) {
      debugPrint('Error initializing VPN: $e');
    }
  }

  Future<void> _toggleVpn() async {
    try {
      if (_isVpnActive) {
        await _vpnService.stopVpn();
      } else {
        // Check permission first
        final hasPermission = await _vpnService.isVpnPermissionGranted();
        if (!hasPermission) {
          final granted = await _vpnService.requestVpnPermission();
          if (!granted) {
            if (mounted) {
              _showError('VPN permission is required');
            }
            return;
          }
        }

        await _vpnService.startVpn();
      }

      // Refresh statistics
      final stats = await _vpnService.getStatistics();
      if (mounted) {
        setState(() {
          _statistics = stats;
        });
      }
    } catch (e) {
      if (mounted) {
        _showError('Failed to toggle VPN: $e');
      }
    }
  }

  void _navigateToSafePage(BlockedContent content) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => SafePageScreen(
          domain: content.domain,
          url: content.url,
        ),
      ),
    );
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Theme.of(context).colorScheme.error,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Eraser'),
        actions: [
          IconButton(
            icon: const Icon(Icons.settings),
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (context) => const SettingsScreen(),
                ),
              );
            },
          ),
        ],
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Day Counter (placeholder)
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(32.0),
                  child: Column(
                    children: [
                      Text(
                        '0',
                        style: theme.textTheme.displayLarge?.copyWith(
                          fontWeight: FontWeight.bold,
                          color: theme.colorScheme.primary,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        'Days Clean',
                        style: theme.textTheme.titleMedium,
                      ),
                    ],
                  ),
                ),
              ),

              const SizedBox(height: 24),

              // VPN Protection Card
              Card(
                color: _isVpnActive
                    ? theme.colorScheme.primaryContainer
                    : theme.colorScheme.surfaceContainerHighest,
                child: Padding(
                  padding: const EdgeInsets.all(24.0),
                  child: Column(
                    children: [
                      Icon(
                        _isVpnActive ? Icons.shield : Icons.shield_outlined,
                        size: 64,
                        color: _isVpnActive
                            ? theme.colorScheme.primary
                            : theme.colorScheme.onSurfaceVariant,
                      ),
                      const SizedBox(height: 16),
                      Text(
                        _isVpnActive
                            ? 'Protection Active'
                            : 'Protection Inactive',
                        style: theme.textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 24),
                      FilledButton(
                        onPressed: _toggleVpn,
                        child: Padding(
                          padding: const EdgeInsets.symmetric(vertical: 8.0),
                          child: Text(
                            _isVpnActive ? 'Stop Protection' : 'Start Protection',
                            style: const TextStyle(fontSize: 16),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),

              const SizedBox(height: 24),

              // Statistics Card
              if (_statistics != null)
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(24.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Statistics',
                          style: theme.textTheme.titleLarge,
                        ),
                        const SizedBox(height: 16),
                        _buildStatRow(
                          'Today',
                          _statistics!.todayBlocked.toString(),
                          theme,
                        ),
                        const Divider(height: 24),
                        _buildStatRow(
                          'This Week',
                          _statistics!.weekBlocked.toString(),
                          theme,
                        ),
                        const Divider(height: 24),
                        _buildStatRow(
                          'Total',
                          _statistics!.totalBlocked.toString(),
                          theme,
                        ),
                      ],
                    ),
                  ),
                ),

              const SizedBox(height: 24),

              // Quick Actions
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: () {
                        Navigator.of(context).push(
                          MaterialPageRoute(
                            builder: (context) => const RewardsScreen(),
                          ),
                        );
                      },
                      icon: const Icon(Icons.emoji_events),
                      label: const Text('Rewards'),
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: () {
                        // TODO: Navigate to reports screen
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text('Reports screen coming soon!'),
                          ),
                        );
                      },
                      icon: const Icon(Icons.analytics),
                      label: const Text('Reports'),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildStatRow(String label, String value, ThemeData theme) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: theme.textTheme.titleMedium,
        ),
        Text(
          value,
          style: theme.textTheme.titleLarge?.copyWith(
            fontWeight: FontWeight.bold,
            color: theme.colorScheme.primary,
          ),
        ),
      ],
    );
  }
}
