import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

import 'session.dart';

Uri validateApiUrl(String value, {bool release = kReleaseMode}) {
  final uri = Uri.parse(value);
  final local =
      !release && {'10.0.2.2', 'localhost', '127.0.0.1'}.contains(uri.host);
  if (uri.host.isEmpty ||
      uri.userInfo.isNotEmpty ||
      uri.hasQuery ||
      uri.hasFragment ||
      (uri.path.isNotEmpty && uri.path != '/') ||
      (uri.scheme != 'https' && !(uri.scheme == 'http' && local))) {
    throw ArgumentError(
      'Configura una URL HTTPS de la API. HTTP solo está permitido en el emulador local de desarrollo.',
    );
  }
  return uri;
}

String? verificationCode(String raw) {
  var value = raw.trim().toUpperCase();
  final uri = Uri.tryParse(raw.trim());
  if (uri != null &&
      {'http', 'https'}.contains(uri.scheme) &&
      uri.pathSegments.length == 2 &&
      uri.pathSegments.first == 'verify') {
    // Only extract a code. Never follow a scanned URL or send a token to its host.
    value = uri.pathSegments.last.toUpperCase();
  }
  return RegExp(r'^CRED-[0-9A-F]{32}$').hasMatch(value) ? value : null;
}

String apiError(Object error) {
  if (error is DioException) {
    if (CancelToken.isCancel(error)) return 'La consulta fue cancelada.';
    final status = error.response?.statusCode;
    if (status == 401) {
      return 'La sesión terminó o los datos de acceso no son correctos.';
    }
    if (status == 403) return 'Tu cuenta no tiene acceso a esta operación.';
    if (status == 404) return 'No se encontró el documento solicitado.';
    if (status == 429) return 'Espera un momento antes de volver a consultar.';
    return 'No se pudo conectar. Revisa tu conexión e inténtalo nuevamente.';
  }
  return 'No se pudo completar la operación.';
}

class AcademicApi {
  final Dio dio;
  final StudentSession session;
  AcademicApi(String baseUrl, this.session, {Dio? client})
    : dio = client ?? Dio() {
    final base = validateApiUrl(baseUrl);
    dio.options = BaseOptions(
      baseUrl: base.origin,
      followRedirects: false,
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 25),
    );
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          if (options.uri.origin != base.origin ||
              !options.path.startsWith('/api/')) {
            handler.reject(
              DioException(
                requestOptions: options,
                error: 'La petición salió del servidor configurado.',
              ),
            );
            return;
          }
          if (!options.path.startsWith('/api/public/') &&
              session.token != null) {
            options.headers['Authorization'] = 'Bearer ${session.token}';
          }
          handler.next(options);
        },
        onError: (error, handler) {
          if (error.response?.statusCode == 401 &&
              !error.requestOptions.path.startsWith('/api/public/')) {
            unawaited(session.clear().catchError((_) {}));
          }
          handler.next(error);
        },
      ),
    );
  }
  Future<void> login(String email, String password) async {
    final response = await dio.post<Map<String, dynamic>>(
      '/api/auth/student/login',
      data: {'email': email.trim(), 'password': password},
    );
    final body = response.data!;
    if (body['role'] != 'STUDENT' || body['token'] is! String) {
      throw StateError('Respuesta de acceso no válida.');
    }
    await session.save(body['token'] as String);
  }

  Future<bool> logout() async {
    var confirmed = true;
    try {
      await dio.delete<void>('/api/security/sessions/current');
    } catch (_) {
      confirmed = false;
    } finally {
      try {
        await session.clear();
      } catch (_) {
        confirmed = false;
      }
    }
    return confirmed;
  }

  Future<Map<String, dynamic>> profile() async =>
      (await dio.get<Map<String, dynamic>>('/api/me/profile')).data!;
  Future<Map<String, dynamic>> filters() async =>
      (await dio.get<Map<String, dynamic>>('/api/me/credentials/filters'))
          .data!;
  Future<Map<String, dynamic>> history({
    int page = 0,
    String? institution,
    int? year,
    String? type,
    CancelToken? cancel,
  }) async => (await dio.get<Map<String, dynamic>>(
    '/api/me/credentials/page',
    queryParameters: {
      'page': page,
      'size': 10,
      'institutionId': ?institution,
      'year': ?year,
      'type': ?type,
    },
    cancelToken: cancel,
  )).data!;
  Future<Uint8List> document(String id) async {
    if (!RegExp(r'^[0-9a-fA-F-]{36}$').hasMatch(id)) {
      throw ArgumentError('Documento no válido.');
    }
    final cancel = CancelToken();
    final response = await dio.get<List<int>>(
      '/api/me/credentials/$id/document',
      options: Options(responseType: ResponseType.bytes),
      cancelToken: cancel,
      onReceiveProgress: (received, total) {
        if (received > 11 * 1024 * 1024 || total > 11 * 1024 * 1024) {
          cancel.cancel();
        }
      },
    );
    final bytes = Uint8List.fromList(response.data!);
    if (bytes.length < 5 ||
        bytes.length > 11 * 1024 * 1024 ||
        String.fromCharCodes(bytes.take(4)) != '%PDF') {
      throw StateError('El archivo recibido no es un PDF válido.');
    }
    return bytes;
  }

  Future<Map<String, dynamic>> verify(String code) async {
    final normalized = verificationCode(code);
    if (normalized == null) {
      throw ArgumentError('Código de credencial no válido.');
    }
    return (await dio.get<Map<String, dynamic>>(
      '/api/public/credentials/$normalized',
    )).data!;
  }
}
