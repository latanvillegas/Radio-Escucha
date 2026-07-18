import re

with open('app/src/main/java/com/example/ui/RadioViewModel.kt', 'r') as f:
    content = f.read()

# Replace playStation mediaItem creation
new_play_station = """                val mediaItem = MediaItem.Builder()
                    .setUri(station.url)
                    .setMediaId(station.id.toString())
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(station.name)
                            .setArtist(station.genre)
                            .build()
                    )
                    .build()"""

content = content.replace("val mediaItem = MediaItem.fromUri(station.url)", new_play_station)

with open('app/src/main/java/com/example/ui/RadioViewModel.kt', 'w') as f:
    f.write(content)

