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
          ],
        ),
      ),
    );
  }
}
