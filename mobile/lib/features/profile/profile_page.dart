import 'package:material_ui/material_ui.dart';
import 'package:flutter/services.dart';
import 'package:qr_flutter/qr_flutter.dart';

import '../../core/api.dart';
import '../../shared/workspace.dart';

class ProfilePage extends StatefulWidget {
  final AcademicApi api;
  const ProfilePage({super.key, required this.api});
  @override
  State<ProfilePage> createState() => _ProfileState();
}

class _ProfileState extends State<ProfilePage> {
  Map<String, dynamic>? profile;
  String? error;
  bool loading = false;
  @override
  void initState() {
    super.initState();
    load();
  }

  Future<void> load() async {
    if (loading) return;
    setState(() {
      loading = true;
      error = null;
    });
    try {
      final value = await widget.api.profile();
      if (mounted) setState(() => profile = value);
    } catch (e) {
      if (mounted) setState(() => error = apiError(e));
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  @override
  Widget build(BuildContext context) => Workspace(
    title: 'Mi código',
    api: widget.api,
    child: ListView(
      padding: const EdgeInsets.all(24),
      children: [
        if (loading) const Center(child: CircularProgressIndicator()),
        if (error != null)
          Text(error!, style: const TextStyle(color: Colors.red)),
        if (profile != null) ...[
          Text(
            profile!['fullName'] as String? ?? 'Mi identidad académica',
            style: Theme.of(context).textTheme.headlineSmall,
          ),
          const SizedBox(height: 16),
          if (profile!['profileComplete'] != true)
            const Text(
              'Completa tu perfil desde la web antes de compartir tu código con una institución.',
            )
          else ...[
            const Text(
              'Comparte este código únicamente con las instituciones que quieras autorizar. No muestra tu historial de otras organizaciones.',
            ),
            const SizedBox(height: 24),
            Center(
              child: QrImageView(
                data: profile!['searchCode'] as String,
                size: 240,
                backgroundColor: Colors.white,
                semanticsLabel: 'Código QR de búsqueda del estudiante',
              ),
            ),
            const SizedBox(height: 20),
            SelectableText(
              profile!['searchCode'] as String,
              textAlign: TextAlign.center,
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 20),
            FilledButton.icon(
              onPressed: () async {
                final messenger = ScaffoldMessenger.of(context);
                await Clipboard.setData(
                  ClipboardData(text: profile!['searchCode'] as String),
                );
                if (mounted) {
                  messenger.showSnackBar(
                    const SnackBar(content: Text('Código copiado.')),
                  );
                }
              },
              icon: const Icon(Icons.copy),
              label: const Text('Copiar código'),
            ),
            const SizedBox(height: 20),
            const Text(
              'Puedes retirar autorizaciones y cambiar el código desde tu cuenta en la versión web.',
            ),
          ],
        ],
        const SizedBox(height: 24),
        OutlinedButton(
          onPressed: loading ? null : load,
          child: const Text('Actualizar'),
        ),
      ],
    ),
  );
}
