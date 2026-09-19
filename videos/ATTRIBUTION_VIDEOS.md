# Video Attribution

Ambient background videos bundled under `androidApp/src/main/assets/videos/`.
All files are user-supplied. Fill in Source / Credit per file before release.

Probe method: `ffprobe -v error -show_entries format=duration,size -show_entries stream=codec_name,width,height -of default=noprint_wrappers=1`
All 14 files probed OK (2026-09-19). All contain a single H.264 video stream, no audio stream.

| Staged file | Codec | Resolution | Duration (s) | Size (bytes) | Source / Credit |
|---|---|---|---|---|---|
| birds1.mp4 | h264 | 540x960 | 9.900000 | 973086 | TODO: add source/credit (user-supplied file) |
| cat1.mp4 | h264 | 540x960 | 18.883333 | 1372172 | TODO: add source/credit (user-supplied file) |
| fire1.mp4 | h264 | 540x960 | 9.000000 | 771851 | TODO: add source/credit (user-supplied file) |
| fire2.mp4 | h264 | 540x960 | 12.866667 | 893678 | TODO: add source/credit (user-supplied file) |
| Night_Ambient.mp4 | h264 | 544x960 | 44.433333 | 2773802 | TODO: add source/credit (user-supplied file) |
| Rain1.mp4 | h264 | 540x960 | 18.000000 | 1692537 | TODO: add source/credit (user-supplied file) |
| Rain2.mp4 | h264 | 540x960 | 31.200000 | 3255262 | TODO: add source/credit (user-supplied file) |
| river1.mp4 | h264 | 540x960 | 10.333333 | 2537191 | TODO: add source/credit (user-supplied file) |
| river2.mp4 | h264 | 540x960 | 9.066667 | 1198232 | TODO: add source/credit (user-supplied file) |
| thunder1.mp4 | h264 | 540x960 | 10.566016 | 641405 | TODO: add source/credit (user-supplied file) |
| wave1.mp4 | h264 | 540x960 | 28.600000 | 5144814 | TODO: add source/credit (user-supplied file) |
| wind1.mp4 | h264 | 540x960 | 7.700000 | 618852 | TODO: add source/credit (user-supplied file) |
| wind2.mp4 | h264 | 540x960 | 8.000000 | 2028884 | TODO: add source/credit (user-supplied file) |
| wind3.mp4 | h264 | 540x960 | 10.733008 | 661734 | TODO: add source/credit (user-supplied file) |

Total: 24563500 bytes (~23.42 MiB / ~24.56 MB) across 14 files.
