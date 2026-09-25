import 'package:drift/drift.dart';
import 'package:drift/native.dart';
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as p;
import 'dart:io';

part 'database.g.dart';

// User table
class Users extends Table {
  IntColumn get id => integer().autoIncrement()();
  DateTimeColumn get startDate => dateTime()();
  IntColumn get currentStreak => integer().withDefault(const Constant(0))();
  IntColumn get longestStreak => integer().withDefault(const Constant(0))();
  IntColumn get totalDaysClean => integer().withDefault(const Constant(0))();
  DateTimeColumn get lastCheckInDate => dateTime().nullable()();
  BoolColumn get isVpnEnabled => boolean().withDefault(const Constant(false))();
  DateTimeColumn get journeyStartTime => dateTime().nullable()();
  DateTimeColumn get journeyStopTime => dateTime().nullable()();
}

// Flashcard table
class Flashcards extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get title => text()();
  TextColumn get message => text()();
  TextColumn get taskType => text()(); // Physical, Mindfulness, Social, Creative, Reflection
  TextColumn get difficulty => text()(); // Easy, Medium, Hard
  TextColumn get category => text()();
  BoolColumn get isActive => boolean().withDefault(const Constant(true))();
  IntColumn get timesShown => integer().withDefault(const Constant(0))();
  IntColumn get timesCompleted => integer().withDefault(const Constant(0))();
  IntColumn get timesSkipped => integer().withDefault(const Constant(0))();
  DateTimeColumn get lastShownAt => dateTime().nullable()();
  RealColumn get effectivenessScore => real().withDefault(const Constant(0.0))();
}

// Achievement table
class Achievements extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get title => text()();
  TextColumn get description => text()();
  TextColumn get icon => text()();
  IntColumn get milestone => integer().nullable()(); // Days required (1, 3, 7, 14, 30, etc.)
  BoolColumn get isUnlocked => boolean().withDefault(const Constant(false))();
  DateTimeColumn get earnedDate => dateTime().nullable()();
}

// Blocked attempt table
class BlockedAttempts extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get domain => text()();
  TextColumn get url => text()();
  DateTimeColumn get timestamp => dateTime()();
  BoolColumn get isIntervened => boolean().withDefault(const Constant(false))();
}

// Daily log table
class DailyLogs extends Table {
  IntColumn get id => integer().autoIncrement()();
  DateTimeColumn get logDate => dateTime()();
  BoolColumn get checkedIn => boolean().withDefault(const Constant(false))();
  IntColumn get moodRating => integer().nullable()(); // 1-5 scale
  TextColumn get reflectionNote => text().nullable()();
  IntColumn get cleanTime => integer().nullable()(); // Minutes clean that day
}

// Intervention session table
class InterventionSessions extends Table {
  IntColumn get id => integer().autoIncrement()();
  DateTimeColumn get timestamp => dateTime()();
  TextColumn get blockedDomain => text()();
  TextColumn get blockedUrl => text()();
  IntColumn get flashcardId => integer().nullable()();
  BoolColumn get taskCompleted => boolean().withDefault(const Constant(false))();
  BoolColumn get taskSkipped => boolean().withDefault(const Constant(false))();
  IntColumn get completionTime => integer().nullable()(); // Seconds to complete
}

@DriftDatabase(tables: [
  Users,
  Flashcards,
  Achievements,
  BlockedAttempts,
  DailyLogs,
  InterventionSessions,
])
class EraserDatabase extends _$EraserDatabase {
  EraserDatabase() : super(_openConnection());

  @override
  int get schemaVersion => 1;

  @override
  MigrationStrategy get migration {
    return MigrationStrategy(
      onCreate: (Migrator m) async {
        await m.createAll();

        // Insert default user
        await into(users).insert(
          UsersCompanion.insert(
            startDate: DateTime.now(),
            currentStreak: const Value(0),
            longestStreak: const Value(0),
            totalDaysClean: const Value(0),
          ),
        );

        // Insert default achievements
        await _insertDefaultAchievements();

        // Insert flashcards
        await _insertDefaultFlashcards();
      },
      onUpgrade: (Migrator m, int from, int to) async {
        // Handle migrations in future versions
      },
    );
  }

  Future<void> _insertDefaultAchievements() async {
    final defaultAchievements = [
      AchievementsCompanion.insert(
        title: '1 Day Strong',
        description: 'Completed your first day clean',
        icon: '🌟',
        milestone: const Value(1),
      ),
      AchievementsCompanion.insert(
        title: '3 Day Warrior',
        description: 'Three days of freedom',
        icon: '💪',
        milestone: const Value(3),
      ),
      AchievementsCompanion.insert(
        title: '1 Week Champion',
        description: 'Seven days of victory',
        icon: '🏆',
        milestone: const Value(7),
      ),
      AchievementsCompanion.insert(
        title: '2 Week Hero',
        description: 'Two weeks of strength',
        icon: '🦸',
        milestone: const Value(14),
      ),
      AchievementsCompanion.insert(
        title: '1 Month Legend',
        description: 'Thirty days of transformation',
        icon: '👑',
        milestone: const Value(30),
      ),
      AchievementsCompanion.insert(
        title: '60 Day Master',
        description: 'Two months of freedom',
        icon: '🎖️',
        milestone: const Value(60),
      ),
      AchievementsCompanion.insert(
        title: '90 Day Victor',
        description: 'Three months of success',
        icon: '🥇',
        milestone: const Value(90),
      ),
      AchievementsCompanion.insert(
        title: '6 Month Titan',
        description: 'Half a year clean',
        icon: '⭐',
        milestone: const Value(180),
      ),
      AchievementsCompanion.insert(
        title: '1 Year Legend',
        description: 'One full year of freedom',
        icon: '🔥',
        milestone: const Value(365),
      ),
      AchievementsCompanion.insert(
        title: 'First Intervention',
        description: 'Completed your first intervention task',
        icon: '🎯',
        milestone: const Value(null),
      ),
      AchievementsCompanion.insert(
        title: 'Streak Keeper',
        description: 'Maintained a 7-day streak',
        icon: '🔒',
        milestone: const Value(null),
      ),
    ];

    for (final achievement in defaultAchievements) {
      await into(achievements).insert(achievement);
    }
  }

  Future<void> _insertDefaultFlashcards() async {
    // Physical activities
    final physicalCards = [
      ('Take a 10-minute walk', 'Physical'),
      ('Do 20 push-ups', 'Physical'),
      ('Run for 15 minutes', 'Physical'),
      ('Stretch for 5 minutes', 'Physical'),
      ('Dance to your favorite song', 'Physical'),
      ('Do jumping jacks for 2 minutes', 'Physical'),
      ('Practice yoga poses for 10 minutes', 'Physical'),
      ('Go for a bike ride', 'Physical'),
      ('Do a quick home workout', 'Physical'),
      ('Take a cold shower', 'Physical'),
    ];

    // Mindfulness activities
    final mindfulnessCards = [
      ('Meditate for 5 minutes', 'Mindfulness'),
      ('Practice deep breathing (4-7-8 technique)', 'Mindfulness'),
      ('Write in your journal', 'Mindfulness'),
      ('List 5 things you\'re grateful for', 'Mindfulness'),
      ('Practice progressive muscle relaxation', 'Mindfulness'),
      ('Sit in silence for 3 minutes', 'Mindfulness'),
      ('Do a body scan meditation', 'Mindfulness'),
      ('Practice mindful breathing', 'Mindfulness'),
      ('Observe your thoughts without judgment', 'Mindfulness'),
      ('Focus on the present moment', 'Mindfulness'),
    ];

    // Social activities
    final socialCards = [
      ('Call a friend or family member', 'Social'),
      ('Send a kind message to someone', 'Social'),
      ('Join an online support group', 'Social'),
      ('Reach out to your accountability partner', 'Social'),
      ('Help someone in need', 'Social'),
      ('Share your progress with someone you trust', 'Social'),
      ('Express gratitude to someone', 'Social'),
      ('Plan a social activity for this week', 'Social'),
      ('Connect with your community', 'Social'),
      ('Have a meaningful conversation', 'Social'),
    ];

    // Creative activities
    final creativeCards = [
      ('Draw or sketch for 10 minutes', 'Creative'),
      ('Write a poem or short story', 'Creative'),
      ('Play a musical instrument', 'Creative'),
      ('Work on a hobby project', 'Creative'),
      ('Learn something new online', 'Creative'),
      ('Build or create something', 'Creative'),
      ('Take photos of interesting things', 'Creative'),
      ('Cook a new recipe', 'Creative'),
      ('Organize a space in your home', 'Creative'),
      ('Start a creative project', 'Creative'),
    ];

    // Reflection activities
    final reflectionCards = [
      ('Write about your recovery goals', 'Reflection'),
      ('Reflect on your progress so far', 'Reflection'),
      ('Identify your triggers', 'Reflection'),
      ('Plan your day with intention', 'Reflection'),
      ('Review your values and priorities', 'Reflection'),
      ('Write a letter to your future self', 'Reflection'),
      ('Think about why you started this journey', 'Reflection'),
      ('Acknowledge your strength', 'Reflection'),
      ('Reflect on lessons learned', 'Reflection'),
      ('Visualize your ideal future', 'Reflection'),
    ];

    final allCards = [
      ...physicalCards,
      ...mindfulnessCards,
      ...socialCards,
      ...creativeCards,
      ...reflectionCards,
    ];

    for (var i = 0; i < allCards.length; i++) {
      final (message, category) = allCards[i];
      await into(flashcards).insert(
        FlashcardsCompanion.insert(
          title: 'Take Action',
          message: message,
          taskType: category,
          difficulty: i % 3 == 0 ? 'Easy' : (i % 3 == 1 ? 'Medium' : 'Hard'),
          category: category,
        ),
      );
    }
  }

  // User queries
  Future<User?> getUser() => (select(users)..limit(1)).getSingleOrNull();

  Future<void> updateUserStreak(int currentStreak, int longestStreak) {
    return (update(users)..where((u) => u.id.equals(1))).write(
      UsersCompanion(
        currentStreak: Value(currentStreak),
        longestStreak: Value(longestStreak),
        lastCheckInDate: Value(DateTime.now()),
      ),
    );
  }

  // Flashcard queries
  Future<List<Flashcard>> getAllFlashcards() => select(flashcards).get();

  Future<List<Flashcard>> getActiveFlashcards() =>
      (select(flashcards)..where((f) => f.isActive.equals(true))).get();

  Future<Flashcard?> getRandomFlashcard() async {
    final cards = await getActiveFlashcards();
    if (cards.isEmpty) return null;
    cards.shuffle();
    return cards.first;
  }

  // Achievement queries
  Future<List<Achievement>> getAllAchievements() => select(achievements).get();

  Future<List<Achievement>> getUnlockedAchievements() =>
      (select(achievements)..where((a) => a.isUnlocked.equals(true))).get();

  Future<void> unlockAchievement(int achievementId) {
    return (update(achievements)..where((a) => a.id.equals(achievementId))).write(
      AchievementsCompanion(
        isUnlocked: const Value(true),
        earnedDate: Value(DateTime.now()),
      ),
    );
  }

  // Blocked attempt queries
  Future<void> insertBlockedAttempt(String domain, String url) {
    return into(blockedAttempts).insert(
      BlockedAttemptsCompanion.insert(
        domain: domain,
        url: url,
        timestamp: DateTime.now(),
      ),
    );
  }

  Future<List<BlockedAttempt>> getRecentBlockedAttempts({int limit = 50}) {
    return (select(blockedAttempts)
          ..orderBy([(t) => OrderingTerm.desc(t.timestamp)])
          ..limit(limit))
        .get();
  }

  Future<int> getBlockedAttemptsToday() async {
    final today = DateTime.now();
    final startOfDay = DateTime(today.year, today.month, today.day);

    return (select(blockedAttempts)
          ..where((t) => t.timestamp.isBiggerOrEqualValue(startOfDay)))
        .get()
        .then((list) => list.length);
  }

  Future<int> getBlockedAttemptsThisWeek() async {
    final now = DateTime.now();
    final startOfWeek = now.subtract(Duration(days: now.weekday - 1));
    final startOfWeekDay = DateTime(startOfWeek.year, startOfWeek.month, startOfWeek.day);

    return (select(blockedAttempts)
          ..where((t) => t.timestamp.isBiggerOrEqualValue(startOfWeekDay)))
        .get()
        .then((list) => list.length);
  }

  // Intervention session queries
  Future<void> insertInterventionSession({
    required String blockedDomain,
    required String blockedUrl,
    required int flashcardId,
  }) {
    return into(interventionSessions).insert(
      InterventionSessionsCompanion.insert(
        timestamp: DateTime.now(),
        blockedDomain: blockedDomain,
        blockedUrl: blockedUrl,
        flashcardId: Value(flashcardId),
      ),
    );
  }

  Future<void> completeInterventionSession(int sessionId, int completionTime) {
    return (update(interventionSessions)..where((s) => s.id.equals(sessionId))).write(
      InterventionSessionsCompanion(
        taskCompleted: const Value(true),
        completionTime: Value(completionTime),
      ),
    );
  }
}

LazyDatabase _openConnection() {
  return LazyDatabase(() async {
    final dbFolder = await getApplicationDocumentsDirectory();
    final file = File(p.join(dbFolder.path, 'eraser.db'));
    return NativeDatabase(file);
  });
}
