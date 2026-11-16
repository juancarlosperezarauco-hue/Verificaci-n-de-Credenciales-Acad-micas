import 'package:material_ui/material_ui.dart';
import 'package:go_router/go_router.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

import '../../core/api.dart';

class ScannerPage extends StatefulWidget {
  const ScannerPage({super.key});
  @override
  State<ScannerPage> createState() => _ScannerState();
}

class _ScannerState extends State<ScannerPage> {
  final controller = MobileScannerController();
  bool done = false;
  String? error;
  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Escanear credencial')),
    body: Stack(
      children: [
        MobileScanner(
          controller: controller,
          errorBuilder: (context, error) => const Center(
            child: Padding(
              padding: EdgeInsets.all(24),
              child: Text(
                'No se pudo abrir la cámara. Vuelve e introduce el código manualmente.',
              ),
            ),
          ),
          onDetect: (capture) {
            if (done) return;
            for (final barcode in capture.barcodes) {
              final code = verificationCode(barcode.rawValue ?? '');
              if (code != null) {
                done = true;
                controller.stop();
                context.pop(code);
                return;
              }
            }
            setState(
              () => error =
                  'Este QR no contiene un código de credencial CRED válido.',
            );
          },
        ),
        Align(
          alignment: Alignment.bottomCenter,
          child: Container(
            color: Colors.white,
            padding: const EdgeInsets.all(24),
            child: Text(
              error ?? 'Apunta al QR del diploma o certificado. Solo se consulta el servidor configurado.',
            ),
          ),
        ),
      ],
    ),
  );
}
