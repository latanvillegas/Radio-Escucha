package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class RadioRepository(private val radioDao: RadioDao) {

    val allStations: Flow<List<RadioStation>> = radioDao.getAllStations()

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

    suspend fun checkAndPrepopulate() {
        val current = radioDao.getAllStations().first()
        val hasNewMega = current.any { it.name == "La Mega" && !it.isCustom }
        if (current.isEmpty() || !hasNewMega) {
            radioDao.deleteDefaultStations()
            val defaults = listOf(
                RadioStation(
                    name = "La Mega",
                    url = "https://eu1.lhdserver.es:9007/stream",
                    country = "Venezuela",
                    genre = "Pop / Rock"
                ),
                RadioStation(
                    name = "Disney FM",
                    url = "https://27433.live.streamtheworld.com/DISNEY_PER_LM_SC",
                    country = "Perú",
                    genre = "Infantil"
                ),
                RadioStation(
                    name = "Acqua",
                    url = "https://sonic-us.streaming-chile.com:7006/",
                    country = "Argentina",
                    region = "Mar del Plata"
                ),
                RadioStation(
                    name = "Acqua 100.1",
                    url = "https://strcdn.klm99.com:10983/acquapinamar",
                    country = "Argentina",
                    region = "Villa Gesell"
                ),
                RadioStation(
                    name = "Arceri",
                    url = "https://stream-178.zeno.fm/rmnr2cphyxhvv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiJybW5yMmNwaHl4aHZ2IiwiaG9zdCI6InN0cmVhbS0xNzguemVuby5mbSIsInJ0dGwiOjUsImp0aSI6ImRYMW41R2s4U08yYWx2V0JKcHRDWEEiLCJpYXQiOjE3NjU0MTkxOTEsImV4cCI6MTc2NTQxOTI1MX0.KWxdZmmrrZmvw7-yDyEuMHt6eBzZ2z_OfEElTQVFrPQ",
                    country = "Costa Rica",
                    region = "Aserrín"
                ),
                RadioStation(
                    name = "Actitud",
                    url = "https://radioenhd.com:8088/",
                    country = "México",
                    region = "San Felipe"
                ),
                RadioStation(
                    name = "Actitud 100.9 FM",
                    url = "https://ss.redradios.net:8002/stream",
                    country = "Guatemala",
                    region = "Ciudad de Guatemala"
                ),
                RadioStation(
                    name = "Activa (Buenos Aires)",
                    url = "https://edge01.radiohdvivo.com/fmra1033",
                    country = "Argentina",
                    region = "Buenos Aires"
                ),
                RadioStation(
                    name = "Activa (Colombia)",
                    url = "https://stream-177.zeno.fm/vkb12zqmgzzuv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiJ2a2IxMnpxbWd6enV2IiwiaG9zdCI6InN0cmVhbS0xNzcuemVuby5mbSIsInJ0dGwiOjUsImp0aSI6InVSNGNtcWRqUks2Q1dYb29xY2c5ckEiLCJpYXQiOjE3NjU0MTk2MDEsImV4cCI6MTc2NTQyMTY4MX0.mzmgEUJ2fr9JQ35IKW3sIeU7Ilioh400N-YjigAkiqM",
                    country = "Colombia",
                    region = "Tuquerres"
                ),
                RadioStation(
                    name = "Activa (Salina Cruz)",
                    url = "https://sp3.servidorrprivado.com/6617/",
                    country = "México",
                    region = "Salina Cruz"
                ),
                RadioStation(
                    name = "Activa (Puerto Rico)",
                    url = "https://cast3.asurahosting.com/proxy/univers1/stream",
                    country = "Puerto Rico",
                    region = "San Juan"
                ),
                RadioStation(
                    name = "Activa 100.7",
                    url = "https://sh4.radioonlinehd.com:8533/stream",
                    country = "Argentina",
                    region = "News"
                ),
                RadioStation(
                    name = "Activa 12340 AM",
                    url = "https://ice66.securenetsystems.net/WNVL",
                    country = "EE.UU",
                    region = "Nashville"
                ),
                RadioStation(
                    name = "Activa 88.7",
                    url = "https://streaming.radiosenlinea.com.ar/9088/stream",
                    country = "Argentina",
                    region = "Pocito"
                ),
                RadioStation(
                    name = "Activa 92.5 CL",
                    url = "https://stream-154.zeno.fm/pvs6hqz3crtvv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiJwdnM2aHF6M2NydHZ2IiwiaG9zdCI6InN0cmVhbS0xNTQuemVuby5mbSIsInJ0dGwiOjUsImp0aSI6InpMR0Q1SEJRUWctazNNa3hNN2xVSXciLCJpYXQiOjE3NjU0MjA3NjcsImV4cCI6MTc2NTQyMDgyN30.R4EmPlaA0KfJ41x2S7efsqu98v_gkSg_7t9xV_Qcwvs",
                    country = "Chile",
                    region = "Chile"
                ),
                RadioStation(
                    name = "Activa 95.1 FM Católica",
                    url = "https://sh4.radioonlinehd.com:8557/stream",
                    country = "Guatemala",
                    region = "Guatemala",
                    genre = "Religiosa"
                ),
                RadioStation(
                    name = "Activa 99.7 FM",
                    url = "https://stream-142.zeno.fm/6n7fp9t2pceuv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiI2bjdmcDl0MnBjZXV2IiwiaG9zdCI6InN0cmVhbS0xNDIuemVuby5mbSIsInJ0dGwiOjUsImp0aSI6Ill2OUlFS2N0Um9pZFFaWDRvclJaQkEiLCJpYXQiOjE3NjU0MjA5NDMsImV4cCI6MTc2NTQyMTAwM30.ZOf-uJfL4P364hvHh9raQmkUFQKFLrMpakK8EeSkXPo",
                    country = "Ecuador",
                    region = "Santo Domingo de Los Colorado"
                ),
                RadioStation(
                    name = "Activa El Salvador",
                    url = "https://stream-179.zeno.fm/ar1f9iuyvhgtv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiJhcjFmOWl1eXZoZ3R2IiwiaG9zdCI6InN0cmVhbS0xNzkuemVuby5mbSIsInJ0dGwiOjUsImp0aSI6InUzZHdHWGF4U3hpY01Rc3BQeWVvUmciLCJpYXQiOjE3NjU0MjEwMzgsImV4cCI6MTc2NTQyMTA5OH0.vcaNAVo91j9YcFO6MOW9XX2DMblnYGQ_yRXvm3yMhI0",
                    country = "El Salvador",
                    region = "San Miguel"
                ),
                RadioStation(
                    name = "Activa FM (Chile)",
                    url = "https://27433.live.streamtheworld.com/ACTIVA.mp3",
                    country = "Chile",
                    region = "Santiago Providencia"
                ),
                RadioStation(
                    name = "Activa FM 93.5",
                    url = "https://streaming5.locucionar.com/proxy/activafm?mp=/stream",
                    country = "Argentina",
                    region = "Los Cóndores"
                ),
                RadioStation(
                    name = "Activa FM (Bolivia)",
                    url = "https://stream-179.zeno.fm/007z22fpx38uv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiIwMDd6MjJmcHgzOHV2IiwiaG9zdCI6InN0cmVhbS0xNzkuemVuby5mbSIsInJ0dGwiOjUsImp0aSI6IlBVYnFobWdOUlVHTU80M04zaDBXVWciLCJpYXQiOjE3NjU0MjE0MzUsImV4cCI6MTc2NTQyMTQ5NX0.j7G6_lvz-Dtb5lsSWIYiARLMAtIN5iy7WWKKjLuFnao",
                    country = "Bolivia",
                    region = "La Paz"
                ),
                RadioStation(
                    name = "Activa Jaén",
                    url = "https://sp.onliveperu.com/8108/stream",
                    country = "Perú",
                    region = "Jaén, Cajamarca"
                ),
                RadioStation(
                    name = "Activa La Paz",
                    url = "https://cloudstream2032.conectarhosting.com/9856/stream",
                    country = "Bolivia",
                    region = "La Paz"
                ),
                RadioStation(
                    name = "Activa Táchira Punta FM",
                    url = "https://stream-157.zeno.fm/53vezhmmad0uv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiI1M3ZlemhtbWFkMHV2IiwiaG9zdCI6InN0cmVhbS0xNTcuemVuby5mbSIsInJ0dGwiOjUsImp0aSI6Ii1vUTExOGtSU1dlZG82cHVzZk1JOEEiLCJpYXQiOjE3NjU0MjE3MjEsImV4cCI6MTc2NTQyMTc4MX0.Xqb3C5FezGjrayWw84Zmpn9tkMV0AIz4gcy8X3V152M",
                    country = "Venezuela",
                    region = "San Cristóbal"
                ),
                RadioStation(
                    name = "Radio Moda",
                    url = "https://25023.live.streamtheworld.com/CRP_MOD_SC",
                    country = "Perú",
                    region = "Nacional",
                    genre = "Urbano / Reggaetón"
                ),
                RadioStation(
                    name = "Ritmo Romántica",
                    url = "https://25103.live.streamtheworld.com/CRP_RIT_SC",
                    country = "Perú",
                    region = "Nacional",
                    genre = "Romántica"
                ),
                RadioStation(
                    name = "Onda Cero",
                    url = "https://mdstrm.com/audio/6598b65ab398c90871aff8cc/icecast.audio",
                    country = "Perú",
                    region = "Nacional",
                    genre = "Urbano / Pop"
                ),
                RadioStation(
                    name = "La Zona",
                    url = "https://mdstrm.com/audio/5fada54116646e098d97e6a5/icecast.audio",
                    country = "Perú",
                    region = "Nacional",
                    genre = "Urbano / Pop"
                ),
                RadioStation(
                    name = "Radio Corazón",
                    url = "https://mdstrm.com/audio/5fada514fc16c006bd63370f/icecast.audio",
                    country = "Perú",
                    region = "Nacional",
                    genre = "Baladas / Pop"
                ),
                RadioStation(
                    name = "La Inolvidable",
                    url = "https://playerservices.streamtheworld.com/api/livestream-redirect/CRP_LI_SC",
                    country = "Perú",
                    region = "Nacional",
                    genre = "Del Recuerdo"
                ),
                RadioStation(
                    name = "Radio Mágica",
                    url = "https://26513.live.streamtheworld.com/MAG_AAC_SC",
                    country = "Perú",
                    region = "Nacional",
                    genre = "Retro / Clásicos"
                ),
                RadioStation(
                    name = "Radiomar",
                    url = "https://24873.live.streamtheworld.com/CRP_MARAAC_SC",
                    country = "Perú",
                    region = "Nacional",
                    genre = "Salsa"
                ),
                RadioStation(
                    name = "RPP Noticias",
                    url = "https://mdstrm.com/audio/5fab3416b5f9ef165cfab6e9/icecast.audio",
                    country = "Perú",
                    region = "Nacional",
                    genre = "Noticias"
                ),
                RadioStation(
                    name = "Exitosa Noticias",
                    url = "https://neptuno-2-audio.mediaserver.digital/79525baf-b0f5-4013-a8bd-3c5c293c6561",
                    country = "Perú",
                    region = "Nacional",
                    genre = "Noticias"
                ),
                RadioStation(
                    name = "Radio PBO",
                    url = "https://stream.radiojar.com/2fse67zuv8hvv",
                    country = "Perú",
                    region = "Nacional",
                    genre = "Noticias"
                ),
                RadioStation(
                    name = "Radio Inca",
                    url = "https://stream.zeno.fm/b9x47pyk21zuv",
                    country = "Perú",
                    region = "Nacional",
                    genre = "Huayno / Folklore"
                ),
                RadioStation(
                    name = "Radio ABN",
                    url = "https://jml-stream.com/radio/8000/radio.mp3",
                    country = "Perú",
                    region = "Nacional",
                    genre = "Religiosa"
                ),
                RadioStation(
                    name = "Radio Abba Padre",
                    url = "https://stream-175.zeno.fm/6rrwumthg6quv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiI2cnJ3dW10aGc2cXV2IiwiaG9zdCI6InN0cmVhbS0xNzUuemVuby5mbSIsInJ0dGwiOjUsImp0aSI6Im9XY2g3dmlTU0NHYlVGQ0QtZmNxUFEiLCJpYXQiOjE3NjQ3OTMwNDksImV4cCI6MTc2NDc5MzEwOX0.U3kdYbFm_XjuESzU_aSQ7owwkG9ScWV9h4fLn36I88U",
                    country = "Perú",
                    region = "Nacional",
                    genre = "Religiosa"
                ),
                RadioStation(
                    name = "Radio Turbo Mix",
                    url = "https://serverssl.innovatestream.pe:8080/167.114.118.120:7624/stream",
                    country = "Perú",
                    region = "Regional"
                ),
                RadioStation(
                    name = "Radio Fuego",
                    url = "https://serverssl.innovatestream.pe:8080/sp.onliveperu.com:8128/",
                    country = "Perú",
                    region = "Regional"
                ),
                RadioStation(
                    name = "Radio Andina",
                    url = "https://serverssl.innovatestream.pe:8080/http://167.114.118.120:7058/;stream",
                    country = "Perú",
                    region = "Regional",
                    genre = "Folklore"
                ),
                RadioStation(
                    name = "Radio Ilucan",
                    url = "https://serverssl.innovatestream.pe:8080/167.114.118.120:7820/;stream",
                    country = "Perú",
                    region = "Regional"
                ),
                RadioStation(
                    name = "Radio Santa Lucía",
                    url = "https://sp.dattavolt.com/8014/stream",
                    country = "Perú",
                    region = "Regional"
                ),
                RadioStation(
                    name = "Radio Pampa Yurac",
                    url = "https://rr5200.globalhost1.com/8242/stream",
                    country = "Perú",
                    region = "Regional"
                ),
                RadioStation(
                    name = "Radio Stereo TV",
                    url = "https://sp.onliveperu.com:7048/stream",
                    country = "Perú",
                    region = "Regional"
                ),
                RadioStation(
                    name = "Radio La Kuadra",
                    url = "https://dattavolt.com/8046/stream",
                    country = "Perú",
                    region = "Regional"
                ),
                RadioStation(
                    name = "Radio Frecuencia",
                    url = "https://conectperu.com/8384/stream",
                    country = "Perú",
                    region = "Regional"
                ),
                RadioStation(
                    name = "Onda Popular (Lima)",
                    url = "https://envivo.top:8443/am",
                    country = "Perú",
                    region = "Regional"
                ),
                RadioStation(
                    name = "Onda Popular (Juliaca)",
                    url = "https://dattavolt.com/8278/stream",
                    country = "Perú",
                    region = "Regional"
                ),
                RadioStation(
                    name = "Radio Nor Andina",
                    url = "https://mediastreamm.com/8012/stream/1/",
                    country = "Perú",
                    region = "Regional"
                ),
                RadioStation(
                    name = "Radio Bambamarca",
                    url = "https://envivo.top:8443/lider",
                    country = "Perú",
                    region = "Regional"
                ),
                RadioStation(
                    name = "Radio Continente",
                    url = "https://sonic6.my-servers.org/10170/",
                    country = "Perú",
                    region = "Regional"
                ),
                RadioStation(
                    name = "La Cheverísima",
                    url = "https://sp.onliveperu.com:8114/stream",
                    country = "Perú",
                    region = "Regional",
                    genre = "Salsa / Cumbia"
                ),
                RadioStation(
                    name = "Radio TV El Shaddai",
                    url = "https://stream.zeno.fm/ppr5q4q3x1zuv",
                    country = "Perú",
                    region = "Regional",
                    genre = "Religiosa"
                ),
                RadioStation(
                    name = "Radio Inica Digital",
                    url = "https://stream.zeno.fm/487vgx80yuhvv",
                    country = "Perú",
                    region = "Regional"
                ),
                RadioStation(
                    name = "Radio Activa",
                    url = "https://sp.onliveperu.com:8108/stream",
                    country = "Perú",
                    region = "Regional"
                ),
                RadioStation(
                    name = "Radio Mía",
                    url = "https://streaming.zonalatinaeirl.com:8020/radio",
                    country = "Perú",
                    region = "Regional"
                ),
                RadioStation(
                    name = "Radio Patrón",
                    url = "https://streaming.zonalatinaeirl.com:8010/radio",
                    country = "Perú",
                    region = "Regional"
                ),
                RadioStation(
                    name = "Radio TV Sureña",
                    url = "https://stream.zeno.fm/p7d5fpx4xnhvv",
                    country = "Perú",
                    region = "Regional"
                ),
                RadioStation(
                    name = "Radio Enamorados",
                    url = "https://stream.zeno.fm/gnybbqc1fnruv",
                    country = "Perú",
                    region = "Regional"
                ),
                RadioStation(
                    name = "Radio ABC (San Luis)",
                    url = "https://16643.live.streamtheworld.com/XHCZFM.mp3",
                    country = "México",
                    region = "Norteamérica"
                ),
                RadioStation(
                    name = "Radio ABC (Taxco)",
                    url = "https://streaming.servicioswebmx.com/8288/stream",
                    country = "México",
                    region = "Norteamérica"
                ),
                RadioStation(
                    name = "ABC 760",
                    url = "https://streamingcwsradio30.com/8292/stream",
                    country = "México",
                    region = "Norteamérica"
                ),
                RadioStation(
                    name = "ABC Radio Puebla",
                    url = "https://streaming.servicioswebmx.com/8264/stream",
                    country = "México",
                    region = "Norteamérica"
                ),
                RadioStation(
                    name = "Radio Acceso Total",
                    url = "https://us10a.serverse.com/proxy/acce8712?mp=/",
                    country = "México",
                    region = "Norteamérica"
                ),
                RadioStation(
                    name = "Ach Kuxlejal 100.3",
                    url = "https://stream-178.zeno.fm/md6tfkaaechvv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiJtZDZ0ZmthYWVjaHZ2IiwiaG9zdCI6InN0cmVhbS0xNzguemVuby5mbSIsInJ0dGwiOjUsImp0aSI6IkI4TUMzLXR3UTR1Q1VzbXY2M0gwUFEiLCJpYXQiOjE3NjQ3OTQ4MTksImV4cCI6MTc2NDY5NDg3OX0.xBJOxw_oGdW4sqNsL4n9WyUeK6CTvzAY8o5i5MjLe78",
                    country = "México",
                    region = "Norteamérica"
                ),
                RadioStation(
                    name = "ABC 94.7",
                    url = "https://stream-176.zeno.fm/n03jc4xoy63tv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiJuMDNqYzR4b3k2M3R2IiwiaG9zdCI6InN0cmVhbS0xNzYuemVuby5mbSIsInJ0dGwiOjUsImp0aSI6Im9aa3kyRHRvUXBDU3kwNUF2OGdPX3ciLCJpYXQiOjE3NjQ3OTM2MzksImV4cCI6MTc4NDc5MzY5OX0.clgYVIm4DHZtHwGTjXdSfYi0SjVgGWj8UkiZEBz3yg0",
                    country = "Argentina",
                    region = "Sudamérica"
                ),
                RadioStation(
                    name = "Estéreo Abejorral",
                    url = "https://icecasthd.net/proxy/abejorral/live",
                    country = "Colombia",
                    region = "Sudamérica"
                ),
                RadioStation(
                    name = "Abriendo Surcos",
                    url = "https://djp.sytes.net/public/abriendo_surcos",
                    country = "Colombia",
                    region = "Sudamérica"
                ),
                RadioStation(
                    name = "Acacio de Chile",
                    url = "https://sonic.portalfoxmix.cl:7057/",
                    country = "Chile",
                    region = "Sudamérica"
                ),
                RadioStation(
                    name = "Acción FM",
                    url = "https://stream-intervalohost.com:7008/stream",
                    country = "Venezuela",
                    region = "Sudamérica"
                ),
                RadioStation(
                    name = "Aclo Chuquisaca",
                    url = "https://cloudstream2030.conectarhosting.com/8192/stream",
                    country = "Bolivia",
                    region = "Sudamérica"
                ),
                RadioStation(
                    name = "Aclo Tarija",
                    url = "https://cloudstream2030.conectarhosting.com/8242/stream",
                    country = "Bolivia",
                    region = "Sudamérica"
                ),
                RadioStation(
                    name = "Radio La Hondureña",
                    url = "https://s2.mkservers.space/rih",
                    country = "Honduras",
                    region = "Centroamérica"
                ),
                RadioStation(
                    name = "Abriendo Los Cielos",
                    url = "https://stream-177.zeno.fm/a8uwe88svy8uv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiJhOHV3ZTg4c3Z5OHV2IiwiaG9zdCI6InN0cmVhbS0xNzcuemVuby4mbSIsInJ0dGwiOjUsImp0aSI6Ik9VWVRibTdpUVUtQjVhSHFOWUNyX1EiLCJpYXQiOjE3NjQ3OTQwOTQsImV4cCI6MTc2NDY5NDE1NH0.n3CeLd9U7rcF9A9NsPpzFGJJjuPsUoaf2EsUxGah04w",
                    country = "Honduras",
                    region = "Centroamérica"
                ),
                RadioStation(
                    name = "Una Radio Viva Voz",
                    url = "https://rr5100.globalhost1.com/8006/stream",
                    country = "Nicaragua",
                    region = "Centroamérica"
                ),
                RadioStation(
                    name = "105.3 El Ritmo",
                    url = "https://n02b-e2.revma.ihrhls.com/zc3209/hls.m3u8?rj-ttl=5&rj-tok=AAABmuXcB-4Ad7qhABJqQGGBcg",
                    country = "EE.UU",
                    region = "Norteamérica"
                ),
                RadioStation(
                    name = "Acción Cristiana",
                    url = "https://panel.lifestreammedia.net:8162/stream",
                    country = "EE.UU",
                    region = "Norteamérica",
                    genre = "Religiosa"
                ),
                RadioStation(
                    name = "RFI Internacional",
                    url = "https://rfienespagnol64k.ice.infomaniak.ch/rfienespagnol-64.mp3",
                    country = "Francia",
                    region = "Europa",
                    genre = "Noticias"
                ),
                RadioStation(
                    name = "RFI Español (96k)",
                    url = "https://rfiespagnol96k.ice.infomaniak.ch/rfiespagnol-96k.mp3",
                    country = "Francia",
                    region = "Europa",
                    genre = "Noticias"
                ),
                RadioStation(
                    name = "DW Español",
                    url = "https://dwstream6-lh.akamaihd.net/i/dwstream6_live@123544/master.m3u8",
                    country = "Alemania",
                    region = "Europa",
                    genre = "Noticias"
                ),
                RadioStation(
                    name = "RNE 5 (España)",
                    url = "https://dispatcher.rndfnk.com/crtve/rne5/main/mp3/high?aggregator=tunein",
                    country = "España",
                    region = "Europa",
                    genre = "Noticias"
                ),
                RadioStation(
                    name = "RNE Radio Clásica",
                    url = "https://rnelivestream.rtve.es/rnerc/main/master.m3u8",
                    country = "España",
                    region = "Europa",
                    genre = "Clásica"
                ),
                RadioStation(
                    name = "RNE Radio Nacional",
                    url = "https://f141.rndfnk.com/star/crtve/rne1/nav/mp3/128/ct/stream.mp3?cid=01GENZSPVYG0R84NK9E1C77RSZ&sid=36LhA65FiO252hsvxBqzfqiI4HF&token=-FbGT-8Eif8zgFPSMX7ER3TPiwAZ4pI8BsNKr1HldC4&tvf=HsCHLkTgfRhmMTQxLnJuZGZuay5jb20",
                    country = "España",
                    region = "Europa",
                    genre = "Noticias"
                ),
                RadioStation(
                    name = "Radio AFRONTAR",
                    url = "https://vigo-copesedes-rrcast.flumotion.com/copesedes/vigo-low.mp3",
                    country = "España",
                    region = "Europa"
                ),
                RadioStation(
                    name = "AB 95 FM",
                    url = "https://stream-153.zeno.fm/szskq9dxs98uv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiJzenNrcTlkeHM5OHV2IiwiaG9zdCI6InN0cmVhbS0xNTMuemVuby5mbSIsInJ0dGwiOjUsImp0aSI6Ims0M0xwaVpDVE1pRXExWVhvMEpjUmciLCJpYXQiOjE3NjQ3OTI4NjUsImV4cCI6MTc2NDY5MjkyNX0.dpzu-0oLrJ2nsOJU25J8ghjMS2O_2FSyXzntk4rD05A",
                    country = "España",
                    region = "Europa"
                ),
                RadioStation(
                    name = "Radio Tele Taxi",
                    url = "https://radiott-web.streaming-pro.com:6103/radiott.mp3",
                    country = "España",
                    region = "Europa"
                ),
                RadioStation(
                    name = "Radio ES",
                    url = "https://libertaddigital-radio-live1.flumotion.com/libertaddigital/ld-live1-low.mp3",
                    country = "España",
                    region = "Europa",
                    genre = "Noticias"
                ),
                RadioStation(
                    name = "Cadena COPE",
                    url = "https://net1-cope-rrcast.flumotion.com/cope/net1-low.mp3",
                    country = "España",
                    region = "Europa",
                    genre = "Noticias"
                )
            )
            radioDao.insertStations(defaults)
        }
    }
}
