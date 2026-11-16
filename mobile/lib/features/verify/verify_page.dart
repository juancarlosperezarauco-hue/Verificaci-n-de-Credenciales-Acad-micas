import 'package:material_ui/material_ui.dart';
import 'package:go_router/go_router.dart';

import '../../core/api.dart';
import '../../shared/workspace.dart';

class VerifyPage extends StatefulWidget {
  final AcademicApi api;
  final String? initialCode;
  const VerifyPage({super.key, required this.api, this.initialCode});
  @override
  State<VerifyPage> createState() => _VerifyState();
}

class _VerifyState extends State<VerifyPage> {
  final code = TextEditingController();
  Map<String, dynamic>? result;
  String? error;
  bool busy = false;
  @override
  void initState() {
    super.initState();
    if (widget.initialCode != null) {
      code.text = widget.initialCode!;
      lookup();
    }
  }

  @override
  void dispose() {
    code.dispose();
    super.dispose();
  }

  Future<void> lookup() async {
    if (busy) return;
    final value = verificationCode(code.text);
    if (value == null) {
      setState(() => error = 'Escribe un código CRED válido o escanea su QR.');
      return;
    }
    setState(() {
      busy = true;
      error = null;
      result = null;
    });
    try {
      final row = await widget.api.verify(value);
      if (mounted) setState(() => result = row);
    } catch (e) {
      if (mounted) setState(() => error = apiError(e));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => Workspace(
    title: 'Verificar documento',
    api: widget.api,
    child: ListView(
      padding: const EdgeInsets.all(24),
      children: [
        const Text('Consulta el registro de una credencial en la plataforma.'),
        const SizedBox(height: 20),
        TextField(
          controller: code,
          enabled: !busy,
          autocorrect: false,
          decoration: const InputDecoration(
            labelText: 'Código de verificación',
            hintText: 'CRED-…',
          ),
          onSubmitted: (_) => lookup(),
        ),
        const SizedBox(height: 16),
        Wrap(
          spacing: 12,
          children: [
            FilledButton(
              onPressed: busy ? null : lookup,
              child: Text(busy ? 'Consultando…' : 'Consultar'),
            ),
            OutlinedButton.icon(
              onPressed: busy
                  ? null
                  : () async {
                      final scanned = await context.push<String>('/scan');
                      if (scanned != null && mounted) {
                        code.text = scanned;
                        lookup();
                      }
                    },
              icon: const Icon(Icons.qr_code_scanner),
              label: const Text('Escanear QR'),
            ),
          ],
        ),
        if (error != null)
          Padding(
            padding: const EdgeInsets.only(top: 20),
            child: Text(error!, style: const TextStyle(color: Colors.red)),
          ),
        if (result != null)
          Card(
            margin: const EdgeInsets.only(top: 24),
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    result!['title'] as String,
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                  const SizedBox(height: 12),
                  Text(result!['organization'] as String),
                  const SizedBox(height: 8),
                  Text(result!['holderName'] as String),
                  const SizedBox(height: 16),
                  Text(
                    result!['status'] == 'REVOKED'
                        ? 'CREDENCIAL REVOCADA'
                        : 'Registro emitido',
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                      color: result!['status'] == 'REVOKED'
                          ? Colors.red
                          : Colors.teal,
                    ),
                  ),
                  const SizedBox(height: 12),
                  Text(
                    'Estado actual de la institución: ${result!['issuerStatus']}',
                  ),
                  const SizedBox(height: 16),
                  const Text(
                    'Esta consulta comprueba el registro y su estado. No compara tu PDF, su firma, la acreditación educativa ni la validez legal.',
                  ),
                  if (result!['testSignature'] == true)
                    const Padding(
                      padding: EdgeInsets.only(top: 12),
                      child: Text(
                        'El documento tiene una firma de prueba.',
                        style: TextStyle(color: Colors.deepOrange),
                      ),
                    ),
                ],
              ),
            ),
          ),
      ],
    ),
  );
}
