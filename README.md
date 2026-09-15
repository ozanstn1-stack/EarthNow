# 🌍 Earth Now

**Live Earth Explorer** — Dünyayı gerçek veri katmanlarıyla keşfet.

Gerçek 3D dünya küresi üzerinde; sıcaklık, bulut, rüzgâr, yağış radarı, deniz yüzey sıcaklığı, depremler, aktif orman yangınları, volkanlar ve aurora — hepsi **gerçek, canlı bilimsel veri kaynaklarından**. Bir noktaya dokunun, **"What's happening here?"** ile AI destekli özet alın.

```
Google Earth + Windy + NASA Earth Observatory + AI  →  Earth Now
```

---

## 📦 İçindekiler

1. [Neden MapLibre?](#1-neden-maplibre)
2. [Kurulum & Build](#2-kurulum--build)
3. [Kullanılan API'ler](#3-kullanılan-apiler)
4. [API Key Ekleme](#4-api-key-ekleme)
5. [Hangi API Ücretsiz? / Rate Limits](#5-hangi-api-ücretsiz--rate-limits)
6. [Veri Kaynağı Atıfları](#6-veri-kaynağı-atıfları)
7. [Harita Lisansları](#7-harita-lisansları)
8. [Gizlilik](#8-gizlilik)
9. [AI Yapılandırması](#9-ai-yapılandırması)
10. [Mimari](#10-mimari)
11. [Release APK](#11-release-apk)
12. [Testler](#12-testler)
13. [Hukuki Notlar](#13-hukuki-notlar)

---

## 1. Neden MapLibre?

| Teknoloji | Globe | API Key | Lisans | Neden seçilmedi / seçildi |
|---|---|---|---|---|
| **MapLibre GL Native** | ✅ (v11+) | Gerekmez | BSD-2 (açık kaynak) | ✅ **Seçildi** — gerçek 3D globe (`"projection": "globe"`), clustering, raster/image overlay, Android'de kanıtlanmış, tamamen ücretsiz |
| Cesium | ⚠️ | — | Apache 2 | Native Android SDK yok (web/Unreal odaklı); Android'de gerçekçi entegrasyon yok |
| Mapbox | ✅ | Zorunlu + ücretli planlar | Proprietary | Globe erişimi ücretli; API key zorunlu; cache kuralları kısıtlayıcı |
| Google Maps | ❌ | Zorunlu | Proprietary | Globe görünümü sağlayan halka açık API yok |
| Mapbox GL JS (WebView) | ✅ | Zorunlu | — | WebView üzerinden 3D render, performans ve batarya açısından kötü |

**Sonuç:** MapLibre GL Native `11.11.0` — ücretsiz, açık kaynak, gerçek globe projeksiyonu, hiçbir API key gerektirmez.

---

## 2. Kurulum & Build

**Gereksinimler:**
- Android Studio **Narwhal (2025.1.1) veya üzeri** (veya JDK 17+ ile komut satırı)
- JDK 17+ (AGP 8.9 ile JDK 21 önerilir)
- Android SDK: `compileSdk 36`, `minSdk 26`, `targetSdk 36` (SDK Manager'da "Android 16" platformu)
- Gradle 8.11.1 (wrapper ile birlikte gelir)

```bash
# 1) SDK yolunu belirt
# local.properties (yoksa oluşturun):
#   sdk.dir=C\:\\Users\\<kullanici>\\AppData\\Local\\Android\\Sdk

# 2) Debug APK
./gradlew assembleDebug          # Linux/macOS
gradlew.bat assembleDebug        # Windows

# 3) Unit testler
./gradlew testDebugUnitTest

# 4) UI testler (cihaz/emülatör gerekir)
./gradlew connectedDebugAndroidTest

# 5) Release APK (imzasız)
./gradlew assembleRelease
```

**Sürümler:**
- Kotlin `2.1.20`, AGP `8.9.2`, KSP `2.1.20-1.0.32`
- Jetpack Compose BOM `2025.04.01`, Material 3
- Hilt `2.56.2`, Room `2.7.1`, Retrofit `2.11.0`, Moshi `1.15.2`, WorkManager `2.10.1`, DataStore `1.1.4`
- MapLibre Android SDK `11.11.0`

APK, `app/build/outputs/apk/debug/app-debug.apk` konumuna üretilir (~60 MB, MapLibre native kütüphaneleri dahil).

---

## 3. Kullanılan API'ler

| Katman | API / Endpoint | Ücretsiz | Key Gerekir |
|---|---|---|---|
| 🌡️ Sıcaklık, ☁️ Bulut, 🌬️ Rüzgâr | **Open-Meteo** Forecast — `https://api.open-meteo.com/v1/forecast` (grid + point) | ✅ (ücretsiz, 10.000 çağrı/gün, ticari olmayan) | Hayır |
| 🌊 Deniz yüzeyi sıcaklığı | **Open-Meteo Marine** — `https://marine-api.open-meteo.com/v1/marine` | ✅ | Hayır |
| 🌧️ Yağış radarı | **RainViewer Weather Maps** — `https://api.rainviewer.com/public/weather-maps.json` + tilecache | ✅ (kişisel/eğitim, atıf şart) | Hayır |
| 🌍 Depremler | **USGS** — `https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_day.geojson` | ✅ (public domain) | Hayır |
| 🔥 Orman yangınları | **NASA LANCE FIRMS API v2** — `https://firms.modaps.eosdis.nasa.gov/api/area/csv/{MAP_KEY}/VIIRS_SNPP_NRT/world/1` | ✅ (ücretsiz MAP_KEY) | **Evet** (ücretsiz) |
| 🌋 Volkanlar | **Smithsonian GVP** — Volcanoes of the World veritabanı (uygulamaya gömülü statik katalog) | ✅ | Hayır |
| 🌌 Aurora / uzay havası | **NOAA SWPC** — `https://services.swpc.noaa.gov/json/planetary_k_index_1m.json` + `ovation_aurora_latest.json` | ✅ (public domain) | Hayır |
| 🔍 Arama (şehir/ülke) | **Open-Meteo Geocoding** — `https://geocoding-api.open-meteo.com/v1/search` + Natural Earth ülke GeoJSON'u (gömülü) | ✅ | Hayır |
| 🤖 AI özeti | **OpenAI uyumlu** (Chat Completions) **veya Gemini** — kullanıcı anahtarıyla | — | **Evet** |
| 🗺️ Baz haritalar | CARTO dark (varsayılan), Esri World Imagery (uydu), OpenStreetMap (sokak) | ✅ (atıf şart) | Hayır |

> 🔍 **Not:** FIRMS dışındaki tüm veri kaynakları **hiçbir API key gerektirmez**; uygulama anahtarsız tam çalışır (yangın katmanı "Data unavailable" gösterir).

---

## 4. API Key Ekleme

Anahtarlar **kaynak kodda tutulmaz**; `local.properties` dosyasına eklenir (git'e işlenmez) veya environment variable olarak verilir. Build sırasında `BuildConfig` alanlarına gömülür.

```properties
# local.properties (proje kökü)
sdk.dir=C\:\\Users\\<kullanici>\\AppData\\Local\\Android\\Sdk

# NASA FIRMS (ücretsiz kayıt: https://firms.modaps.eosdis.nasa.gov/api/area )
FIRMS_API_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# AI — ikisinden birini kullanın
AI_OPENAI_API_KEY=sk-...                    # OpenAI veya uyumlu sağlayıcı
AI_BASE_URL=https://api.openai.com          # İsteğe bağlı: herhangi bir OpenAI-uyumlu uç
AI_GEMINI_API_KEY=AIza...                   # https://aistudio.google.com/apikey
```

Environment variable alternatifi: `FIRMS_API_KEY=... ./gradlew assembleDebug` (aynı adlar).

Anahtar girilmezse uygulama **template fallback** ile çalışır: AI çağrısı yerine aynı gerçek verilerden oluşturulmuş, "AI yapılandırılmadı" etiketli deterministik bir özet gösterilir. **Uydurma veri üretilmez.**

---

## 5. Hangi API Ücretsiz? / Rate Limits

| Servis | Limit | Uygulamadaki önlem |
|---|---|---|
| Open-Meteo | 10.000 çağrı/gün (ücretsiz, ticari olmayan); `non-commercial` kuralı | Viewport tabanlı grid istekleri, TTL cache (30 dk), debounce, istek deduplication, batarya tasarrufu modunda daha seyrek/iri grid |
| RainViewer | Kişisel/eğitim kullanımı; önbellekleme önerilir | Frame listesi 10 dk TTL ile cache; tile'lar yalnızca görüntüleme için |
| USGS | Public feed; kısıtlama yok | 10 dk TTL cache; yalnızca aktif katman yenilenir |
| NASA FIRMS | **5000 işlem / 10 dk** (MAP_KEY) | 15 dk TTL cache; günde 1 kez tüm dünya sorgusu (yangınlar "günlük" anlamında) |
| NOAA SWPC | Kısıtlama yok | 15 dk TTL |
| AI (OpenAI/Gemini) | Sizin hesabınız | Sadece kullanıcı tetiklediğinde çağrılır |
| CARTO/Esri/OSM tile | Atıf şartı; tile cache kurallarına uyulur | MapLibre tile cache yalnızca render amaçlı, kalıcı değil |

---

## 6. Veri Kaynağı Atıfları

Uygulama içi **Data sources** ekranı her katmanın kaynağını, güncelleme sıklığını ve son güncelleme zamanını gösterir.

- **Open-Meteo** — CC BY 4.0. Model verileri ulusal hava servislerinden (GFS/ICON vb.).
- **RainViewer** — `https://www.rainviewer.com/` atıfı zorunlu; kişisel/eğitim kullanımı.
- **USGS** — Public domain; atıf: *U.S. Geological Survey, Earthquake Hazards Program*.
- **NASA FIRMS** — Atıf zorunlu: *NASA LANCE FIRMS (MODIS/VIIRS)*; free MAP_KEY.
- **NOAA SWPC** — Public domain; atıf: *NOAA Space Weather Prediction Center*.
- **Smithsonian GVP** — Gömülü volkan kataloğu: *Global Volcanism Program, Volcanoes of the World (v5), Smithsonian Institution, https://volcano.si.edu*. **Statik katalogdur; gerçek zamanlı aktivite akışı DEĞİLDİR** — UI bunu her zaman etiketler ve güncel volkanik aktivite asla uydurulmaz.
- **Natural Earth** — Public domain (ülke sınırları için).

---

## 7. Harita Lisansları

- **Space (varsayılan):** CARTO dark basemap — © OpenStreetMap contributors, © CARTO. Ticari olmayan kullanım için ücretsiz; atıf zorunlu.
- **Satellite:** Esri World Imagery — © Esri, Maxar, Earthstar Geographics. Atıf zorunlu.
- **Streets:** OpenStreetMap raster tile'ları (ODbL).
- Tile'lar **önbelleğe alınmaz/dağıtılmaz** (yalnızca render için MapLibre cache'i). Harita sağlayıcısı görüntülerinin izinsiz kopyalanması/redistribüsyonu yasaktır.

---

## 8. Gizlilik

- Konum izni **isteğe bağlı**dır; uygulama konumsuz tam kullanılabilir.
- Konum verisi **sunucuya gönderilmez** — yalnızca cihazda işlenir (hava durumu sorgusu konuma göre yapılır ama bu bir "konum gönderimi" değildir; diğer veri sağlayıcılarına hiçbir kişisel veri iletilmez).
- AI sağlayıcısına yalnızca **seçili konumun gerçek hava/olay verileri** (üretilmiş yapılandırılmış JSON) gönderilir; kişisel bilgi, reklam kimliği vb. asla gönderilmez.
- Veri ve ayarlar cihazda (Room + DataStore) saklanır.

---

## 9. AI Yapılandırması

`AiSummaryProvider` arayüzü üzerinden sağlayıcı bağımsız çalışır; `AiManager` ayara göre yönlendirir:

- **OpenAI uyumlu** (`OpenAiProvider`) — `AI_BASE_URL` ile herhangi bir OpenAI-uyumlu endpoint (OpenAI, Groq, Ollama vb.) kullanılabilir.
- **Gemini** (`GeminiProvider`) — `AI_GEMINI_API_KEY`.
- **Template** (`TemplateSummaryProvider`) — anahtar yoksa yerel, deterministik, gerçek veri tabanlı özet.

**AI güvenlik kuralları (sistem promptunda zorunlu):**
1. Yalnızca kendisine verilen yapılandırılmış gerçek verileri kullanır; hiçbir şey uydurmaz; eksik alan için "Data unavailable" der.
2. Deprem/volkan/fırtına tehlikesi hakkında **kesin güvenlik tavsiyesi vermez**; "Check your local official authorities and emergency services" ifadesini kullanır.
3. Tahmin/ölçüm belirsizliklerini ifade eder ("may", "estimated", "reported").
4. Özetler 3-6 cümle, sorular kısa yanıt.

Uygulama UI'ında: *"For safety guidance, check local official authorities and emergency services."* her zaman gösterilir.

---

## 10. Mimari

```
app/src/main/java/com/earthnow/app/
├── data/
│   ├── api/          Retrofit arayüzleri (Open-Meteo, USGS, NOAA, RainViewer, FIRMS, AI)
│   ├── remote/dto/   Moshi DTO'ları (gerçek response formatlarıyla birebir)
│   ├── db/           Room: FavoriteLocation, SearchHistory, CacheEntry, WatchRegion
│   ├── prefs/        DataStore tabanlı SettingsRepository
│   └── repository/   Weather, GridWeather (viewport raster), Earthquake, Wildfire,
│                     Volcano, Aurora, Radar, Ocean, Geocoding, LocalData, JsonCache
├── domain/model/     Katman/olay/hava modelleri, birim tercihleri
├── map/              GlobeController (MapLibre globe + katman renderer'ları),
│                     RasterRenderer, DayNightRenderer, GeoJsonBuilder
├── ai/               AiSummaryProvider + OpenAI/Gemini/Template + AiPromptBuilder
├── presentation/     Compose: GlobeScreen, LayerSheet, SearchSheet, TimelineBar,
│                     LocationDetail, Settings, Favorites, Watch, DataSources, Onboarding
├── work/             WorkManager RefreshWorker (6 saatlik, batarya dostu)
├── notification/     Bildirim kanalı + yardımcılar
└── di/               Hilt modülü
```

**Veri doğruluğu ilkeleri:**
- Her katmanın `lastUpdated` zamanı UI'da gösterilir; eski veri hiçbir zaman "canlı" gibi sunulmaz.
- Veri yoksa **"Data unavailable"** denir — asla uydurma değer gösterilmez.
- Offline modda yalnızca önbellekteki son veri, "⚠️ Data from X minutes ago" uyarısıyla gösterilir.
- Raster katmanlar viewport + zoom'a göre uyarlanır (düşük zoom'da iri grid, yakınlaşınca incelir); batarya tasarrufu modunda çözünürlük ve eşzamanlı istek sayısı düşer.
- Olay katmanları MapLibre clustering ile ölçeklenir (dünya → kıta → ülke → bölge → olay).
- Rüzgâr katmanı gerçek u/v grid verisinden animasyonlu akış çizgileri üretir; düşük cihazlarda yoğunluk otomatik azalır.

---

## 11. Release APK

```bash
# 1) İmza dosyası
keytool -genkey -v -keystore release.jks -alias earthnow -keyalg RSA -keysize 2048 -validity 10000

# 2) app/build.gradle.kts içindeki release bloğuna imzayı ekleyin (örnek):
# signingConfigs { release { storeFile=file("release.jks"); storePassword=...; keyAlias="earthnow"; keyPassword=... } }

# 3) Build
./gradlew assembleRelease

# 4) Doğrulama
./gradlew lintRelease
```

Release yapılandırması: R8/ProGuard açık (`proguard-rules.pro`), kaynak küçültme açık, `buildConfig` alanları dahil. APK imzalarken anahtarları kaynak kodda tutmayın — `keystore.properties` veya environment variable kullanın.

**Google Play hazırlığı:** adaptive icon, splash screen (AndroidX Core SplashScreen), versionCode/versionName, ağ güvenlik yapılandırması, izin açıklamaları (konum: isteğe bağlı), gizlilik politikası bu projede hazırdır. Play'e yüklerken gizlilik politikası URL'si ve veri güvenliği formu (konum/uygulama verisi: hayır) doldurulmalıdır.

---

## 12. Testler

**Unit testler** (`app/src/test`):
- API parsing (USGS GeoJSON, FIRMS CSV)
- Veri normalizasyonu ve birim dönüşümleri (°C/°F, km/h/mph/knots, hPa/inHg, km/mi)
- Deprem sıralama (magnitude)
- Katman/renk rampası ve grid cache serialize/deserialize
- Güneş/ay hesapları (terminator, moon phase)
- AI prompt yapısı (yalnızca gerçek veri, uydurma yasağı, null elemanlar)

**UI testler** (`app/src/androidTest`): katman toggle, arama sonuçları, bottom sheet bileşenleri.

```bash
./gradlew testDebugUnitTest          # JVM testleri
./gradlew connectedDebugAndroidTest  # cihaz/emülatör testleri
```

---

## 13. Hukuki Notlar

- **Open-Meteo:** CC BY 4.0; ücretsiz katman yalnızca ticari olmayan kullanım içindir (10k çağrı/gün). Ticari kullanım için Open-Meteo ile iletişime geçin.
- **RainViewer:** Kişisel/eğitim kullanımı; veri sahipleri radarları kaldırabilir; atıf zorunlu.
- **NASA FIRMS:** Veriler NASA EOSDIS'e aittir; atıf zorunlu; MAP_KEY kullanım şartları geçerlidir.
- **USGS/NOAA:** Kamu malı (public domain); atıf rica edilir.
- **Smithsonian GVP:** Katalog kullanımı için atıf zorunlu (DOI: 10.5479/si.GVP.VOTW5-...); ticari kullanım için GVP şartlarını kontrol edin.
- **Natural Earth:** Public domain.
- **CARTO/Esri/OSM:** Basemap atıfları ve tile kullanım şartları; tile'ların yeniden dağıtımı yasaktır.
- Bu proje bir örnek/araçtır; acil durum bilgisi için **yerel resmî otoritelere** başvurun.

---

## Özellik Listesi (MVP)

- [x] 3D Earth globe (MapLibre, globe projeksiyon, atmosfer, gece tarafı)
- [x] Katmanlar: 🌡️ sıcaklık, 🌧️ yağış radarı, ☁️ bulut, 🌬️ rüzgâr animasyonu, 🌊 deniz sıcaklığı, 🔥 yangın (FIRMS), 🌍 deprem (USGS), 🌋 volkan (GVP), 🌌 aurora (NOAA), ☀️/🌙 gündüz-gece
- [x] Zaman çizelgesi: radar geçmiş + nowcast; hava modellerinde -6h / NOW / +6h
- [x] Arama (şehir/ülke/volkan/okyanus), son aramalar
- [x] Nokta seçimi → bottom sheet → "What's happening here?" (AI veya şablon)
- [x] "Ask AI" sohbeti (güvenlik kurallı)
- [x] Detay ekranı (saatlik tahmin, güneş/ay, olay akışı, paylaşım)
- [x] Favoriler, bölge takibi (WorkManager, varsayılan kapalı bildirimler)
- [x] Data sources şeffaflık ekranı, offline/stale uyarıları
- [x] Ayarlar: tema (dark/light/system), birimler, harita stili, yenileme aralığı, batarya tasarrufu, AI, bildirimler
- [x] Room cache + TTL + debounce + dedup; konum isteğe bağlı ve sunucuya gönderilmiyor
- [x] Birim testleri + UI testleri