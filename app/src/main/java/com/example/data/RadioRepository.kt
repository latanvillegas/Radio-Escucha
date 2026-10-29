package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class RadioRepository(
    private val radioDao: RadioDao,
    private val playbackHistoryDao: PlaybackHistoryDao
) {
    val allStations: Flow<List<RadioStation>> = radioDao.getAllStations()
    val recentHistory: Flow<List<PlaybackHistory>> = playbackHistoryDao.getRecentHistory()

    suspend fun insert(station: RadioStation): Long {
        return radioDao.insertStation(station)
    }

    suspend fun update(station: RadioStation) {
        radioDao.updateStation(station)
    }

    suspend fun delete(station: RadioStation) {
        radioDao.deleteStation(station)
    }

    suspend fun deleteById(id: Int) {
        radioDao.deleteStationById(id)
    }

    suspend fun getStationById(id: Long): RadioStation? {
        return radioDao.getStationById(id)
    }

    suspend fun insertHistory(station: RadioStation) {
        playbackHistoryDao.recordStationPlay(
            PlaybackHistory(
                stationId = station.id.toLong(),
                stationName = station.name,
                stationGenre = station.genre
            )
        )
    }

    suspend fun clearHistory() {
        playbackHistoryDao.clearHistory()
    }

    suspend fun checkAndPrepopulate() {
        val current = radioDao.getAllStations().first()
        val defaults = getDefaultStations()
        if (current.isEmpty()) {
            radioDao.insertStations(defaults)
            return
        }

        // Remove legacy duplicate placeholders that were replaced by full Cajamarca metadata
        current.filter {
            (it.name == "La Cheverísima" && it.region == "Regional") ||
                (it.name == "Radio Activa" && it.region == "Regional")
        }.forEach { legacy ->
            radioDao.deleteStation(legacy)
        }

        val refreshedCurrent = radioDao.getAllStations().first()
        for (def in defaults) {
            val existing = refreshedCurrent.find {
                it.name.equals(def.name, ignoreCase = true) &&
                    it.province.equals(def.province, ignoreCase = true)
            } ?: refreshedCurrent.find {
                it.name.equals(def.name, ignoreCase = true)
            }

            if (existing == null) {
                radioDao.insertStation(def)
            } else if (
                (existing.faviconUrl.isBlank() && def.faviconUrl.isNotBlank()) ||
                (existing.province.isBlank() && def.province.isNotBlank()) ||
                (existing.district.isBlank() && def.district.isNotBlank())
            ) {
                radioDao.updateStation(
                    existing.copy(
                        url = def.url,
                        genre = def.genre,
                        country = def.country,
                        region = def.region,
                        province = def.province,
                        district = def.district,
                        faviconUrl = def.faviconUrl
                    )
                )
            }
        }
    }

    companion object {
        fun getDefaultStations(): List<RadioStation> {
            return listOf(
                // === Emisoras de Cajamarca, Perú (Catálogo Satelital Web) ===
                RadioStation(
                    id = 0,
                    name = "Radio La Cheverísima 88.3 FM",
                    url = "https://sp.onliveperu.com/8114/stream",
                    genre = "Cumbia / Tropical",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Jaén",
                    district = "Jaén",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/pr_126.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Campesina 760 AM",
                    url = "https://panel.innovatestream.pe:10943/stream",
                    genre = "Música / Noticias",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Cajamarca",
                    district = "Cajamarca",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/campesinaxxx1.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Ciberplus",
                    url = "https://naxradiopanel.com/proxy/radiocib/stream",
                    genre = "Cumbia / Salsa / Reggaetón",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Cajamarca",
                    district = "Cajamarca",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/radio-ciberplus-1.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Coremarca 100.5 FM",
                    url = "https://streaming.servernorte.com/8152/stream",
                    genre = "Cumbia / Folklor / Noticias",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Bambamarca",
                    district = "Bambamarca",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/na112.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio El Edén 96.3 FM",
                    url = "https://stream-176.surfernetwork.com/r3qbtr8fxxhvv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiJyM3FidHI4Znh4aHZ2IiwiaG9zdCI6InN0cmVhbS0xNzYuc3VyZmVybmV0d29yay5jb20iLCJydHRsIjo1LCJqdGkiOiJVZXh0SHQ0QlFTcU5ZanJEUlJfMkRRIiwiaWF0IjoxNzkwMjY5MjQ1LCJleHAiOjE3OTAyNjkzMDV9.9_VuPC7kEdxqLuKMPeMz7_1P9MoGZhutcUL8YsHIFPw",
                    genre = "Noticias / Música",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Celendín",
                    district = "Celendín",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/eledenxxx1.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Internacional del Perú 103.3 FM",
                    url = "https://naxradiopanel.com/proxy/chuquili/stream",
                    genre = "Variada",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "San Pablo",
                    district = "San Pablo",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/internacionalxx1x1.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Ilucán",
                    url = "https://live.radioilucan.com/listen/radio_ilucan/radio.mp3",
                    genre = "Variada / Tropical",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Cutervo",
                    district = "Cutervo",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/ilucan777d84xs.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Onda Popular",
                    url = "https://envivo.top:8443/am",
                    genre = "Informativa / Música",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Bambamarca",
                    district = "Bambamarca",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/radio-onda-popular-bambamarca.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Súper Latina",
                    url = "https://streaming.serviclictecnologia.com/8018/stream",
                    genre = "Cumbia / Tropical / Folklor",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Bambamarca",
                    district = "Bambamarca",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/fm_am_online_531.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio San Francisco",
                    url = "https://cajradiosf.radioca.st/stream",
                    genre = "Rock / Pop 80s 90s",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Cajamarca",
                    district = "Cajamarca",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/radio__san_francisco__cajamarca.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Stereo TV",
                    url = "https://sp.onliveperu.com:7046/;",
                    genre = "Música / Noticias",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Jaén",
                    district = "Jaén",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/radio_stereotv_1432.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Stereo Morán",
                    url = "https://stream.zeno.fm/rytx8fwfny8uv",
                    genre = "Cumbia Sanjuanera / Huayno / Folklor",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Hualgayoc",
                    district = "Morán Lirio",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/Radio__StereoMoran_Cajamarca1_pe.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio La Palma",
                    url = "https://sp.onliveperu.com:7038/;",
                    genre = "Cumbia / Sanjuanera / Tropical",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Cutervo",
                    district = "Sócota",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/comx9.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Activa",
                    url = "https://sp.onliveperu.com/8108/stream",
                    genre = "Cumbia / Sanjuanera / Variada",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Jaén",
                    district = "Jaén",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/comx9.jpg"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Sudamérica",
                    url = "https://sp.onliveperu.com:7078/;",
                    genre = "Cumbia / Folklor / Huayno / Noticias",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Cutervo",
                    district = "Cutervo",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/Radio__Sudamerica_AM_Cutervo_cajamarca.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Visión",
                    url = "https://panel.innovatestream.pe/7024/stream",
                    genre = "Variada / Educativa / Juvenil",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Jaén",
                    district = "Jaén",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/Radio__Vision_FM_Jaen_cajamarca1.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio La Kuadra (Cutervo)",
                    url = "https://dattavolt.com/8046/stream",
                    genre = "Cumbia / Salsa / Reggaetón",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Cutervo",
                    district = "Cutervo",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/pr_com29.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Swing Latino",
                    url = "https://sp.onliveperu.com/8142/stream",
                    genre = "Cumbia / Salsa / Reggaetón / Huayno",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Jaén",
                    district = "Jaén",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/comx12.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Mega Star",
                    url = "https://stream.zeno.fm/kdv9pertarzuv",
                    genre = "Cumbia / Tropical / Sanjuanera",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "San Ignacio",
                    district = "San Ignacio",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/comx13.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Onda Mix",
                    url = "https://sp.onliveperu.com/8126/stream",
                    genre = "Cumbia / Folklor / Huayno / Noticias",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Cajamarca",
                    district = "Cajamarca",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/comx14.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio El Ochentero",
                    url = "https://stream.cajamarcahost.com/8154/;",
                    genre = "Rock 80s / 90s",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Cajamarca",
                    district = "Cajamarca",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/elochentero_1424.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio La Nueva",
                    url = "https://sp.onliveperu.com/8110/stream",
                    genre = "Noticias / Entretenimiento",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Jaén",
                    district = "Jaén",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/radio_lanueva_jaen_envivo.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Virus",
                    url = "https://sp.onliveperu.com:7078/;",
                    genre = "Rock 80s 90s / Cumbia / Variada",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Jaén",
                    district = "Jaén",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/comx16.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Mega Mix",
                    url = "https://sp.onliveperu.com/8120/stream",
                    genre = "Salsa / Cumbia / Reggaetón / Electro",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Jaén",
                    district = "Jaén",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/ds5544ds.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Diario Perú",
                    url = "https://mpc1.mediacp.eu:8112/stream",
                    genre = "Música / Noticias / Actualidad",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Cajamarca",
                    district = "Cajamarca",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/fm_radios_326.png"
                ),
                RadioStation(
                    id = 0,
                    name = "JC Radio",
                    url = "https://stream.zeno.fm/2vzna2c52d0uv",
                    genre = "Cumbia / Tropical / Sanjuanera / Folklor",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Cajamarca",
                    district = "Cajamarca",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/comx19.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Cadena Radio Visión",
                    url = "https://radio.magozoft.com/;stream.mp3",
                    genre = "Religiosa / Evangélica",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Cajamarca",
                    district = "Cajamarca",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/ee216.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Atikam",
                    url = "https://sp.onliveperu.com/8070/stream",
                    genre = "Cumbia / Folklor",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "San Miguel",
                    district = "San Miguel",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/pr267.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Frecuencia Popular (Jaén)",
                    url = "https://sp.onliveperu.com/8122/stream",
                    genre = "Cumbia / Tropical",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Jaén",
                    district = "Jaén",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/ee477.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Mega Stelar",
                    url = "https://sp.dattavolt.com:7080/;",
                    genre = "Cumbia / Reggaetón / Salsa",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Santa Cruz",
                    district = "Santa Cruz",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/pr_com26.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio La Kuadra (Jaén)",
                    url = "https://sp.dattavolt.com/8014/stream",
                    genre = "Cumbia / Reggaetón / Salsa",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Jaén",
                    district = "Jaén",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/pr_com29.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Jelvi",
                    url = "https://sp.onliveperu.com/8040/stream",
                    genre = "Alabanzas / Cultura / Música",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "San Ignacio",
                    district = "San Ignacio",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/pr_com65.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio San Pablo",
                    url = "https://streaming.servernorte.com/8148/stream",
                    genre = "Folklor Cajamarquino / Cumbia",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "San Pablo",
                    district = "San Pablo",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/pr23_13.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Galaxia",
                    url = "https://sp.onliveperu.com:7082/;",
                    genre = "Cumbia / Tropical",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Cutervo",
                    district = "Sócota",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/pr23_174.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Jireh Internacional",
                    url = "https://radiostreaming.pro/8286/stream",
                    genre = "Cristiana / Religiosa",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Cajamarca",
                    district = "Cajamarca",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/fm_am_online_872.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Disco Plus",
                    url = "https://streaming.servernorte.com/8146/stream",
                    genre = "Cumbia / Tropical",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Bambamarca",
                    district = "Bambamarca",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/pr23_349.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Onda Celestial",
                    url = "https://stream.zeno.fm/tinyrerwdtuvv",
                    genre = "Cristiana / Religiosa",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Cajamarca",
                    district = "Cajamarca",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/fm_am_online_624.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio La Caprichosa",
                    url = "https://sp.onliveperu.com/8058/stream",
                    genre = "Cumbia / Tropical / Sanjuanera",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Cajamarca",
                    district = "Cospán",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/fm_am_online_247.webp"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Frecuencia Popular (Bambamarca)",
                    url = "https://stream-149.zeno.fm/bep3fair4lptv",
                    genre = "Cumbia / Tropical / Folklor",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Bambamarca",
                    district = "Bambamarca",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/fm_radios_504.webp"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Santa Lucía",
                    url = "https://sp.dattavolt.com/8014/stream",
                    genre = "Cumbia / Tropical / Folklor",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "Cutervo",
                    district = "La Sacilia",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/fm_radios_118.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Laser",
                    url = "https://stream-150.zeno.fm/5ympr191schvv",
                    genre = "Cumbia / Rock / Salsa / Tropical",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Cajamarca",
                    province = "San Marcos",
                    district = "San Marcos",
                    faviconUrl = "https://www.estacionesfm.com/logos_radio/fm_radios_174.png"
                ),

                // === Emisoras Internacionales y Regionales Adicionales ===
                RadioStation(0, "La Mega", "https://eu1.lhdserver.es:9007/stream", "Pop / Rock", false, true, "Venezuela", "Nacional"),
                RadioStation(0, "Disney FM", "https://27433.live.streamtheworld.com/DISNEY_PER_LM_SC", "Infantil", false, false, "Perú", "Lima"),
                RadioStation(0, "Acqua", "https://sonic-us.streaming-chile.com:7006/", "Variada", false, true, "Argentina", "Mar del Plata"),
                RadioStation(0, "Acqua 100.1", "https://strcdn.klm99.com:10983/acquapinamar", "Variada", false, false, "Argentina", "Villa Gesell"),
                RadioStation(0, "Arceri", "https://stream-178.zeno.fm/rmnr2cphyxhvv", "Variada", false, true, "Costa Rica", "Aserrín"),
                RadioStation(0, "Actitud", "https://radioenhd.com:8088/", "Variada", false, true, "México", "San Felipe"),
                RadioStation(0, "Actitud 100.9 FM", "https://ss.redradios.net:8002/stream", "Variada", false, true, "Guatemala", "Ciudad de Guatemala"),
                RadioStation(0, "Radio Continente", "https://sonic6.my-servers.org/10170/", "Variada", false, true, "Perú", "Regional"),
                RadioStation(0, "Radio TV El Shaddai", "https://stream.zeno.fm/ppr5q4q3x1zuv", "Religiosa", false, true, "Perú", "Regional"),
                RadioStation(0, "Radio Inica Digital", "https://stream.zeno.fm/487vgx80yuhvv", "Variada", false, true, "Perú", "Regional"),
                RadioStation(0, "Radio Mía", "https://streaming.zonalatinaeirl.com:8020/radio", "Variada", false, true, "Perú", "Regional"),
                RadioStation(0, "Radio Patrón", "https://streaming.zonalatinaeirl.com:8010/radio", "Variada", false, true, "Perú", "Regional"),
                RadioStation(0, "Radio TV Sureña", "https://stream.zeno.fm/p7d5fpx4xnhvv", "Variada", false, true, "Perú", "Regional"),
                RadioStation(0, "Radio Enamorados", "https://stream.zeno.fm/gnybbqc1fnruv", "Baladas / Romántica", false, true, "Perú", "Regional"),
                RadioStation(0, "Radio ABC (San Luis)", "https://16643.live.streamtheworld.com/XHCZFM.mp3", "Variada", false, true, "México", "Norteamérica"),
                RadioStation(0, "Radio ABC (Taxco)", "https://streaming.servicioswebmx.com/8288/stream", "Variada", false, true, "México", "Norteamérica"),
                RadioStation(0, "ABC 760", "https://streamingcwsradio30.com/8292/stream", "Variada", false, true, "México", "Norteamérica"),
                RadioStation(0, "ABC Radio Puebla", "https://streaming.servicioswebmx.com/8264/stream", "Variada", false, true, "México", "Norteamérica"),
                RadioStation(0, "Radio Acceso Total", "https://us10a.serverse.com/proxy/acce8712?mp=/", "Variada", false, true, "México", "Norteamérica"),
                RadioStation(0, "Ach Kuxlejal 100.3", "https://stream-178.zeno.fm/md6tfkaaechvv", "Variada", false, true, "México", "Norteamérica"),
                RadioStation(0, "ABC 94.7", "https://stream-176.zeno.fm/n03jc4xoy63tv", "Variada", false, true, "Argentina", "Sudamérica"),
                RadioStation(0, "Estéreo Abejorral", "https://icecasthd.net/proxy/abejorral/live", "Variada", false, true, "Colombia", "Sudamérica"),
                RadioStation(0, "Abriendo Surcos", "https://djp.sytes.net/public/abriendo_surcos", "Variada", false, true, "Colombia", "Sudamérica"),
                RadioStation(0, "Acacio de Chile", "https://sonic.portalfoxmix.cl:7057/", "Variada", false, true, "Chile", "Sudamérica"),
                RadioStation(0, "Acción FM", "https://stream-intervalohost.com:7008/stream", "Variada", false, true, "Venezuela", "Sudamérica"),
                RadioStation(0, "Aclo Chuquisaca", "https://cloudstream2030.conectarhosting.com/8192/stream", "Variada", false, true, "Bolivia", "Sudamérica"),
                RadioStation(0, "Aclo Tarija", "https://cloudstream2030.conectarhosting.com/8242/stream", "Variada", false, true, "Bolivia", "Sudamérica"),
                RadioStation(0, "Radio La Hondureña", "https://s2.mkservers.space/rih", "Variada", false, true, "Honduras", "Centroamérica"),
                RadioStation(0, "Abriendo Los Cielos", "https://stream-177.zeno.fm/a8uwe88svy8uv", "Religiosa", false, true, "Honduras", "Centroamérica"),
                RadioStation(0, "Una Radio Viva Voz", "https://rr5100.globalhost1.com/8006/stream", "Variada", false, true, "Nicaragua", "Centroamérica"),
                RadioStation(0, "105.3 El Ritmo", "https://n02b-e2.revma.ihrhls.com/zc3209/hls.m3u8", "Latina", false, true, "EE.UU", "Norteamérica"),
                RadioStation(0, "Acción Cristiana", "https://panel.lifestreammedia.net:8162/stream", "Religiosa", false, true, "EE.UU", "Norteamérica"),
                RadioStation(0, "RFI Internacional", "https://rfienespagnol64k.ice.infomaniak.ch/rfienespagnol-64.mp3", "Noticias", false, true, "Francia", "Europa"),
                RadioStation(0, "RFI Español (96k)", "https://rfiespagnol96k.ice.infomaniak.ch/rfiespagnol-96k.mp3", "Noticias", false, true, "Francia", "Europa"),
                RadioStation(0, "DW Español", "https://dwstream6-lh.akamaihd.net/i/dwstream6_live@123544/master.m3u8", "Noticias", false, true, "Alemania", "Europa"),
                RadioStation(0, "RNE 5 (España)", "https://dispatcher.rndfnk.com/crtve/rne5/main/mp3/high?aggregator=tunein", "Noticias", false, true, "España", "Europa"),
                RadioStation(0, "RNE Radio Clásica", "https://rnelivestream.rtve.es/rnerc/main/master.m3u8", "Clásica", false, true, "España", "Europa"),
                RadioStation(0, "RNE Radio Nacional", "https://f141.rndfnk.com/star/crtve/rne1/nav/mp3/128/ct/stream.mp3", "Noticias", false, true, "España", "Europa"),
                RadioStation(0, "Radio AFRONTAR", "https://vigo-copesedes-rrcast.flumotion.com/copesedes/vigo-low.mp3", "Variada", false, true, "España", "Europa"),
                RadioStation(0, "AB 95 FM", "https://stream-153.zeno.fm/szskq9dxs98uv", "Variada", false, true, "España", "Europa"),
                RadioStation(0, "Radio Tele Taxi", "https://radiott-web.streaming-pro.com:6103/radiott.mp3", "Variada", false, true, "España", "Europa"),
                RadioStation(0, "Radio ES", "https://libertaddigital-radio-live1.flumotion.com/libertaddigital/ld-live1-low.mp3", "Noticias", false, true, "España", "Europa"),
                RadioStation(0, "Cadena COPE", "https://net1-cope-rrcast.flumotion.com/cope/net1-low.mp3", "Noticias", false, true, "España", "Europa")
            )
        }
    }
}
