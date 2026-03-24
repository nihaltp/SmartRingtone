import 'package:flutter/material.dart';
import 'package:ringtone_changer/models/ringtone.dart';

enum RingtoneMode { random, sequential, fixed }

class SettingsPage extends StatefulWidget {
  final List<Ringtone> ringtones;
  const SettingsPage({super.key, required this.ringtones});

  @override
  State<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends State<SettingsPage> {
  RingtoneMode selectedMode = RingtoneMode.random;
  int? fixedRingtoneId;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Settings')),
      body: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Ringtone Selection Mode',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 12),
              Center(
                child: SegmentedButton<RingtoneMode>(
                  segments: const <ButtonSegment<RingtoneMode>>[
                    ButtonSegment<RingtoneMode>(
                      value: RingtoneMode.random,
                      label: Text('Random'),
                      icon: Icon(Icons.shuffle),
                    ),
                    ButtonSegment<RingtoneMode>(
                      value: RingtoneMode.sequential,
                      label: Text('Sequential'),
                      icon: Icon(Icons.low_priority),
                    ),
                    ButtonSegment<RingtoneMode>(
                      value: RingtoneMode.fixed,
                      label: Text('Fixed'),
                      icon: Icon(Icons.lock),
                    ),
                  ],
                  selected: <RingtoneMode>{selectedMode},
                  onSelectionChanged: (Set<RingtoneMode> newSelection) {
                    setState(() {
                      selectedMode = newSelection.first;
                    });
                  },
                  multiSelectionEnabled: false,
                  emptySelectionAllowed: false,
                ),
              ),
              const SizedBox(height: 32),
              AnimatedSwitcher(
                duration: const Duration(milliseconds: 300),
                child: _buildModeSettings(),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildModeSettings() {
    switch (selectedMode) {
      case RingtoneMode.random:
        return const Card(
          key: ValueKey('random'),
          child: ListTile(
            leading: Icon(Icons.info_outline),
            title: Text('Random Mode Active'),
            subtitle: Text(
              'A new ringtone will be picked randomly for every call.',
            ),
          ),
        );
      case RingtoneMode.sequential:
        return const Card(
          key: ValueKey('sequential'),
          child: ListTile(
            leading: Icon(Icons.info_outline),
            title: Text('Sequential Mode Active'),
            subtitle: Text(
              'The app will cycle through your ringtones in order.',
            ),
          ),
        );
      case RingtoneMode.fixed:
        return Column(
          key: const ValueKey('fixed'),
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Card(
              child: ListTile(
                leading: Icon(Icons.info_outline),
                title: Text('Fixed Mode Active'),
                subtitle: Text('Only the selected ringtone will be used.'),
              ),
            ),
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 16.0, horizontal: 8.0),
              child: Text(
                'Choose a ringtone:',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
            ),
            ...widget.ringtones.map(
              (ringtone) => RadioListTile<int>(
                title: Text(ringtone.name),
                value: ringtone.id,
                groupValue: fixedRingtoneId,
                onChanged: (int? value) {
                  setState(() {
                    fixedRingtoneId = value;
                  });
                },
              ),
            ),
            if (widget.ringtones.isEmpty)
              const Padding(
                padding: EdgeInsets.all(8.0),
                child: Text(
                  'No ringtones found. Please add some on the Home page.',
                ),
              ),
          ],
        );
    }
  }
}
