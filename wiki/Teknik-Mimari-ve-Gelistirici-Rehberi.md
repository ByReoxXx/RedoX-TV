# 🛠️ Teknik Mimari ve Geliştirici Rehberi

## Yığın

Kotlin 2.0.0 • Android Gradle Plugin 8.5.2 • Gradle 8.9 • JDK 17 • Jetpack Compose (BOM 2024.06.00; Material3 1.2.1 ve `androidx.tv:tv-material` 1.0.0) • Media3 1.3.1 (ExoPlayer, HLS, DASH, OkHttp veri kaynağı, MediaSession) • OkHttp 4.12 • Coil 2.6 • `minSdk 28`, `targetSdk 34`.

## Üç APK = üç ürün çeşidi

`app/build.gradle.kts` tek boyutlu (`device`) üç ürün çeşidi tanımlar:

| Çeşit | Kaynak kümeleri | `applicationId` | `BuildConfig.DEVICE_KIND` |
|---|---|---|---|
| `tv` | `main` + `tv` | `com.redoxtv.two` | `"tv"` |
| `tablet` | `main` + `mobile` + `tablet` | `com.redoxtv.two.tablet` | `"tablet"` |
| `phone` | `main` + `mobile` + `phone` | `com.redoxtv.two.phone` | `"phone"` |

Her çeşit kendi `RedoXApp(viewModel, host)` giriş bileşenini sağlar; `MainActivity` (ortak) onu çağırır ve platform işlerini `RedoXHost` arayüzüyle verir (PiP, tam ekran, parlaklık, sesli arama, bildirim izni, paylaşma…). Sınıfların hepsi düz `com.example.chromecasttv` paketindedir (`namespace`); dizin yapısı yalnızca düzen içindir.

```text
app/src/
├── main/    ortak çekirdek
│   └── java/com/example/chromecasttv/
│       ├── MainActivity.kt          Activity + RedoXHost (PiP, immersive, parlaklık, sesli arama…)
│       ├── model/TvModels.kt        TvChannel, EpgProgram, TvUiState, MainNavTab…
│       ├── data/                    TvRepository, M3uAndEpgParser, XmltvEpg, DynamicChannelsJsonService
│       ├── feature/                 FeatureHub (rehber, hatırlatıcı, favori listeleri, istatistik, güncelleme),
│       │                            Reminders, FeatureStores, CrashLogger, UpdateChecker, ChannelExt (raflar, arama)
│       ├── network/                 HttpSecurity, LocalControlServer, LocalTransferWebServer,
│       │                            RemoteProtocol/Client/Discovery
│       ├── player/                  ExoPlayerFactory, PlayerExtras (kalite, gece modu, ölçülen istatistikler)
│       ├── theme/                   RedoXColors (logodan ölçülen palet, AMOLED), TvTheme, RedoXLogo (vektör logo)
│       └── viewmodel/TvViewModel.kt üç arayüzün ortak ViewModel'i
├── tv/       TV arayüzü (ekranlar, oynatıcı, ortak bileşenler, TV ana ekranı yayıncısı)
├── mobile/   telefon+tablet: ekranlar, PlaybackHub, PlaybackService, RemoteHub, MobileShell
├── phone/    telefon iskeleti (RedoXApp: alt gezinti + mini oynatıcı)
└── tablet/   tablet iskeleti (RedoXApp: gezinti rayı)
```

## Önemli tasarım kararları

- **Tek ViewModel:** `TvViewModel` kanalları, favorileri, kilitleri, rehberi birleştirip `TvUiState` üretir. Gerçek XMLTV rehberi ve kullanıcının verdiği kanal adı/numarası, `combine` içinde kanallara bindirilir (`FeatureOverlays`). `TvChannel.hasRealEpg` arayüzün "Canlı yayın" ya da gerçek program göstermesini belirler.
- **FeatureHub:** yeni özelliklerin durumunu (`FeatureState`) tek akışta toplar; üç arayüz aynı akışı dinler.
- **Tasarım dili "Kızıl Halka":** altın halka = odak (`tvSurface`: 3 dp altın çerçeve + ışıma), kırmızı = eylem/canlı yayın, emoji yok, TV'de ≥ 12 sp yazı, mobilde ≥ 44 dp hedef. TV bileşenleri `TvCommonComponents.kt` içindedir: `tvSurface`, `TvIconPill` (odakta etiketi açılır), `TvLiveBadge`, `TvStatPill`, `TvProgressBar`, `TvKeyHint`; mobilde `MobileDesign.kt` (`MChip`, `MPrimaryButton`, `MLiveBadge`, `MChannelRow`, `MChannelCard`).
- **Vektör logo:** `redox_mark.xml` / `redox_wordmark.xml` / `ic_launcher_*` / `ic_stat_redox.xml` / `ic_tv_banner.xml` birer `VectorDrawable`'dır; PNG yoktur. Logodan üretim aracı depoda yoktur (izleme betikleri geliştirme sırasında kullanıldı); çizim doğrudan XML'de düzenlenebilir.
- **Tema:** `RedoXThemeState` (paletler, AMOLED, yazı ölçeği) Compose *snapshot state* olduğundan seçim anında her yere yansır. `TvThemeColors` eski adları yeni belirteçlere bağlayan uyumluluk katmanıdır.
- **Oynatıcı fabrikası:** `createTvExoPlayer` OkHttp veri kaynağını gevşek-TLS'li akış istemcisiyle kurar; uygulamanın geri kalanı katı TLS kullanır (`HttpSecurity`).
- **Mobil oynatma merkezi (`PlaybackHub`):** tek bir ExoPlayer; tam ekran oynatıcı, mini oynatıcı ve `PlaybackService` (Media3 `MediaSessionService`) aynı örneği kullanır. `MobileShell` oynatma sürerken servise bir `MediaController` ile bağlanır; böylece bildirim/kilit ekranı kontrolleri ve ön plan servisi çalışır.
- **Kumanda sunucusu (yalnızca TV):** `LocalControlServer` ham soket üzerinde küçük bir HTTP sunucusudur; web sayfaları kod+çerez, `/api/*` Bearer anahtarı ister. NSD ile `_redoxtv2._tcp.` olarak duyurulur.
- **TV ana ekranı:** `androidx.tvprovider` API'si **yansıma** ile çağrılır; desteklenmeyen cihazlarda adımlar sessizce atlanır.
- **Dürüst veri:** hiçbir yerde uydurma program adı, bit hızı veya istatistik üretilmez.

## Derleme

Yerel (Android Studio / komut satırı):
```bash
gradle :app:assembleTvRelease :app:assembleTabletRelease :app:assemblePhoneRelease
```
Çıktılar: `app/build/outputs/apk/<çeşit>/release/`. İmza: `keystore.properties` (yerel) veya `REDOX_KEYSTORE_FILE`, `REDOX_KEYSTORE_PASSWORD`, `REDOX_KEY_ALIAS`, `REDOX_KEY_PASSWORD` ortam değişkenleri; hiçbiri yoksa debug anahtarı kullanılır.

GitHub Actions (`.github/workflows/build.yml`): üç çeşit paralel derlenir, APK'lar `RedoX-TV-v<sürüm>-<çeşit>.apk` adıyla Artifacts'a yüklenir; `v*` etiketi veya elle tetikleme ile Release oluşur; `wiki/` klasörü GitHub Wiki'ye eşitlenir. `GITHUB_REPOSITORY` otomatik olarak `BuildConfig.GITHUB_REPO`'ya gömülür (güncelleme denetimi ve kanal paketi senkronu uygulamanın kendi deposuna bakar).

`.github/workflows/ui-preview.yml` ("Arayüz önizleme"): `ui-*` dallarına push'ta veya elle çalışır; telefon, tablet ve Android TV emülatörlerinde `ci/capture.sh` ile uygulamayı açar, gezer ve ekran görüntüsü alır (Artifacts + `shots-<tür>` dalları).

## Doğrulama durumu (dürüst not)

Kod GitHub Actions'ta derlenir (üç çeşit); "Arayüz önizleme" iş akışı emülatörde açılışı ve gezintiyi otomatik dener ve ekran görüntüsü üretir. Gerçek TV / telefon / tablet cihazlarında ve gerçek yayınlarla uzun süreli deneme yapılmamıştır. Derleme hatası çıkarsa Actions günlüğündeki `e: file://…` satırları sorunu gösterir.

## AES-256 şifreli kanal paketi

`channel/channels.enc` şifreli (`REDOX_ENC_V1:`) bir kanal veritabanıdır. Uygulama açılışta `assets/channels.enc` (veya önbellek) paketini çözer; arka planda **kendi deposundaki** `channel/channels.enc` dosyasını katı HTTPS ile kontrol edip güncel listeyi alır (APK güncellemeye gerek kalmaz). `assets/channels.enc` ile `channel/channels.enc` aynı dosyanın iki kopyasıdır; birlikte güncellenir.

İçerik, AES-256-CBC ile şifrelenmiş JSON'dur (`{"version","updatedAt","description","channels":[{id, number, name, logoText, c1, c2, cat, badge, logoUrl, sources:[{label,url,type}]}]}`; anahtar = SHA-256(uygulamadaki parola), ilk 16 bayt IV). M3U metni de kabul edilir. Güncel liste (v2.1.0): Türksat'ın resmi kanal/frekans listesindeki **şifresiz (TKGS)** kanallarla eşleşen ve çalışan yayını olan kanallar; aynı kanalın tekrarları tek kanalda alternatif akış olarak birleştirilmiş, yalnızca erişilebilen akışlar bırakılmıştır. Kanal sırası (`number`), Türksat frekans listesi tablosundaki (T4A) satır sırasını izler; tabloda bulunmayan kanallar sona eklenir. Kategori çipleri kanal sırasından bağımsız olarak sabit sıradadır (Ulusal, Haber, Spor, Belgesel, Müzik, Çocuk, Dini, Kıbrıs, Dünya, Yerel). Kanal kimlikleri (`id`) korunur; favoriler ve kilitler bozulmaz.
