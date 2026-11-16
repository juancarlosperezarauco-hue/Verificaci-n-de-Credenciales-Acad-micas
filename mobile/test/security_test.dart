import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:credential_wallet/core/api.dart';
import 'package:credential_wallet/core/session.dart';

class MemoryStorage implements TokenStorage {
  String? value;
  bool failWrite = false;
  @override
  Future<String?> read() async => value;
  @override
  Future<void> write(String token) async {
    if (failWrite) throw StateError('unavailable');
    value = token;
  }

  @override
  Future<void> delete() async {
    value = null;
  }
}

class Adapter implements HttpClientAdapter {
  final requests = <RequestOptions>[];
  int status = 200;
  Map<String, dynamic> body = {};
  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? stream,
    Future<void>? cancel,
  ) async {
    requests.add(options);
    return ResponseBody.fromString(
      jsonEncode(body),
      status,
      headers: {
        Headers.contentTypeHeader: ['application/json'],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

void main() {
  test('release requires HTTPS and rejects credentials or paths in the API endpoint', () {
    for (final url in [
      'http://10.0.2.2:18080',
      'https://user:password@example.test',
      'https://example.test/other',
      'https://example.test?token=x',
    ]) {
      expect(() => validateApiUrl(url, release: true), throwsArgumentError);
    }
    expect(
      validateApiUrl('https://api.example.test', release: true).host,
      'api.example.test',
    );
  });
  test(
    'QR input extracts only credential codes and never uses a scanned server',
    () {
      final code = ('CRED-${List.filled(32, 'A').join()}');
      expect(verificationCode('https://untrusted.example/verify/$code'), code);
      expect(verificationCode('javascript:alert(1)'), isNull);
      expect(
        verificationCode('https://untrusted.example/login?token=secret'),
        isNull,
      );
      expect(verificationCode('EST-12345'), isNull);
    },
  );
  test('a token is saved before authenticating and storage failure does not authenticate', () async {
    final storage = MemoryStorage()..failWrite = true;
    final session = StudentSession(storage);
    await expectLater(session.save('test-token'), throwsStateError);
    expect(session.signedIn, isFalse);
    storage.failWrite = false;
    await session.save('test-token');
    expect(session.signedIn, isTrue);
    await session.clear();
    expect(session.signedIn, isFalse);
    expect(storage.value, isNull);
  });
  test('credentials stay within the configured API and public verification carries no token', () async {
    final session = StudentSession(MemoryStorage());
    await session.save('private-test-token');
    final adapter = Adapter()..body = {'items': [], 'total': 0};
    final dio = Dio()..httpClientAdapter = adapter;
    final api = AcademicApi('https://api.example.test', session, client: dio);
    await api.history(institution: 'tenant', year: 2025, type: 'CERTIFICADO');
    expect(
      adapter.requests.single.headers['Authorization'],
      'Bearer private-test-token',
    );
    expect(adapter.requests.single.queryParameters['year'], 2025);
    expect(adapter.requests.single.followRedirects, isFalse);
    await api.verify(('CRED-${List.filled(32, 'A').join()}'));
    expect(adapter.requests.last.headers.containsKey('Authorization'), isFalse);
    await expectLater(
      dio.get<dynamic>('https://untrusted.example/api/me/profile'),
      throwsA(isA<DioException>()),
    );
    expect(adapter.requests.length, 2);
  });
  test('an expired private session is cleared and failed logout still clears local credentials', () async {
    final session = StudentSession(MemoryStorage());
    await session.save('test-token');
    final adapter = Adapter()..status = 401;
    final dio = Dio()..httpClientAdapter = adapter;
    final api = AcademicApi('https://api.example.test', session, client: dio);
    await expectLater(api.profile(), throwsA(isA<DioException>()));
    expect(session.signedIn, isFalse);
    await session.save('test-token');
    adapter.status = 503;
    expect(await api.logout(), isFalse);
    expect(session.signedIn, isFalse);
  });
}
