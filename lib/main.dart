import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'src/screens/home_screen.dart';
import 'src/utils/app_theme.dart';

void main() {
  runApp(
    const ProviderScope(
      child: EraserApp(),
    ),
  );
}

class EraserApp extends StatelessWidget {
  const EraserApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Eraser - Content Blocker',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      darkTheme: AppTheme.darkTheme,
      themeMode: ThemeMode.system,
      home: const HomeScreen(),
    );
  }
}
