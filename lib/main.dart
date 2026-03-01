import 'dart:convert';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:file_picker/file_picker.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Ringtone Rotator',
      theme: ThemeData(
        colorScheme: .fromSeed(
          seedColor: Colors.deepPurple,
          brightness: Brightness.dark,
        ),
      ),
      home: const MyHomePage(title: 'Ringtone Rotator'),
    );
  }
}

class Ringtone {
  final int id;
  final String name;
  final String path;

  Ringtone({required this.id, required this.name, required this.path});

  Map<String, dynamic> toMap() {
    return {'id': id, 'name': name, 'path': path};
  }

  factory Ringtone.fromMap(Map<String, dynamic> map) {
    return Ringtone(id: map['id'], name: map['name'], path: map['path']);
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  @override
  void initState() {
    super.initState();
    _loadRingtones();
  }

  final List<Ringtone> _ringtones = [];
  int _nextRingtoneId = 1;

  Future<void> _addRingtone() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.audio,
      allowMultiple: false,
    );

    if (result == null) return; // user cancelled
    final file = result.files.single;

    const audioExts = [
      'mp3',
      'wav',
      'ogg',
      'flac',
      'aac',
      'm4a',
      'wma',
      'aiff',
      'opus',
    ];
    final ext = file.extension?.toLowerCase();

    if (!context.mounted) return;
    if (!(ext != null && audioExts.contains(ext))) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Selected file is not an audio file.')),
      );
      return;
    }

    setState(() {
      final id = _nextRingtoneId++;
      _ringtones.add(Ringtone(id: id, name: file.name, path: file.path ?? ''));
    });
    await _saveRingtones();
  }

  Future<void> _loadRingtones() async {
    final prefs = await SharedPreferences.getInstance();
    final data = prefs.getStringList('ringtones');

    if (data == null) return;

    setState(() {
      _ringtones.clear();
      _ringtones.addAll(
        data.map((ringtone) => Ringtone.fromMap(jsonDecode(ringtone))),
      );
    });

    _nextRingtoneId =
        _ringtones.map((ringtone) => ringtone.id).fold(0, max) + 1;
  }

  Future<void> _saveRingtones() async {
    final prefs = await SharedPreferences.getInstance();

    final data = _ringtones
        .map((ringtone) => jsonEncode(ringtone.toMap()))
        .toList();
    await prefs.setStringList('ringtones', data);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text(widget.title),
        actions: [
          IconButton(
            icon: Icon(Icons.settings),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (context) => const SettingsPage()),
              );
            },
          ),
        ],
      ),
      body: ListView.builder(
        itemCount: _ringtones.length,
        itemBuilder: (context, index) {
          return ListTile(title: Text(_ringtones[index].name));
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _addRingtone,
        tooltip: 'Add a new Ringtone',
        child: const Icon(Icons.add),
      ),
    );
  }
}

class SettingsPage extends StatelessWidget {
  const SettingsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Settings')),
      body: const Center(child: Text('Settings Page')),
    );
  }
}
