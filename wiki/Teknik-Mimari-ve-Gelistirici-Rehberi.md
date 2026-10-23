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
│       ├── theme/                   RedoXColors (paletler/AMOLED), TvTheme, RedoXLogo
│       └── viewmodel/TvViewModel.kt üç arayüzün ortak ViewModel'i
├── tv/       TV arayüzü (ekranlar, oynatıcı, ortak bileşenler, TV ana ekranı yayıncısı)
├── mobile/   telefon+tablet: ekranlar, PlaybackHub, PlaybackService, RemoteHub, MobileShell
├── phone/    telefon iskeleti (RedoXApp: alt gezinti + mini oynatıcı)
└── tablet/   tablet iskeleti (RedoXApp: gezinti rayı)
```

## Önemli tasarım kararları

- **Tek ViewModel:** `TvViewModel` kanalları, favorileri, kilitleri, rehberi birleştirip `TvUiState` üretir. Gerçek XMLTV rehberi ve kullanıcının verdiği kanal adı/numarası, `combine` içinde kanallara bindirilir (`FeatureOverlays`). `TvChannel.hasRealEpg` arayüzün "Canlı yayın" ya da gerçek program göstermesini belirler.
- **FeatureHub:** yeni özelliklerin durumunu (`FeatureState`) tek akışta toplar; üç arayüz aynı akışı dinler.
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

GitHub Actions (`.github/workflows/build.yml`): üç çeşit paralel derlenir, APK'lar `RedoX-TV-2-v<sürüm>-<çeşit>.apk` adıyla Artifacts'a yüklenir; `v*` etiketi veya elle tetikleme ile Release oluşur; `wiki/` klasörü GitHub Wiki'ye eşitlenir. `GITHUB_REPOSITORY` otomatik olarak `BuildConfig.GITHUB_REPO`'ya gömülür (güncelleme denetimi ve kanal paketi senkronu uygulamanın kendi deposuna bakar).

## Doğrulama durumu (dürüst not)

Bu sürümün kodu **Android derleyicisi olmadan** yazıldı. Kullanılan denetimler: sınır/delimitör dengesi, projedeki fonksiyonların çağrı parametreleri, import ve kaynak (`R.*`) başvurularının kontrolü için yazılmış bir betik ve elle inceleme. Bunlar derlemenin yerini tutmaz; ilk gerçek derleme GitHub Actions'ta olur ve cihaz testi yapılmamıştır. Derleme hatası çıkarsa Actions günlüğündeki `e: file://…` satırları sorunu gösterir.

## AES-256 şifreli kanal paketi

`channel/channels.enc` şifreli (`REDOX_ENC_V1:`) bir kanal veritabanıdır. Uygulama açılışta `assets/channels.enc` (veya önbellek) paketini çözer; arka planda **kendi deposundaki** `channel/channels.enc` dosyasını katı HTTPS ile kontrol edip güncel listeyi alır (APK güncellemeye gerek kalmaz).
