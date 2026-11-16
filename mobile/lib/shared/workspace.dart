import 'package:material_ui/material_ui.dart';
import 'package:go_router/go_router.dart';

import '../core/api.dart';

class Workspace extends StatelessWidget {
  final String title;
  final Widget child;
  final AcademicApi api;
  const Workspace({
    super.key,
    required this.title,
    required this.child,
    required this.api,
  });
  @override
  Widget build(BuildContext context) {
    final path = GoRouterState.of(context).uri.path;
    final index = path == '/history'
        ? 0
        : path == '/profile'
        ? 1
        : 2;
    return Scaffold(
      appBar: AppBar(
        title: Text(title),
        actions: [
          if (api.session.signedIn)
            IconButton(
              tooltip: 'Cerrar sesión',
              onPressed: () async {
                final messenger = ScaffoldMessenger.of(context);
                final confirmed = await api.logout();
                if (!confirmed) {
                  messenger.showSnackBar(
                    const SnackBar(
                      content: Text(
                        'La sesión local se cerró. Revisa tus sesiones en la web para confirmar el cierre del servidor.',
                      ),
                    ),
                  );
                }
              },
              icon: const Icon(Icons.logout),
            ),
        ],
      ),
      body: SafeArea(child: child),
      bottomNavigationBar: NavigationBar(
        selectedIndex: index,
        onDestinationSelected: (selected) =>
            context.go(['/history', '/profile', '/verify'][selected]),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.folder_outlined),
            label: 'Expediente',
          ),
          NavigationDestination(icon: Icon(Icons.qr_code), label: 'Mi código'),
          NavigationDestination(
            icon: Icon(Icons.verified_outlined),
            label: 'Verificar',
          ),
        ],
      ),
    );
  }
}
