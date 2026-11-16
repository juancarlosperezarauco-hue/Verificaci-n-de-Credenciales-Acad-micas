# Expediente Android

Flutter 3.50.0 / Dart 3.14.0. Cuenta STUDENT global; no se crean trabajadores tenant.
Incluye login, token en almacenamiento seguro, expediente paginado con filtros,
guardado de PDF mediante el selector nativo de Android, QR propio y lectura de QR
de credenciales. Los QR solo aportan el código: nunca se abre su URL ni se envía
el token a un servidor leído de la cámara. La consulta pública distingue registro
y archivo sin comprobar.

Desde PowerShell 7, en la raíz del repositorio:

    ./scripts/setup-mobile.ps1 -Flutter C:/ruta/flutter/bin/flutter.bat
    cd mobile
    flutter analyze
    flutter test
    flutter build apk --debug --dart-define=API_BASE_URL=http://10.0.2.2:18080
    flutter run --dart-define=API_BASE_URL=http://10.0.2.2:18080

El emulador usa 10.0.2.2 para acceder al backend local. Producción exige API HTTPS.
La aplicación no contiene secretos de IA, MinIO ni firma institucional. No se
respaldan sus datos mediante Android Auto Backup. El usuario elige dónde guardar
sus PDFs; esos archivos no se borran al cerrar sesión.

La preparación genera el proyecto Android oficial con la versión de Flutter fijada
y aplica las fuentes nativas mantenidas en platform/android. No reescribe lib/.
Una compilación release requiere android/key.properties y un keystore privado;
el script evita firmar silenciosamente publicaciones con la clave debug.
La distribución Play Store, dominio y pruebas en un teléfono real requieren
recursos que el usuario todavía no tiene configurados.
