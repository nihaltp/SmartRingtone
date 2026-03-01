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
