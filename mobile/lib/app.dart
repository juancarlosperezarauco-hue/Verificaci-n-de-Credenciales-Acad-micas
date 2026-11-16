import 'package:material_ui/material_ui.dart';
import 'package:go_router/go_router.dart';

import 'core/api.dart';
import 'features/auth/login_page.dart';
import 'features/history/history_page.dart';
import 'features/profile/profile_page.dart';
import 'features/verify/verify_page.dart';
import 'features/verify/scanner_page.dart';

class CredentialApp extends StatefulWidget {
  final AcademicApi api;
  const CredentialApp({super.key, required this.api});
  @override
  State<CredentialApp> createState() => _AppState();
}

class _AppState extends State<CredentialApp> {
  late final GoRouter router = GoRouter(
    initialLocation: '/history',
    refreshListenable: widget.api.session,
    redirect: (context, state) {
      final public = {'/login', '/verify', '/scan'}.contains(state.uri.path);
      if (!widget.api.session.signedIn && !public) return '/login';
      if (widget.api.session.signedIn && state.uri.path == '/login') {
        return '/history';
      }
      return null;
    },
    routes: [
      GoRoute(
        path: '/login',
        builder: (context, state) => LoginPage(api: widget.api),
      ),
      GoRoute(
        path: '/history',
        builder: (context, state) => HistoryPage(api: widget.api),
      ),
      GoRoute(
        path: '/profile',
        builder: (context, state) => ProfilePage(api: widget.api),
      ),
      GoRoute(
        path: '/verify',
        builder: (context, state) =>
            VerifyPage(api: widget.api, initialCode: state.extra as String?),
      ),
      GoRoute(path: '/scan', builder: (context, state) => const ScannerPage()),
    ],
  );
  @override
  void dispose() {
    router.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => MaterialApp.router(
    title: 'Credenciales académicas',
    debugShowCheckedModeBanner: false,
    routerConfig: router,
    theme: ThemeData(
      useMaterial3: true,
      brightness: Brightness.light,
      colorScheme: ColorScheme.fromSeed(
        seedColor: const Color(0xff2563eb),
        brightness: Brightness.light,
      ),
      scaffoldBackgroundColor: const Color(0xfff8fafc),
      inputDecorationTheme: const InputDecorationTheme(
        border: OutlineInputBorder(),
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.white,
      ),
    ),
  );
}
