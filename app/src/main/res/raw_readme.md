# Raw Resources Directory

This directory contains raw audio files for the PeekDetector app.

## Required Files

### siren.mp3 (or siren.ogg)
A loud, attention-grabbing siren sound for the panic alert feature.

**Requirements:**
- Format: MP3 or OGG (Android-compatible audio format)
- Duration: 2-5 seconds (will be looped)
- Volume: Should be loud and attention-grabbing
- License: Royalty-free for commercial use

**Suggested sources for royalty-free siren sounds:**
- freesound.org
- pixabay.com/sound-effects
- zapsplat.com

**Note:** Place the audio file in this directory and name it `siren.mp3` or `siren.ogg`.
The PanicAlertService will reference this file as `R.raw.siren`.
