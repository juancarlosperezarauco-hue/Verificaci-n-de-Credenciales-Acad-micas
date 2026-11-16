import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

abstract interface class TokenStorage {
  Future<String?> read();
  Future<void> write(String value);
  Future<void> delete();
}

class SecureTokenStorage implements TokenStorage {
  final FlutterSecureStorage storage;
  SecureTokenStorage([FlutterSecureStorage? storage])
      : storage = storage ?? const FlutterSecureStorage();
  static const key = 'academic_student_session';
  @override Future<String?> read() => storage.read(key: key);
  @override Future<void> write(String value) => storage.write(key: key, value: value);
  @override Future<void> delete() => storage.delete(key: key);
}

class StudentSession extends ChangeNotifier {
  final TokenStorage storage;
  StudentSession(this.storage);
  String? _token;
  String? get token => _token;
  bool get signedIn => _token != null;
  Future<void> restore() async {
    _token = await storage.read();
    if (_token?.isEmpty ?? false) _token = null;
    notifyListeners();
  }
  Future<void> save(String token) async {
    if (token.isEmpty) throw ArgumentError('Sesión vacía.');
    await storage.write(token);
    _token = token;
    notifyListeners();
  }
  Future<void> clear() async {
    _token = null;
    notifyListeners();
    await storage.delete();
  }
}
