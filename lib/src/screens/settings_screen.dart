import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:url_launcher/url_launcher.dart';

class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Settings'),
        centerTitle: true,
      ),
      body: ListView(
        children: [
          const SizedBox(height: 16),

          // VPN Settings Section
          _SectionHeader(title: 'VPN & Blocking'),
          _SettingsTile(
            icon: Icons.shield_outlined,
            title: 'VPN Protection',
            subtitle: 'System-wide content blocking',
            trailing: Switch(
              value: false, // TODO: Connect to VPN service
              onChanged: (value) {
                // TODO: Toggle VPN
              },
            ),
          ),
          _SettingsTile(
            icon: Icons.list_alt,
            title: 'Blocklist Management',
            subtitle: '156,911 domains blocked',
            onTap: () {
              // TODO: Navigate to blocklist screen
            },
          ),
          _SettingsTile(
            icon: Icons.history,
            title: 'Blocked History',
            subtitle: 'View blocked attempts',
            onTap: () {
              // TODO: Navigate to history screen
            },
          ),

          const Divider(height: 32),

          // Journey Settings Section
          _SectionHeader(title: 'Journey'),
          _SettingsTile(
            icon: Icons.restart_alt,
            title: 'Reset Journey',
            subtitle: 'Start over from day 0',
            onTap: () => _showResetDialog(context),
          ),
          _SettingsTile(
            icon: Icons.check_circle_outline,
            title: 'Daily Check-In',
            subtitle: 'Mood and reflection tracking',
            onTap: () {
              // TODO: Navigate to check-in screen
            },
          ),

          const Divider(height: 32),

          // Notifications Section
          _SectionHeader(title: 'Notifications'),
          _SettingsTile(
            icon: Icons.notifications_outlined,
            title: 'Enable Notifications',
            subtitle: 'Reminders and achievements',
            trailing: Switch(
              value: true, // TODO: Connect to prefs
              onChanged: (value) {
                // TODO: Toggle notifications
              },
            ),
          ),
          _SettingsTile(
            icon: Icons.access_time,
            title: 'Daily Reminder',
            subtitle: 'Set check-in reminder time',
            onTap: () {
              // TODO: Show time picker
            },
          ),

          const Divider(height: 32),

          // Appearance Section
          _SectionHeader(title: 'Appearance'),
          _SettingsTile(
            icon: Icons.palette_outlined,
            title: 'Theme',
            subtitle: 'Light, Dark, or System',
            onTap: () => _showThemeDialog(context),
          ),

          const Divider(height: 32),

          // Data & Privacy Section
          _SectionHeader(title: 'Data & Privacy'),
          _SettingsTile(
            icon: Icons.lock_outline,
            title: 'Privacy Policy',
            subtitle: '100% local, no tracking',
            onTap: () => _showPrivacyInfo(context),
          ),
          _SettingsTile(
            icon: Icons.download_outlined,
            title: 'Export Data',
            subtitle: 'Backup your progress',
            onTap: () {
              // TODO: Implement data export
            },
          ),
          _SettingsTile(
            icon: Icons.delete_outline,
            title: 'Clear All Data',
            subtitle: 'Cannot be undone',
            onTap: () => _showClearDataDialog(context),
          ),

          const Divider(height: 32),

          // About Section
          _SectionHeader(title: 'About'),
          _SettingsTile(
            icon: Icons.info_outline,
            title: 'About Eraser',
            subtitle: 'Version 1.0.0',
            onTap: () => _showAboutDialog(context),
          ),
          _SettingsTile(
            icon: Icons.code,
            title: 'Open Source',
            subtitle: 'View on GitHub',
            onTap: () => _launchURL('https://github.com/yourusername/eraser'),
          ),
          _SettingsTile(
            icon: Icons.help_outline,
            title: 'Get Help',
            subtitle: 'Support and resources',
            onTap: () {
              // TODO: Navigate to help screen
            },
          ),

          const SizedBox(height: 32),
        ],
      ),
    );
  }

  void _showResetDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Reset Journey?'),
        content: const Text(
          'This will reset your streak and progress to day 0. '
          'Your achievements and blocked history will be preserved. '
          'This action cannot be undone.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () {
              // TODO: Implement reset
              Navigator.pop(context);
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Journey reset')),
              );
            },
            style: FilledButton.styleFrom(
              backgroundColor: Colors.red,
            ),
            child: const Text('Reset'),
          ),
        ],
      ),
    );
  }

  void _showThemeDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => SimpleDialog(
        title: const Text('Choose Theme'),
        children: [
          SimpleDialogOption(
            onPressed: () {
              // TODO: Set light theme
              Navigator.pop(context);
            },
            child: const Padding(
              padding: EdgeInsets.symmetric(vertical: 8.0),
              child: Text('Light'),
            ),
          ),
          SimpleDialogOption(
            onPressed: () {
              // TODO: Set dark theme
              Navigator.pop(context);
            },
            child: const Padding(
              padding: EdgeInsets.symmetric(vertical: 8.0),
              child: Text('Dark'),
            ),
          ),
          SimpleDialogOption(
            onPressed: () {
              // TODO: Set system theme
              Navigator.pop(context);
            },
            child: const Padding(
              padding: EdgeInsets.symmetric(vertical: 8.0),
              child: Text('System Default'),
            ),
          ),
        ],
      ),
    );
  }

  void _showPrivacyInfo(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Privacy First'),
        content: const SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                'Eraser is designed with your privacy in mind:',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              SizedBox(height: 16),
              Text('✅ 100% local - no data leaves your device'),
              SizedBox(height: 8),
              Text('✅ No accounts required'),
              SizedBox(height: 8),
              Text('✅ No tracking or analytics'),
              SizedBox(height: 8),
              Text('✅ No cloud sync'),
              SizedBox(height: 8),
              Text('✅ Open source code'),
              SizedBox(height: 16),
              Text(
                'All your data is stored only on your device and is never transmitted anywhere.',
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Got it'),
          ),
        ],
      ),
    );
  }

  void _showClearDataDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Clear All Data?'),
        content: const Text(
          'This will permanently delete all your data including:\n\n'
          '• Journey progress and streaks\n'
          '• Achievement unlocks\n'
          '• Blocked history\n'
          '• All settings\n\n'
          'This action cannot be undone.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () {
              // TODO: Implement data clear
              Navigator.pop(context);
            },
            style: FilledButton.styleFrom(
              backgroundColor: Colors.red,
            ),
            child: const Text('Delete All'),
          ),
        ],
      ),
    );
  }

  void _showAboutDialog(BuildContext context) {
    showAboutDialog(
      context: context,
      applicationName: 'Eraser',
      applicationVersion: '1.0.0',
      applicationIcon: Container(
        width: 48,
        height: 48,
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.primary,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Center(
          child: Icon(
            Icons.shield,
            color: Theme.of(context).colorScheme.onPrimary,
            size: 32,
          ),
        ),
      ),
      children: [
        const Text(
          'A compassionate, gamified recovery support app that blocks adult content.',
        ),
        const SizedBox(height: 16),
        const Text(
          'Built with Flutter and designed for privacy.',
        ),
      ],
    );
  }

  Future<void> _launchURL(String url) async {
    final uri = Uri.parse(url);
    if (await canLaunchUrl(uri)) {
      await launchUrl(uri);
    }
  }
}

class _SectionHeader extends StatelessWidget {
  final String title;

  const _SectionHeader({required this.title});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
      child: Text(
        title.toUpperCase(),
        style: Theme.of(context).textTheme.labelLarge?.copyWith(
              color: Theme.of(context).colorScheme.primary,
              fontWeight: FontWeight.bold,
            ),
      ),
    );
  }
}

class _SettingsTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback? onTap;
  final Widget? trailing;

  const _SettingsTile({
    required this.icon,
    required this.title,
    required this.subtitle,
    this.onTap,
    this.trailing,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon),
      title: Text(title),
      subtitle: Text(subtitle),
      trailing: trailing ??
          (onTap != null
              ? const Icon(Icons.chevron_right)
              : null),
      onTap: onTap,
    );
  }
}
