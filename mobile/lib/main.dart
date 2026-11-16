import 'package:material_ui/material_ui.dart';

import 'app.dart';
import 'core/api.dart';
import 'core/session.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final session = StudentSession(SecureTokenStorage());
  try {
    await session.restore();
  } catch (_) {
    /* A missing local key requires signing in again. */
  }
  const endpoint = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:18080',
  );
  runApp(CredentialApp(api: AcademicApi(endpoint, session)));
}
