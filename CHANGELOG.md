# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.1] - 2026-06-05

Commits included in this release:

- 0226213  feat: add confirmation for removing ringtones
- 15f3671  feat: add close buton to log view
- b7e07c2  feat: scan whole call log on first time install

## [1.1.0] - 2026-06-04

Commits included in this release:

- f74b572  fix: release not working due to r8 minification
- 1b014a0  feat: add light theme
- f266656  style(settings): move github card below licences

## [1.0.0] - 2026-06-04

Initial release of Smart Ringtone.

- **Call-Score Escalation**: Automatically adjusts contacts' custom ringtones based on calling habits (missed calls add `+1`, rejected calls add `+2`, answered/outgoing calls reset back to `0`).
- **Ringtone Sequence Customizer**: Add, delete, reorder, and preview custom audio files from the device.
- **Contact Manager & Sorting**: Sort contacts by Name or Call Score (ascending/descending), with a search bar that supports space-separated queries.
- **Privacy/Screenshot Mode**: Setup mock contact profiles (names/numbers) in settings to safely capture clean screenshots.
- **Logging & Diagnostics**: Local diagnostic logging toggle with a built-in log viewer and quick clipboard copies for troubleshooting.
- **Background Call Sync**: Reliable background tracking of incoming and outgoing calls to dynamically manage ringtone scoring.
- **Adaptive Launcher Icon**: Optimized monochrome and adaptive icon configs to support themed launchers (including Oppo/ColorOS dark/grey themes).
- **Batch Progress Notification**: Background notification showing real-time percentage progress during bulk contact updates.
