import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'dart:async';

class SafePageScreen extends ConsumerStatefulWidget {
  final String domain;
  final String url;

  const SafePageScreen({
    super.key,
    required this.domain,
    required this.url,
  });

  @override
  ConsumerState<SafePageScreen> createState() => _SafePageScreenState();
}

class _SafePageScreenState extends ConsumerState<SafePageScreen>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _fadeAnimation;
  late Animation<Offset> _slideAnimation;

  // Flashcard data (will be replaced with database call)
  String flashcardMessage = 'Take a 10-minute walk outside';
  String flashcardCategory = 'Physical';
  int _secondsElapsed = 0;
  Timer? _timer;

  @override
  void initState() {
    super.initState();

    _controller = AnimationController(
      duration: const Duration(milliseconds: 800),
      vsync: this,
    );

    _fadeAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _controller, curve: Curves.easeIn),
    );

    _slideAnimation = Tween<Offset>(
      begin: const Offset(0, 0.3),
      end: Offset.zero,
    ).animate(CurvedAnimation(parent: _controller, curve: Curves.easeOut));

    _controller.forward();

    // Start timer to track how long user stays
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      setState(() {
        _secondsElapsed++;
      });
    });

    // TODO: Load random flashcard from database
    _loadFlashcard();
  }

  Future<void> _loadFlashcard() async {
    // TODO: Implement database call
    // final flashcard = await ref.read(databaseProvider).getRandomFlashcard();
    // setState(() {
    //   flashcardMessage = flashcard.message;
    //   flashcardCategory = flashcard.category;
    // });
  }

  @override
  void dispose() {
    _controller.dispose();
    _timer?.cancel();
    super.dispose();
  }

  void _completeTask() {
    // TODO: Record intervention completion
    // ref.read(databaseProvider).completeInterventionSession(sessionId, _secondsElapsed);

    Navigator.of(context).pop();

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: const Text('Great job! Keep up the good work! 🎉'),
        backgroundColor: Colors.green,
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
      ),
    );
  }

  void _skipTask() {
    // TODO: Record skip
    Navigator.of(context).pop();
  }

  Color _getCategoryColor(String category) {
    switch (category.toLowerCase()) {
      case 'physical':
        return Colors.orange;
      case 'mindfulness':
        return Colors.blue;
      case 'social':
        return Colors.green;
      case 'creative':
        return Colors.purple;
      case 'reflection':
        return Colors.teal;
      default:
        return Colors.grey;
    }
  }

  IconData _getCategoryIcon(String category) {
    switch (category.toLowerCase()) {
      case 'physical':
        return Icons.directions_run;
      case 'mindfulness':
        return Icons.self_improvement;
      case 'social':
        return Icons.people;
      case 'creative':
        return Icons.brush;
      case 'reflection':
        return Icons.psychology;
      default:
        return Icons.task_alt;
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Take a Moment'),
        centerTitle: true,
        automaticallyImplyLeading: false,
      ),
      body: FadeTransition(
        opacity: _fadeAnimation,
        child: SlideTransition(
          position: _slideAnimation,
          child: SafeArea(
            child: Padding(
              padding: const EdgeInsets.all(24.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // Blocked domain info
                  Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: theme.colorScheme.errorContainer,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Row(
                      children: [
                        Icon(
                          Icons.block,
                          color: theme.colorScheme.error,
                          size: 24,
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'Content Blocked',
                                style: theme.textTheme.titleMedium?.copyWith(
                                  fontWeight: FontWeight.bold,
                                  color: theme.colorScheme.onErrorContainer,
                                ),
                              ),
                              const SizedBox(height: 4),
                              Text(
                                widget.domain,
                                style: theme.textTheme.bodySmall?.copyWith(
                                  color: theme.colorScheme.onErrorContainer,
                                ),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),

                  const SizedBox(height: 32),

                  // Motivational message
                  Text(
                    'You\'re stronger than this urge',
                    style: theme.textTheme.headlineSmall?.copyWith(
                      fontWeight: FontWeight.bold,
                      color: theme.colorScheme.primary,
                    ),
                    textAlign: TextAlign.center,
                  ),

                  const SizedBox(height: 16),

                  Text(
                    'Take a moment to refocus your energy on something positive:',
                    style: theme.textTheme.bodyLarge,
                    textAlign: TextAlign.center,
                  ),

                  const Spacer(),

                  // Flashcard
                  Card(
                    elevation: 8,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Container(
                      padding: const EdgeInsets.all(32),
                      child: Column(
                        children: [
                          // Category badge
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 16,
                              vertical: 8,
                            ),
                            decoration: BoxDecoration(
                              color: _getCategoryColor(flashcardCategory)
                                  .withOpacity(0.2),
                              borderRadius: BorderRadius.circular(20),
                            ),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Icon(
                                  _getCategoryIcon(flashcardCategory),
                                  size: 16,
                                  color: _getCategoryColor(flashcardCategory),
                                ),
                                const SizedBox(width: 8),
                                Text(
                                  flashcardCategory.toUpperCase(),
                                  style: theme.textTheme.labelMedium?.copyWith(
                                    fontWeight: FontWeight.bold,
                                    color: _getCategoryColor(flashcardCategory),
                                  ),
                                ),
                              ],
                            ),
                          ),

                          const SizedBox(height: 24),

                          // Flashcard message
                          Text(
                            flashcardMessage,
                            style: theme.textTheme.headlineSmall?.copyWith(
                              fontWeight: FontWeight.w500,
                            ),
                            textAlign: TextAlign.center,
                          ),

                          const SizedBox(height: 24),

                          // Time indicator
                          Text(
                            'Time: ${_secondsElapsed}s',
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: theme.colorScheme.onSurfaceVariant,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),

                  const Spacer(),

                  // Action buttons
                  FilledButton.icon(
                    onPressed: _completeTask,
                    icon: const Icon(Icons.check_circle),
                    label: const Padding(
                      padding: EdgeInsets.symmetric(vertical: 12.0),
                      child: Text('I Did It!', style: TextStyle(fontSize: 16)),
                    ),
                  ),

                  const SizedBox(height: 12),

                  OutlinedButton(
                    onPressed: _skipTask,
                    child: const Padding(
                      padding: EdgeInsets.symmetric(vertical: 12.0),
                      child: Text('Skip for Now', style: TextStyle(fontSize: 16)),
                    ),
                  ),

                  const SizedBox(height: 8),

                  Text(
                    'Taking action helps break the pattern',
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                      fontStyle: FontStyle.italic,
                    ),
                    textAlign: TextAlign.center,
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
