import 'dart:convert';
import 'dart:math';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:ringtone_changer/features/settings/settings_page.dart';
import 'package:ringtone_changer/models/ringtone.dart';
import 'package:ringtone_changer/services/call_state_service.dart';
import 'package:shared_preferences/shared_preferences.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key, required this.title});

  final String title;

  @override
  State<HomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<HomePage> with WidgetsBindingObserver {
  final _callStateService = CallStateService();
  bool hasPhonePermission = false;
  bool hasWriteSettingsPermission = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _loadRingtones();
    _callStateService.initialize(
      onAccepted: _onAccepted,
      onRejected: _onRejected,
      onIncoming: (number) {
        print("Incoming from $number");
      },
    );
    _initPermissionsAndListener();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _initPermissionsAndListener();
    }
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

    if (!mounted) return;
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

  void _onAccepted() {
    // TODO: Revert to default ringtone
    print("debug_call: Call Accepted");
  }

  void _onRejected() {
    // TODO: Change the ringtone
    print("debug_call: Call Rejected");
  }

  Future<void> _initPermissionsAndListener() async {
    final canWrite = await _callStateService.canWriteSettings();
    if (!mounted) return;
    setState(() {
      hasWriteSettingsPermission = canWrite;
    });

    if (!canWrite) {
      return;
    }
    final status = await Permission.phone.request();
    if (!mounted) return;
    if (status.isGranted) {
      setState(() {
        hasPhonePermission = true;
      });
      await _callStateService.startListening();
    } else {
      // TODO: Better error handling and user feedback
      setState(() {
        hasPhonePermission = false;
      });
      print("Permission denied - cannot listen to call states.");
    }
  }

  @override
  Widget build(BuildContext context) {
    return !(hasPhonePermission && hasWriteSettingsPermission)
        ? Scaffold(
            appBar: AppBar(
              backgroundColor: Theme.of(context).colorScheme.inversePrimary,
              title: Text(widget.title),
            ),
            body: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(Icons.warning, size: 64, color: Colors.amber),
                const SizedBox(height: 16),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  child: Text(
                    !hasWriteSettingsPermission
                        ? 'System settings access is required to change the ringtone. Tap below, enable "Allow to modify system settings" for this app, then return and tap again.'
                        : 'Permission to read phone state is required to change ringtones based on call state.',
                    textAlign: TextAlign.center,
                  ),
                ),
                const SizedBox(height: 4),
                ElevatedButton(
                  onPressed: () async {
                    if (!hasWriteSettingsPermission) {
                      await _callStateService.openWriteSettings();
                      return;
                    }
                    await _initPermissionsAndListener();
                  },
                  child: Text(
                    !hasWriteSettingsPermission
                        ? 'Grant System Settings Access'
                        : 'Grant Phone Permission',
                  ),
                ),
              ],
            ),
          )
        : Scaffold(
            appBar: AppBar(
              backgroundColor: Theme.of(context).colorScheme.inversePrimary,
              title: Text(widget.title),
              actions: [
                IconButton(
                  icon: Icon(Icons.settings),
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (context) =>
                            SettingsPage(ringtones: _ringtones),
                      ),
                    );
                  },
                ),
              ],
            ),
            body: ListView.builder(
              itemCount: _ringtones.length,
              itemBuilder: (context, index) {
                return ListTile(
                  title: Text(_ringtones[index].name),
                  onTap: () async {
                    final result = await showMenu(
                      context: context,
                      position: RelativeRect.fromLTRB(100, 100, 0, 0),
                      items: [
                        const PopupMenuItem(
                          value: 'delete',
                          child: Text('Delete'),
                        ),
                        const PopupMenuItem(
                          value: 'move_up',
                          child: Text('Move UP'),
                        ),
                        const PopupMenuItem(
                          value: 'move_down',
                          child: Text('Move DOWN'),
                        ),
                      ],
                    );
                    if (result == 'delete') {
                      setState(() {
                        _ringtones.removeAt(index);
                      });
                      await _saveRingtones();
                    } else if (result == 'move_up') {
                      if (index > 0) {
                        setState(() {
                          final temp = _ringtones[index];
                          _ringtones[index] = _ringtones[index - 1];
                          _ringtones[index - 1] = temp;
                        });
                        await _saveRingtones();
                      }
                    } else if (result == 'move_down') {
                      if (index < _ringtones.length - 1) {
                        setState(() {
                          final temp = _ringtones[index];
                          _ringtones[index] = _ringtones[index + 1];
                          _ringtones[index + 1] = temp;
                        });
                        await _saveRingtones();
                      }
                    }
                  },
                );
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
