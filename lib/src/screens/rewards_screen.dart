import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class RewardsScreen extends ConsumerWidget {
  const RewardsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);

    // TODO: Replace with actual database data
    final achievements = _getDummyAchievements();
    final unlockedCount = achievements.where((a) => a.isUnlocked).length;
    final totalCount = achievements.length;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Achievements'),
        centerTitle: true,
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Progress header
              Container(
                margin: const EdgeInsets.all(24),
                padding: const EdgeInsets.all(24),
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    colors: [
                      theme.colorScheme.primaryContainer,
                      theme.colorScheme.secondaryContainer,
                    ],
                  ),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Column(
                  children: [
                    Text(
                      '$unlockedCount / $totalCount',
                      style: theme.textTheme.displayMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                        color: theme.colorScheme.primary,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Achievements Unlocked',
                      style: theme.textTheme.titleMedium,
                    ),
                    const SizedBox(height: 16),
                    LinearProgressIndicator(
                      value: unlockedCount / totalCount,
                      minHeight: 8,
                      borderRadius: BorderRadius.circular(4),
                    ),
                  ],
                ),
              ),

              // Achievements grid
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 24.0),
                child: GridView.builder(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 2,
                    crossAxisSpacing: 16,
                    mainAxisSpacing: 16,
                    childAspectRatio: 0.85,
                  ),
                  itemCount: achievements.length,
                  itemBuilder: (context, index) {
                    final achievement = achievements[index];
                    return _AchievementCard(achievement: achievement);
                  },
                ),
              ),

              const SizedBox(height: 24),
            ],
          ),
        ),
      ),
    );
  }

  List<_Achievement> _getDummyAchievements() {
    return [
      _Achievement(
        title: '1 Day Strong',
        description: 'Completed your first day clean',
        icon: '🌟',
        milestone: 1,
        isUnlocked: true,
      ),
      _Achievement(
        title: '3 Day Warrior',
        description: 'Three days of freedom',
        icon: '💪',
        milestone: 3,
        isUnlocked: false,
      ),
      _Achievement(
        title: '1 Week Champion',
        description: 'Seven days of victory',
        icon: '🏆',
        milestone: 7,
        isUnlocked: false,
      ),
      _Achievement(
        title: '2 Week Hero',
        description: 'Two weeks of strength',
        icon: '🦸',
        milestone: 14,
        isUnlocked: false,
      ),
      _Achievement(
        title: '1 Month Legend',
        description: 'Thirty days of transformation',
        icon: '👑',
        milestone: 30,
        isUnlocked: false,
      ),
      _Achievement(
        title: '60 Day Master',
        description: 'Two months of freedom',
        icon: '🎖️',
        milestone: 60,
        isUnlocked: false,
      ),
      _Achievement(
        title: '90 Day Victor',
        description: 'Three months of success',
        icon: '🥇',
        milestone: 90,
        isUnlocked: false,
      ),
      _Achievement(
        title: '6 Month Titan',
        description: 'Half a year clean',
        icon: '⭐',
        milestone: 180,
        isUnlocked: false,
      ),
      _Achievement(
        title: '1 Year Legend',
        description: 'One full year of freedom',
        icon: '🔥',
        milestone: 365,
        isUnlocked: false,
      ),
      _Achievement(
        title: 'First Intervention',
        description: 'Completed your first intervention task',
        icon: '🎯',
        milestone: null,
        isUnlocked: true,
      ),
      _Achievement(
        title: 'Streak Keeper',
        description: 'Maintained a 7-day streak',
        icon: '🔒',
        milestone: null,
        isUnlocked: false,
      ),
    ];
  }
}

class _AchievementCard extends StatelessWidget {
  final _Achievement achievement;

  const _AchievementCard({required this.achievement});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isLocked = !achievement.isUnlocked;

    return Card(
      elevation: isLocked ? 1 : 4,
      color: isLocked
          ? theme.colorScheme.surfaceContainerHighest
          : theme.colorScheme.primaryContainer,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
      ),
      child: InkWell(
        onTap: () => _showAchievementDetails(context),
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              // Icon
              Container(
                width: 64,
                height: 64,
                decoration: BoxDecoration(
                  color: isLocked
                      ? Colors.grey.withOpacity(0.3)
                      : theme.colorScheme.primary.withOpacity(0.2),
                  shape: BoxShape.circle,
                ),
                child: Center(
                  child: Text(
                    achievement.icon,
                    style: TextStyle(
                      fontSize: 32,
                      color: isLocked ? Colors.grey : null,
                    ),
                  ),
                ),
              ),

              const SizedBox(height: 12),

              // Title
              Text(
                achievement.title,
                style: theme.textTheme.titleSmall?.copyWith(
                  fontWeight: FontWeight.bold,
                  color: isLocked
                      ? theme.colorScheme.onSurfaceVariant
                      : theme.colorScheme.primary,
                ),
                textAlign: TextAlign.center,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),

              const SizedBox(height: 8),

              // Milestone or status
              if (achievement.milestone != null)
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 8,
                    vertical: 4,
                  ),
                  decoration: BoxDecoration(
                    color: isLocked
                        ? Colors.grey.withOpacity(0.2)
                        : theme.colorScheme.primary.withOpacity(0.2),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(
                    '${achievement.milestone} days',
                    style: theme.textTheme.labelSmall?.copyWith(
                      color: isLocked
                          ? theme.colorScheme.onSurfaceVariant
                          : theme.colorScheme.primary,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),

              if (isLocked) ...[
                const SizedBox(height: 8),
                Icon(
                  Icons.lock_outline,
                  size: 16,
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  void _showAchievementDetails(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(
          achievement.title,
          textAlign: TextAlign.center,
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              achievement.icon,
              style: const TextStyle(fontSize: 64),
            ),
            const SizedBox(height: 16),
            Text(
              achievement.description,
              textAlign: TextAlign.center,
            ),
            if (achievement.milestone != null) ...[
              const SizedBox(height: 16),
              Text(
                'Milestone: ${achievement.milestone} days',
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
              ),
            ],
            const SizedBox(height: 8),
            Text(
              achievement.isUnlocked ? '🎉 Unlocked!' : '🔒 Locked',
              style: TextStyle(
                color: achievement.isUnlocked ? Colors.green : Colors.grey,
                fontWeight: FontWeight.bold,
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Close'),
          ),
        ],
      ),
    );
  }
}

class _Achievement {
  final String title;
  final String description;
  final String icon;
  final int? milestone;
  final bool isUnlocked;

  _Achievement({
    required this.title,
    required this.description,
    required this.icon,
    this.milestone,
    required this.isUnlocked,
  });
}
