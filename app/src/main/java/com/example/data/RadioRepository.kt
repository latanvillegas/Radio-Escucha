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
        playbackHistoryDao.insertHistoryEntry(
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
        val hasNewMega = current.any { it.name == "La Mega" && it.country == "Venezuela" }
        if (current.isEmpty() || !hasNewMega) {
            val defaults = getDefaultStations()
            radioDao.deleteDefaultStations()
            radioDao.insertStations(defaults)
        }
    }

    companion object {
        fun getDefaultStations(): List<RadioStation> {
            return listOf(
                RadioStation(0, "La Mega", "https://eu1.lhdserver.es:9007/stream", "Pop / Rock", false, true, "Venezuela", "Nacional"),
                RadioStation(0, "Disney FM", "https://27433.live.streamtheworld.com/DISNEY_PER_LM_SC", "Infantil", false, false, "Perú", "Lima"),
                RadioStation(0, "Acqua", "https://sonic-us.streaming-chile.com:7006/", "Variada", false, true, "Argentina", "Mar del Plata"),
                RadioStation(0, "Acqua 100.1", "https://strcdn.klm99.com:10983/acquapinamar", "Variada", false, false, "Argentina", "Villa Gesell"),
                RadioStation(0, "Arceri", "https://stream-178.zeno.fm/rmnr2cphyxhvv", "Variada", false, true, "Costa Rica", "Aserrín"),
                RadioStation(0, "Actitud", "https://radioenhd.com:8088/", "Variada", false, true, "México", "San Felipe"),
                RadioStation(0, "Actitud 100.9 FM", "https://ss.redradios.net:8002/stream", "Variada", false, true, "Guatemala", "Ciudad de Guatemala"),
                RadioStation(0, "Radio Continente", "https://sonic6.my-servers.org/10170/", "Variada", false, true, "Perú", "Regional"),
                RadioStation(0, "La Cheverísima", "https://sp.onliveperu.com:8114/stream", "Salsa / Cumbia", false, true, "Perú", "Regional"),
                RadioStation(0, "Radio TV El Shaddai", "https://stream.zeno.fm/ppr5q4q3x1zuv", "Religiosa", false, true, "Perú", "Regional"),
                RadioStation(0, "Radio Inica Digital", "https://stream.zeno.fm/487vgx80yuhvv", "Variada", false, true, "Perú", "Regional"),
                RadioStation(0, "Radio Activa", "https://sp.onliveperu.com:8108/stream", "Variada", false, true, "Perú", "Regional"),
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
