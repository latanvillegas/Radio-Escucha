import re

with open('app/src/main/java/com/example/ui/RadioViewModel.kt', 'r') as f:
    content = f.read()

# Replace togglePlayPause mediaItem creation
new_play_station = """                val mediaItem = MediaItem.Builder()
                    .setUri(current.url)
                    .setMediaId(current.id.toString())
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(current.name)
                            .setArtist(current.genre)
                            .build()
                    )
                    .build()"""

content = content.replace("val mediaItem = MediaItem.fromUri(current.url)", new_play_station)

with open('app/src/main/java/com/example/ui/RadioViewModel.kt', 'w') as f:
    f.write(content)

