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

    private fun normalizeKey(value: String): String {
        val decomposed = java.text.Normalizer.normalize(value.trim().lowercase(), java.text.Normalizer.Form.NFD)
        return decomposed.replace(DIACRITICS_REGEX, "")
    }

    suspend fun checkAndPrepopulate() {
        val current = radioDao.getAllStations().first()
        val defaults = getDefaultStations()
        if (current.isEmpty()) {
            radioDao.insertStations(defaults)
            return
        }

        // Remove deprecated stations ("quitar estas radios") and legacy renamed duplicates
        val removedNamesNormalized = REMOVED_AND_LEGACY_STATION_NAMES.map { normalizeKey(it) }.toSet()
        current.filter { station ->
            normalizeKey(station.name) in removedNamesNormalized ||
                (normalizeKey(station.name) == "la cheverisima" && station.region == "Regional") ||
                (normalizeKey(station.name) == "radio activa" && station.region == "Regional")
        }.forEach { toDelete ->
            radioDao.deleteStation(toDelete)
        }

        val refreshedCurrent = radioDao.getAllStations().first()
        val missingToInsert = mutableListOf<RadioStation>()
        val matchedIds = mutableSetOf<Int>()
        val defaultMatches = mutableMapOf<Int, RadioStation>()

        // Pass 1: Match strictly by normalized name + province, or normalized name first
        // so stations sharing the same stream URL never steal each other's row
        for ((index, def) in defaults.withIndex()) {
            val defNameNorm = normalizeKey(def.name)
            val defProvNorm = normalizeKey(def.province)

            val byName = refreshedCurrent.find {
                it.id !in matchedIds &&
                    normalizeKey(it.name) == defNameNorm &&
                    normalizeKey(it.province) == defProvNorm
            } ?: refreshedCurrent.find {
                it.id !in matchedIds && normalizeKey(it.name) == defNameNorm
            }

            if (byName != null) {
                matchedIds.add(byName.id)
                defaultMatches[index] = byName
            }
        }

        // Pass 2: Fallback URL match for renamed or user-added stations, then update or insert
        for ((index, def) in defaults.withIndex()) {
            val defUrlClean = def.url.trim().lowercase()
            val existing = defaultMatches[index] ?: refreshedCurrent.find {
                it.id !in matchedIds && it.url.trim().lowercase() == defUrlClean
            }

            if (existing == null) {
                missingToInsert.add(def)
            } else {
                matchedIds.add(existing.id)
                if (
                    existing.name != def.name ||
                    existing.url != def.url ||
                    existing.faviconUrl != def.faviconUrl ||
                    existing.genre != def.genre ||
                    existing.country != def.country ||
                    existing.region != def.region ||
                    existing.province != def.province ||
                    existing.district != def.district
                ) {
                    radioDao.updateStation(
                        existing.copy(
                            name = def.name,
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

        // Pass 3: Delete any leftover non-custom stations (duplicates or legacy entries from older APKs)
        // and deduplicate any custom stations with identical (name, url)
        val seenCustomKeys = mutableSetOf<String>()
        for (station in refreshedCurrent) {
            if (!station.isCustom) {
                if (station.id !in matchedIds) {
                    radioDao.deleteStation(station)
                }
            } else {
                val customKey = "${normalizeKey(station.name)}|${station.url.trim().lowercase()}"
                if (!seenCustomKeys.add(customKey)) {
                    radioDao.deleteStation(station)
                }
            }
        }

        if (missingToInsert.isNotEmpty()) {
            radioDao.insertStations(missingToInsert)
        }
    }

    companion object {
        private val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")

        // List of stations explicitly requested to be removed + legacy duplicate names
        val REMOVED_AND_LEGACY_STATION_NAMES = listOf(
            // Explicitly requested to remove ("quitar estas radios")
            "Abriendo Surcos",
            "Radio Abriendo Surcos",
            "Acción Cristiana",
            "Acqua",
            "DW Español",
            "RFI Español (96k)",
            "RFI Español",
            "RNE 5 (España)",
            "RNE 5 España",
            "RNE Radio Clásica",
            "Radio ABC",
            "Radio ABC (San Luis)",
            "Radio ABC (Taxco)",
            "ABC 760",
            "ABC Radio Puebla",
            "ABC 94.7",
            "ABC Radio 760 SOY",
            "Radio Continente",
            "Radio Inica Digital",
            "Radio La Hondureña",
            "Radio Hondureña",
            "Radio Mía",
            "Radio Patrón",
            "Radio TV El Shaddai",
            "Radio Tele Taxi",
            // Legacy names replaced by updated names in the new list
            "Disney FM",
            "Arceri",
            "Actitud",
            "Acción FM",
            "Aclo Tarija",
            "Estéreo Abejorral",
            "Radio Acceso Total",
            "Una Radio Viva Voz",
            "Radio ES",
            "Cadena COPE",
            "105.3 El Ritmo"
        )

        fun getDefaultStations(): List<RadioStation> {
            return listOf(
                // === 1. Emisoras de Cajamarca, Perú (Catálogo Satelital Web) ===
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

                // === 2. Emisoras Nacionales de Perú (Lima y Puno) ===
                RadioStation(
                    id = 0,
                    name = "Radio Corazón",
                    url = "https://mdstrm.com/audio/5fada514fc16c006bd63370f/live.m3u8",
                    genre = "Romántica / Bachata / Latina",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Lima",
                    province = "Lima",
                    district = "Lima",
                    faviconUrl = "https://static2.mytuner.mobi/media/tvos_radios/eLk5CpPL4E.png"
                ),
                RadioStation(
                    id = 0,
                    name = "La Zona",
                    url = "https://mdstrm.com/audio/5fada54116646e098d97e6a5/live.m3u8",
                    genre = "Reggaetón / Trap / Salsa / Bachata",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Lima",
                    province = "Lima",
                    district = "Lima",
                    faviconUrl = "https://cdn.radoxo.com/images/peru/la-zona.webp?v=1793211895"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Disney",
                    url = "https://24283.live.streamtheworld.com/DISNEY_PER_LM_SC",
                    genre = "Reggaetón / Baladas / Romántica",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Lima",
                    province = "Lima",
                    district = "Lima",
                    faviconUrl = "https://md1.rpp-noticias.io/180x180/cutter/2026/02/05/1770305772.jpg"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Enamorados",
                    url = "https://stream.zeno.fm/gnybbqc1fnruv",
                    genre = "Baladas / Romántica",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Lima",
                    province = "Lima",
                    district = "Lima",
                    faviconUrl = "https://play.emisorasperu.com/radio-enamorados/assets/logo-radio-enamorados.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio del Amor",
                    url = "https://stream.zeno.fm/dzed0u64gd0uv",
                    genre = "Romántica / Baladas",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Lima",
                    province = "Lima",
                    district = "Lima",
                    faviconUrl = "https://play.emisorasperu.com/radio-enamorados/images/delamor_cover.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Retro Ritmos Latinos",
                    url = "https://stream.zeno.fm/b10btvx2dxhvv",
                    genre = "Pop Latino / Clásicos",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Lima",
                    province = "Lima",
                    district = "Lima",
                    faviconUrl = "https://latino.radioretroperu.com/assets/logo_web.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Retro Baladas en Inglés",
                    url = "https://stream.zeno.fm/xy5zqsdnnnruv",
                    genre = "Baladas en Inglés / Power Ballads",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Lima",
                    province = "Lima",
                    district = "Lima",
                    faviconUrl = "https://static2.mytuner.mobi/media/tvos_radios/345/radio-retro-baladas-ingles.80a2b296.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Retro Baladas en Español",
                    url = "https://stream.zeno.fm/zre2prvhaxwuv",
                    genre = "Romántica / Baladas del Recuerdo",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Lima",
                    province = "Lima",
                    district = "Lima",
                    faviconUrl = "https://static2.mytuner.mobi/media/tvos_radios/001/radio-retro-baladas-en-espanol.bfd1a8fd.png"
                ),
                RadioStation(
                    id = 0,
                    name = "KPop Love Radio",
                    url = "https://stream.zeno.fm/0gmdtr2rsf9uv",
                    genre = "K-Pop / Romántica / Baladas",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Lima",
                    province = "Lima",
                    district = "Lima",
                    faviconUrl = "https://static2.mytuner.mobi/media/tvos_radios/007/kpop-love-radio.18317783.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Cristianos",
                    url = "https://stream.zeno.fm/erg33shws98uv",
                    genre = "Alabanza y Adoración / Cristiana",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Lima",
                    province = "Lima",
                    district = "Lima",
                    faviconUrl = "https://play.emisorasperu.com/radio-cristianos/assets/cover.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio TV Sureña",
                    url = "https://stream.zeno.fm/p7d5fpx4xnhvv",
                    genre = "Cumbia Sureña / Folklore / Tropical",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Puno",
                    province = "Juliaca",
                    district = "Juliaca",
                    faviconUrl = "https://static.mytuner.mobi/media/radios-150px/347/radio-television-surena.a4ac4dca.jpg"
                ),

                // === 3. Emisoras Internacionales Actualizadas ===
                RadioStation(
                    id = 0,
                    name = "ABC Radio en vivo 760",
                    url = "https://streamingcwsradio30.com/8292/stream",
                    genre = "Noticias / Deportes / Variado",
                    isFavorite = false,
                    isCustom = false,
                    country = "México",
                    region = "Puebla",
                    province = "Puebla",
                    district = "Puebla",
                    faviconUrl = "https://static.mytuner.mobi/media/tvos_radios/111/abc-radio-mexico.b96e68da.png"
                ),
                RadioStation(
                    id = 0,
                    name = "AB 95 FM",
                    url = "https://stream.zenolive.com/szskq9dxs98uv",
                    genre = "Pop / Reggaetón",
                    isFavorite = false,
                    isCustom = false,
                    country = "España",
                    region = "Castilla-La Mancha",
                    province = "La Mancha",
                    district = "Albacete",
                    faviconUrl = "https://static.mytuner.mobi/media/tvos_radios/t9KXQMbxpy.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Abriendo Los Cielos",
                    url = "https://stream-177.zeno.fm/a8uwe88svy8uv",
                    genre = "Religiosa",
                    isFavorite = false,
                    isCustom = false,
                    country = "Honduras",
                    region = "Honduras",
                    province = "Honduras",
                    district = "Honduras",
                    faviconUrl = "https://images.zeno.fm/UrE_ePeN8Hk9iGUZkhDTXSqBpMDYAsDkhyn4M4xEkig/rs:fill:304:304/q:75/g:ce:0:0/aHR0cHM6Ly9wcm94eS56ZW5vLmZtL2NvbnRlbnQvc3RhdGlvbnMvYWd4emZucGxibTh0YzNSaGRITnlNZ3NTQ2tGMWRHaERiR2xsYm5RWWdJQ3dtWi11b2dvTUN4SU9VM1JoZEdsdmJsQnliMlpwYkdVWWdJQ3cyYldRalFvTW9nRUVlbVZ1YncvaW1hZ2UvP3U9MTcxNDA1MDU0MTAwMA"
                ),
                RadioStation(
                    id = 0,
                    name = "Acacio de Chile",
                    url = "https://sonic.portalfoxmix.cl:7057/",
                    genre = "Folklore",
                    isFavorite = false,
                    isCustom = false,
                    country = "Chile",
                    region = "Chile",
                    province = "Chile",
                    district = "Chile",
                    faviconUrl = "https://radioacaciodechile.cl/radio/wp-content/uploads/2019/08/400x400.jpg"
                ),
                RadioStation(
                    id = 0,
                    name = "Acción 100.1 FM",
                    url = "https://stream-151.zeno.fm/4tcnaczn7bluv?zs=cHYcU1_WTzi5v-K0mW2ikA",
                    genre = "Pop / Variada",
                    isFavorite = false,
                    isCustom = false,
                    country = "Venezuela",
                    region = "Maracaibo",
                    province = "Maracaibo",
                    district = "Maracaibo",
                    faviconUrl = "https://static.mytuner.mobi/media/radios-150px/wUXZPQCZNa.jpg"
                ),
                RadioStation(
                    id = 0,
                    name = "Ach Kuxlejal 100.3",
                    url = "https://stream-178.zeno.fm/md6tfkaaechvv",
                    genre = "Religiosa",
                    isFavorite = false,
                    isCustom = false,
                    country = "México",
                    region = "México",
                    province = "México",
                    district = "México",
                    faviconUrl = "https://images.zeno.fm/RJAtYoqbQ5NsaOH0_54mH5p5siY3lKl7qfiQ65Ta2ew/rs:fill:304:304/q:75/g:ce:0:0/aHR0cHM6Ly9wcm94eS56ZW5vLmZtL2NvbnRlbnQvc3RhdGlvbnMvYWd4emZucGxibTh0YzNSaGRITnlNZ3NTQ2tGMWRHaERiR2xsYm5RWWdJRHd6NDNvdVFzTUN4SU9VM1JoZEdsdmJsQnliMlpwYkdVWWdJRHd6NEtTa0FrTW9nRUVlbVZ1YncvaW1hZ2UvP3U9MTc4NzQyMjE4MTAwMA"
                ),
                RadioStation(
                    id = 0,
                    name = "Aclo Chuquisaca",
                    url = "https://cloudstream2030.conectarhosting.com/8192/stream",
                    genre = "Noticias / Música Latina",
                    isFavorite = false,
                    isCustom = false,
                    country = "Bolivia",
                    region = "Sucre",
                    province = "Sucre",
                    district = "Sucre",
                    faviconUrl = "https://cdn.instant.audio/images/logos/radios-com-bo/aclo-potosi.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Aclo Tarija 101.5 FM",
                    url = "https://cloudstream2030.conectarhosting.com/8234/stream",
                    genre = "Crossover / Noticias",
                    isFavorite = false,
                    isCustom = false,
                    country = "Bolivia",
                    region = "Tarija",
                    province = "Tarija",
                    district = "Tarija",
                    faviconUrl = "https://cdn.bolivia.com/canales/aclo-3563-v1.jpg"
                ),
                RadioStation(
                    id = 0,
                    name = "Acqua 100.1",
                    url = "https://strcdn.klm99.com:10983/acquapinamar",
                    genre = "Variada",
                    isFavorite = false,
                    isCustom = false,
                    country = "Argentina",
                    region = "Mar del Plata",
                    province = "Villa Gesell",
                    district = "Villa Gesell",
                    faviconUrl = "https://www.acqua.fm/img/Acqua_logo.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio Actitud 89.7 FM",
                    url = "https://streaming.servicioswebmx.com/8158/stream",
                    genre = "Latino / Pop / Salsa / Religión",
                    isFavorite = false,
                    isCustom = false,
                    country = "México",
                    region = "Tamaulipas",
                    province = "Cd. Victoria",
                    district = "Cd. Victoria",
                    faviconUrl = "https://static2.mytuner.mobi/media/tvos_radios/347/radio-actitud-897-fm.b7186c38.jpg"
                ),
                RadioStation(
                    id = 0,
                    name = "Actitud 100.9 FM",
                    url = "https://ss.redradios.net:8002/stream",
                    genre = "Religiosa",
                    isFavorite = false,
                    isCustom = false,
                    country = "Guatemala",
                    region = "Guatemala",
                    province = "Guatemala",
                    district = "Guatemala",
                    faviconUrl = "https://cdn.instant.audio/images/logos/emisoras-com-gt/actitud-100-9.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Acseri 107.7 FM",
                    url = "https://stream-178.zeno.fm/rmnr2cphyxhvv",
                    genre = "Baladas / Pop / Latino",
                    isFavorite = false,
                    isCustom = false,
                    country = "Costa Rica",
                    region = "San José",
                    province = "Aserrí",
                    district = "Aserrí",
                    faviconUrl = "https://cdn.radoxo.com/images/costa-rica/radio-acseri.webp?v=1791194402"
                ),
                RadioStation(
                    id = 0,
                    name = "COPE Emisión Nacional",
                    url = "https://flucast33-h-cloud.flumotion.com/cope/net1.mp3?referrer_url=https%3A%2F%2Fwww.cope.es%2F&player_type=mobile&domain=www.cope.es",
                    genre = "Noticias",
                    isFavorite = false,
                    isCustom = false,
                    country = "España",
                    region = "Madrid",
                    province = "Madrid",
                    district = "Madrid",
                    faviconUrl = "https://www.cope.es/favicon/cope/track-default-cope.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Estéreo Abejorral 107.4 FM",
                    url = "https://icecasthd.net/proxy/abejorral/live",
                    genre = "Variada",
                    isFavorite = false,
                    isCustom = false,
                    country = "Colombia",
                    region = "Antioquia",
                    province = "Antioquia",
                    district = "Antioquia",
                    faviconUrl = "https://cdn.instant.audio/images/logos/radios-com-co/abejorral-estereo.png"
                ),
                RadioStation(
                    id = 0,
                    name = "La Mega",
                    url = "https://acp4.lorini.net:2050/stream",
                    genre = "Pop / Rock / Latino",
                    isFavorite = false,
                    isCustom = false,
                    country = "Venezuela",
                    region = "Caracas",
                    province = "Caracas",
                    district = "Caracas",
                    faviconUrl = "https://cdn.instant.audio/images/logos/radio-co-ve/mega-caracas.png"
                ),
                RadioStation(
                    id = 0,
                    name = "RFI Internacional",
                    url = "https://rfienespagnol64k.ice.infomaniak.ch/rfienespagnol-64.mp3",
                    genre = "Noticias / Entrevistas",
                    isFavorite = false,
                    isCustom = false,
                    country = "Francia",
                    region = "París",
                    province = "París",
                    district = "París",
                    faviconUrl = "https://s.rfi.fr/media/display/20f7c302-b9e2-11ed-86cc-005056a90321/w:246/p:1x1/carre-1200-DIRECT-Monde%20%281%29.webp"
                ),
                RadioStation(
                    id = 0,
                    name = "RNE Radio Nacional",
                    url = "https://dispatcher.rndfnk.com/crtve/rne5/main/mp3/high?aggregator=tunein",
                    genre = "Noticias",
                    isFavorite = false,
                    isCustom = false,
                    country = "España",
                    region = "Madrid",
                    province = "Madrid",
                    district = "Madrid",
                    faviconUrl = "https://img2.rtve.es/p/1750/imgbackground/?h=320"
                ),
                RadioStation(
                    id = 0,
                    name = "Radio AFRONTAR",
                    url = "https://vigo-copesedes-rrcast.flumotion.com/copesedes/vigo-low.mp3",
                    genre = "Noticias",
                    isFavorite = false,
                    isCustom = false,
                    country = "España",
                    region = "Galicia",
                    province = "Vigo",
                    district = "Vigo",
                    faviconUrl = "https://www.cope.es/favicon/cope/track-default-cope.png"
                ),
                RadioStation(
                    id = 0,
                    name = "Acceso Total",
                    url = "https://us10a.serverse.com/proxy/acce8712?mp=/",
                    genre = "Pop / Reggaetón",
                    isFavorite = false,
                    isCustom = false,
                    country = "México",
                    region = "México",
                    province = "México",
                    district = "México",
                    faviconUrl = "https://static2.mytuner.mobi/media/tvos_radios/113/acceso-total-web-radio.9d209f7f.jpg"
                ),
                RadioStation(
                    id = 0,
                    name = "EsRadio 99.1 FM",
                    url = "https://libertaddigital-radio-live1.flumotion.com/libertaddigital/ld-live1-low.mp3",
                    genre = "Deportes / Noticias",
                    isFavorite = false,
                    isCustom = false,
                    country = "España",
                    region = "Madrid",
                    province = "Madrid",
                    district = "Madrid",
                    faviconUrl = "https://static.mytuner.mobi/media/radios-150px/592/esradio.fab2c4b6.jpg"
                ),
                RadioStation(
                    id = 0,
                    name = "Viva Voz",
                    url = "https://rr5100.globalhost1.com/8006/stream",
                    genre = "Salsa",
                    isFavorite = false,
                    isCustom = false,
                    country = "Nicaragua",
                    region = "Managua",
                    province = "Managua",
                    district = "Managua",
                    faviconUrl = "https://static2.mytuner.mobi/media/tvos_radios/561/vivavoz-radio.e6d93172.png"
                ),
                RadioStation(
                    id = 0,
                    name = "TV Perú (Modo Radio)",
                    url = "https://www.tvperu.gob.pe/play",
                    genre = "TV en Vivo / Noticias / Cultura",
                    isFavorite = false,
                    isCustom = false,
                    country = "Perú",
                    region = "Lima",
                    province = "Lima",
                    district = "Lima",
                    faviconUrl = "https://www.tvperu.gob.pe/sites/default/files/logo_tvperu_0.png"
                )
            )
        }
    }
}
