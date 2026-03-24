import 'package:flutter/material.dart';

enum RingtoneMode { random, sequential, fixed }

class SettingsPage extends StatefulWidget {
  const SettingsPage({super.key});

  @override
  State<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends State<SettingsPage> {
  RingtoneMode selectedMode = RingtoneMode.random;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Settings')),
      body: Padding(
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
              'A new ringtone will be picked randomly from your list for every call.',
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
              'The app will cycle through your ringtones in the order they appear in the list.',
            ),
          ),
        );
      case RingtoneMode.fixed:
        return const Card(
          key: ValueKey('fixed'),
          child: ListTile(
            leading: Icon(Icons.info_outline),
            title: Text('Fixed Mode Active'),
            subtitle: Text('Only the top ringtone in your list will be used.'),
          ),
        );
      case null:
      default:
        return const Center(
          key: ValueKey('none'),
          child: Padding(
            padding: EdgeInsets.only(top: 20),
            child: Text(
              'No mode selected. System default ringtone will be used.',
            ),
          ),
        );
    }
  }
}
