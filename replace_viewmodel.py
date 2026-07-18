import re

with open('app/src/main/java/com/example/ui/RadioViewModel.kt', 'r') as f:
    content = f.read()

# Replace exoPlayer declaration
content = content.replace("private var exoPlayer: ExoPlayer? = null", """
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
""")

setup_player = """    private fun setupPlayer() {
        val sessionToken = SessionToken(getApplication(), ComponentName(getApplication(), PlaybackService::class.java))
        controllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                try {
                    mediaController = controllerFuture?.get()
                    mediaController?.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            updateStatus()
                        }
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            updateStatus()
                        }
                        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                            updateStatus()
                        }
                        override fun onPlayerError(error: PlaybackException) {
                            _playbackStatus.value = PlaybackStatus.ERROR
                            _errorMessage.value = "Error de red o stream no disponible"
                            _currentStation.value = null
                        }
                    })
                    mediaController?.volume = _volume.value
                } catch (e: Exception) {
                    _errorMessage.value = "No se pudo iniciar el reproductor multimedia."
                }
            },
            MoreExecutors.directExecutor()
        )
    }"""

# Replace setupPlayer function
content = re.sub(r'    private fun setupPlayer\(\) \{.*?\n    \}', setup_player, content, flags=re.DOTALL)

# Replace updateStatus
content = content.replace("val player = exoPlayer ?: return", "val player = mediaController ?: return")

# Replace playStation
content = content.replace("exoPlayer?.let { player ->", "mediaController?.let { player ->")

# Replace togglePlayPause
content = content.replace("val player = exoPlayer ?: return", "val player = mediaController ?: return")

# Replace stopPlayback
content = content.replace("exoPlayer?.stop()", "mediaController?.stop()")

# Replace setVolume
content = content.replace("exoPlayer?.volume = clamped", "mediaController?.volume = clamped")

# Replace onCleared
on_cleared = """    override fun onCleared() {
        super.onCleared()
        cancelSleepTimer()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }"""
content = re.sub(r'    override fun onCleared\(\) \{.*?\}', on_cleared, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/RadioViewModel.kt', 'w') as f:
    f.write(content)

