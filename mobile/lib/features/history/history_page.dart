import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import '../../core/api.dart';
import '../../shared/workspace.dart';

class HistoryPage extends StatefulWidget {
  final AcademicApi api;
  const HistoryPage({super.key, required this.api});
  @override State<HistoryPage> createState() => _HistoryState();
}
class _HistoryState extends State<HistoryPage> {
  Map<String, dynamic> filters = {'institutions': [], 'types': [], 'years': []};
  List<dynamic> rows = [];
  int page = 0, total = 0, generation = 0;
  String? institution, type, error, downloading;
  int? year;
  bool loading = true;
  CancelToken? request;
  static const channel = MethodChannel('bo.edu.uagrm.credentials/documents');
  @override void initState() { super.initState(); loadFilters(); load(); }
  @override void dispose() { request?.cancel(); super.dispose(); }
  Future<void> loadFilters() async {
    try { final data = await widget.api.filters(); if(mounted) setState(() => filters = data); }
    catch(e) { if(mounted) setState(() => error = apiError(e)); }
  }
  Future<void> load([int next = 0]) async {
    final mine = ++generation;
    request?.cancel(); request = CancelToken();
    setState(() {loading = true; error = null;});
    try {
      final result = await widget.api.history(page: next, institution: institution,
          year: year, type: type, cancel: request);
      if(mounted && mine == generation) setState(() {
        rows = result['items'] as List<dynamic>; total = (result['total'] as num).toInt();
        page = next;
      });
    } catch(e) {
      if(mounted && mine == generation && !(e is DioException && CancelToken.isCancel(e))) {
        setState(() {error = apiError(e); rows = [];});
      }
    } finally { if(mounted && mine == generation) setState(() => loading = false); }
  }
  Future<void> download(Map<String, dynamic> credential) async {
    if(downloading != null) return;
    final id = credential['credentialId'] as String;
    setState(() {downloading = id; error = null;});
    try {
      final bytes = await widget.api.document(id);
      if(!mounted) return;
      final saved = await channel.invokeMethod<bool>('savePdf', {
        'bytes': bytes, 'name': 'credencial-${credential['verificationCode']}.pdf'});
      if(mounted && saved == true) ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('PDF guardado en la ubicación que elegiste.')));
    } catch(e) { if(mounted) setState(() => error = e is PlatformException
        ? 'No se pudo guardar el PDF en el dispositivo.' : apiError(e)); }
    finally { if(mounted) setState(() => downloading = null); }
  }
  @override Widget build(BuildContext context) => Workspace(title: 'Mi expediente', api: widget.api,
    child: RefreshIndicator(onRefresh: () => load(page), child: ListView(
      physics: const AlwaysScrollableScrollPhysics(), padding: const EdgeInsets.all(20), children: [
      Text('Mis diplomas y certificados', style: Theme.of(context).textTheme.headlineSmall),
      const SizedBox(height: 8),
      const Text('Tus documentos de todas las instituciones, sin duplicar tu identidad.'),
      const SizedBox(height: 20),
      DropdownButtonFormField<String>(initialValue: institution,
        decoration: const InputDecoration(labelText: 'Institución'), hint: const Text('Todas'),
        isExpanded: true, items: [
          const DropdownMenuItem<String>(value: null, child: Text('Todas')),
          for(final value in filters['institutions'] as List<dynamic>)
            DropdownMenuItem(value: value['id'] as String, child: Text(value['name'] as String, overflow: TextOverflow.ellipsis)),
        ], onChanged: loading ? null : (value) {setState(() => institution = value); load();}),
      const SizedBox(height: 14),
      Row(children: [
        Expanded(child: DropdownButtonFormField<int>(initialValue: year, hint: const Text('Todos'),
          decoration: const InputDecoration(labelText: 'Año (UTC)'), items: [
            const DropdownMenuItem<int>(value: null, child: Text('Todos')),
            for(final value in filters['years'] as List<dynamic>)
              DropdownMenuItem(value: (value as num).toInt(), child: Text(value.toString())),
          ], onChanged: loading ? null : (value) {setState(() => year = value); load();})),
        const SizedBox(width: 12),
        Expanded(child: DropdownButtonFormField<String>(initialValue: type, hint: const Text('Todos'),
          isExpanded: true, decoration: const InputDecoration(labelText: 'Documento'), items: [
            const DropdownMenuItem<String>(value: null, child: Text('Todos')),
            for(final value in filters['types'] as List<dynamic>)
              DropdownMenuItem(value: value as String, child: Text(value, overflow: TextOverflow.ellipsis)),
          ], onChanged: loading ? null : (value) {setState(() => type = value); load();})),
      ]),
      if(error != null) Padding(padding: const EdgeInsets.symmetric(vertical: 16),
        child: Text(error!, style: const TextStyle(color: Colors.red))),
      if(loading) const Padding(padding: EdgeInsets.all(24), child: Center(child: CircularProgressIndicator()))
      else ...[
        Padding(padding: const EdgeInsets.symmetric(vertical: 16), child: Text('$total documentos')),
        if(rows.isEmpty && error == null) const Padding(padding: EdgeInsets.all(24),
          child: Text('No hay documentos con estos filtros.')),
        for(final raw in rows) documentCard(Map<String,dynamic>.from(raw as Map)),
        Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
          TextButton(onPressed: page > 0 ? () => load(page-1) : null, child: const Text('Anterior')),
          Text('Página ${page+1}'),
          TextButton(onPressed: (page+1)*10 < total ? () => load(page+1) : null, child: const Text('Siguiente')),
        ]),
      ],
      if(error != null) OutlinedButton(onPressed: loading ? null : () => load(page),
          child: const Text('Volver a consultar')),
    ])));
  Widget documentCard(Map<String,dynamic> c) => Card(
    margin: const EdgeInsets.only(bottom: 16),
    child: Padding(padding: const EdgeInsets.all(18), child: Column(
      crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(c['title'] as String, style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 6),
        Text(c['organization'] as String),
        const SizedBox(height: 8),
        Text(c['status'] == 'REVOKED' ? 'Revocado' : 'Emitido',
          style: TextStyle(color: c['status'] == 'REVOKED' ? Colors.red : Colors.teal)),
        const SizedBox(height: 4), Text(c['credentialType'] as String),
        const SizedBox(height: 12),
        Wrap(spacing: 10, children: [
          OutlinedButton(onPressed: () => context.push('/verify', extra: c['verificationCode']),
            child: const Text('Verificar')),
          if(c['documentName'] != null) FilledButton.icon(
            onPressed: downloading != null ? null : () => download(c),
            icon: const Icon(Icons.download_outlined),
            label: Text(downloading == c['credentialId'] ? 'Descargando…' : 'Guardar PDF')),
        ]),
      ])));
}
