<p align="center">
  <img src="docs/brand/readme-header.png" alt="RedoX TV" width="100%">
</p>

# RedoX TV 2 — TV, Tablet ve Telefon için Canlı TV / IPTV Oynatıcı

![Platform](https://img.shields.io/badge/Platform-Android%20TV%20%7C%20Tablet%20%7C%20Telefon-F02848?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-TV%20%26%20Mobil-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Media3](https://img.shields.io/badge/Media3-ExoPlayer-F3C05D?style=for-the-badge)
![Sürüm](https://img.shields.io/badge/S%C3%BCr%C3%BCm-2.1.0-0A0A14?style=for-the-badge)

**[⬇️ APK'ları indir (Releases)](https://github.com/ByReoxXx/RedoX-TV-2/releases)** • [Hata / öneri (Issues)](https://github.com/ByReoxXx/RedoX-TV-2/issues) • [Wiki](wiki/Home.md)

**RedoX TV 2**, eski RedoX TV'nin **ayrı bir uygulama olarak** yeniden tasarlanmış sürümüdür. Aynı sadık arayüz düzeni, yeni kırmızı-altın-lacivert görünüm ve yeni özelliklerle gelir. Üç cihaz sınıfı için **üç ayrı APK** üretir; her biri kendi arayüzüne sahiptir:

| APK | Cihaz | Arayüz | Paket kimliği |
|---|---|---|---|
| `RedoX-TV-2-v2.1.0-tv.apk` | Android TV, Google TV, TV Box | Kumanda (D-Pad) odaklı "lean-back" arayüz | `com.redoxtv.two` |
| `RedoX-TV-2-v2.1.0-tablet.apk` | Tabletler | Gezinti rayı, geniş ızgaralar, yan yana bölünmüş ekran | `com.redoxtv.two.tablet` |
| `RedoX-TV-2-v2.1.0-phone.apk` | Telefonlar | Alt gezinti, mini oynatıcı, hareketler — aynı zamanda **TV kumandası** | `com.redoxtv.two.phone` |

> **Eski RedoX TV ile bağımsızdır.** Paket kimliği, bağlantı şeması (`redoxtv2://`), ağ servisi (`_redoxtv2._tcp`) ve verileri farklıdır; eski uygulamayla **yan yana kurulabilir**, ayarlar ve favoriler paylaşılmaz.

---

## İçindekiler
1. [Ad ve logo](#ad-ve-logo)
2. [Neler var?](#neler-var)
3. [GitHub'da derleme: zip'ten APK'ya](#githubda-derleme-zipten-apkya)
4. [İmza anahtarı (güncellenebilir APK)](#imza-anahtarı-güncellenebilir-apk)
5. [Kurulum](#kurulum)
6. [Dürüstlük notları](#dürüstlük-notları)
7. [Güvenlik](#güvenlik)
8. [Kumanda ve hareket haritası](#kumanda-ve-hareket-haritası)
9. [Proje yapısı](#proje-yapısı)
10. [Wiki](#wiki)

---

## Ad ve logo

**RedoX = Red + Ox** — "kırmızı boğa". Logo, kırmızı faset (low-poly) boğa başı, altın halka, altın burun halkası ve parlayan gözlerden oluşur; "RedoX" kırmızı italik, "TV" altın yazılır.

**Logo tamamen vektördür.** Uygulama simgesi (uyarlanabilir ön plan ve Android 13+ temalı tek renkli simge), bildirim simgesi, Android TV afişi ve uygulama içindeki işaret/yazı birer `VectorDrawable`'dır (`app/src/main/res/drawable/redox_mark.xml`, `redox_wordmark.xml`, `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`, `ic_stat_redox.xml`; TV: `app/src/tv/res/drawable/ic_tv_banner.xml`). PNG yoktur; her ekran yoğunluğunda ve Android sürümünde (üst sürümlerdeki temalı simgeler dahil) keskin kalır. Tek istisna: Android TV ana ekranının başka bir süreçte yüklediği "poster" yedeği (`tv_poster_fallback.png`) bit eşlemdir.

Arayüz paleti doğrudan logodan ölçülmüştür:

| Renk | Değer | Kullanım |
|---|---|---|
| Kızıl (şarap → mercan) | `#590823` `#96112F` `#BD1436` `#F02848` `#FA6465` | Eylemler, canlı rozeti, seçili öğeler; düğmelerde `#F02848` → `#BD1436` gradyanı |
| Altın | `#A17844` `#DAA24F` `#F3C05D` `#FDDE7A` | **Odak halkası**, favori yıldızı, ilerleme çubukları, "TV" yazısı |
| Lacivert-siyah | `#0A0A14` `#161621` `#1F1D2A` | Zemin, kart, yükseltilmiş yüzey (isteğe bağlı OLED modu: saf siyah) |

**Tasarım dili "Kızıl Halka":** logodaki altın halka arayüzün odak göstergesidir. TV'de odaktaki öğe 3 dp altın çerçeve ve yumuşak altın ışımayla belirir; kırmızı yalnızca eylem ve canlı yayın içindir; emoji yerine simge kullanılır; TV'de en küçük yazı 12 sp, telefon ve tablette dokunma hedefleri en az 44 dp'dir.

Ayarlar'dan alternatif paletler (Okyanus, Orman, Mono), OLED modu (saf siyah) ve yazı boyutu seçilebilir; logo her zaman kırmızı-altın kalır. Vektör logo dosyaları (SVG) ve README başlığı görseli: [`docs/brand/`](docs/brand).

---

## Neler var?

### Her üç cihazda
- **Gerçek yayın rehberi (XMLTV) ve hatırlatıcılar.** XMLTV adresi (`.xml` / `.xml.gz`) eklenir; M3U listelerinizdeki rehber adresleri de otomatik kullanılır. Kanallar rehberle `tvg-id` ve ad benzerliğiyle eşleştirilir. *Uydurma program adı gösterilmez:* rehberi olmayan kanalda yalnızca "Canlı yayın" yazar. Programa dokunarak hatırlatma eklersiniz (bildirim; sistem alarmı birkaç dakika sapabilir).
- **Kalite seçici, gerçek istatistikler, gece modu.** Otomatik / 1080p / 720p / 480p / Veri Tasarrufu. Oynatıcıdaki çözünürlük, video bit hızı, ağ tahmini ve düşen kare sayısı **ölçülen** değerlerdir (eskiden tahmini sabitlerdi). Gece modu konuşmaları belirginleştirir, ani yüksek sesleri baskılar.
- **Arama.** Kanal adı, kategori, numara ve **gerçek program adları** içinde; küçük yazım hatalarını tolere eder; sesli arama.
- **Akıllı raflar.** "İzlemeye devam et", "En çok izlediklerin", "Favorilerinde şimdi yayında".
- **Çoklu favori listeleri.** İstediğiniz kadar liste (Çocuklar, Spor…); sıralama, yeniden adlandırma, silme. **Kanal adı / numarası düzenleme** (yalnızca bu cihazda, geri alınabilir).
- **Tema seçenekleri.** Palet, OLED modu (saf siyah), yazı boyutu.
- **Güvenlik ve bakım.** Katı TLS, PIN'li yerel sunucu, güncelleme denetimi, dosyaya yedekleme, yerel tanılama günlüğü ([Güvenlik](#güvenlik)).
- Eski sürümden gelenler: çoklu kaynak ve otomatik yedek akış geçişi, canlı yayında duraklatma, ebeveyn PIN kilidi, kanal gizleme, M3U / Xtream / dosya yükleme, 2'li ve 4'lü bölünmüş ekran, uyku zamanlayıcı.

### Android TV
- **Android TV ana ekranı entegrasyonu:** Favori Kanallar satırı ve "Devam et" (Watch Next) — TV'nizin ana ekranı destekliyorsa.
- **Telefon = kumanda sunucusu:** Telefon/tablet uygulaması TV'yi ağda bulur, ekrandaki 6 haneli kodla eşleşir. Kod yalnızca TV'de görünür.
- QR kodlu web sayfasıyla telefondan M3U / Xtream / yedek gönderimi (kodla korunur).

### Telefon ve tablet
- **Hareketler:** sol kenarda dikey kaydır = parlaklık, sağ kenarda = ses, yatay kaydır = kanal değiştir, çift dokun = duraklat/oynat.
- **Otomatik PiP:** oynatırken ana ekrana çıkınca küçük pencere.
- **Arka planda ses:** bildirim ve kilit ekranı kontrolleriyle (Media3 oturumu). Ayarlardan kapatılabilir; açıkken görüntü kapatılarak veri/pil tasarrufu sağlanır.
- **Telefon = Kumanda:** yön tuşları, ses, kanal, rakamlar, TV'deki kanal listesi; **"TV'de oynat"** ile telefondaki bir kanalı TV'ye gönderme.
- Mobil veri algılanınca (ayara bağlı) otomatik veri tasarrufu kalitesi.
- Tablet: gezinti rayı, çok sütunlu kanal ızgarası, yan yana 2'li / 2×2 bölünmüş ekran.

---

## GitHub'da derleme: zip'ten APK'ya

Bilgisayarınıza Android Studio / JDK / SDK kurmanız **gerekmez**; derleme GitHub Actions'ta yapılır.

1. **Yeni bir GitHub deposu** oluşturun (ör. `RedoX-TV-2`).
2. Bu zip'in içindekileri depoya yükleyin. En sağlam yol:
   ```bash
   git init
   git add .
   git commit -m "RedoX TV 2"
   git branch -M main
   git remote add origin https://github.com/<kullanıcı>/<depo>.git
   git push -u origin main
   ```
   (Web arayüzüyle sürükle-bırak da olur; gizli `.github` klasörünün dahil olduğundan emin olun. Tek seferde en fazla 100 dosya yüklenebildiği için birkaç parçaya bölmeniz gerekebilir.)
3. Depoda **Actions** sekmesine girin. İlk push derlemeyi kendiliğinden başlatır (gerekirse "RedoX TV 2 - APK derle" → *Run workflow*).
4. Bitince çalışmanın sayfasında **Artifacts** altında üç APK görünür: `RedoX-TV-2-tv`, `RedoX-TV-2-tablet`, `RedoX-TV-2-phone`.
5. **Release yayınlamak için:** `git tag v2.1.0 && git push origin v2.1.0` (ya da *Run workflow* → `release` işaretli). APK'lar Releases sayfasına yüklenir; uygulamadaki "güncelleme denetimi" bu sayfaya bakar.

Workflow, `wiki/` klasörünü de GitHub Wiki'ye eşitler (depoda Wiki'yi açıp ilk sayfayı oluşturduysanız).

> Güncelleme denetimi ve kanal paketi senkronu, uygulamanın **kendi** deposuna bakar: GitHub Actions'ta derlerseniz depo adı otomatik gömülür; yerelde derlerseniz `ByReoxXx/RedoX-TV-2` varsayılan olur (`GITHUB_REPOSITORY` ortam değişkeniyle değiştirilebilir).

### Arayüz önizleme (emülatör ekran görüntüleri)

`ui-*` dallarına yaptığınız her push'ta (ya da *Actions → "RedoX TV 2 - Arayüz önizleme" → Run workflow* ile) telefon, tablet ve Android TV emülatörlerinde uygulama açılır, gezilir ve ekran görüntüleri alınır (`ci/capture.sh`). Görüntüler çalışmanın **Artifacts** bölümüne ve deponun `shots-phone` / `shots-tablet` / `shots-tv` dallarına yüklenir. Cihazınız olmadan arayüzü görmenin ve ilk açılışta çökme olup olmadığını denetlemenin en kolay yoludur.

---

## İmza anahtarı (güncellenebilir APK)

Android, bir uygulamanın güncellemesini yalnızca **aynı anahtarla imzalanmışsa** kabul eder. Anahtar tanımlamazsanız CI her derlemede yeni bir debug anahtarı üretir: APK kurulur ama bir önceki derlemenin üzerine **güncellenemez**. Sabit anahtar için bir kez:

```bash
keytool -genkeypair -v -keystore redox-release.keystore -alias redox -keyalg RSA -keysize 2048 -validity 36500
base64 -w0 redox-release.keystore > redox-release.keystore.b64     # macOS: base64 -i redox-release.keystore
```

Depoda **Settings → Secrets and variables → Actions → New repository secret** ile şunları ekleyin:

| Secret | Değer |
|---|---|
| `REDOX_KEYSTORE_BASE64` | `redox-release.keystore.b64` dosyasının içeriği |
| `REDOX_KEYSTORE_PASSWORD` | Anahtar deposu parolası |
| `REDOX_KEY_ALIAS` | `redox` |
| `REDOX_KEY_PASSWORD` | Anahtar parolası |

Keystore dosyasını **asla depoya koymayın** (`.gitignore` zaten engeller) ve yedeğini güvenli yerde saklayın; kaybederseniz güncelleme yayınlayamazsınız.

---

## Kurulum

- **Telefon / tablet:** APK'yı indirip açın; "bilinmeyen kaynaklardan yükleme"ye izin verin.
- **Android TV / Google TV:** APK'yı USB bellekle ya da `adb` ile kurun:
  ```bash
  adb connect <TV-IP>:5555
  adb install -r RedoX-TV-2-v2.1.0-tv.apk
  ```
- İlk açılışta TV'de **Ayarlar → Yayın Rehberi**'nden XMLTV adresi, **M3U ve Dosya**'den kendi listenizi ekleyebilirsiniz. Telefonla kumanda için telefonda **Kumanda** sekmesini açın; TV ekranındaki 6 haneli kodu girin.

---

## Dürüstlük notları

- **Bu sürüm yerel bir derleyici olmadan hazırlandı.** Kodun tamamı, yazan tarafın elindeki araçlarla (sözdizimi, parametre ve kaynak denetimi yapan bir kontrol betiği ve elle inceleme) denetlendi ama **Android derleyicisinden geçirilmedi**. İlk gerçek derleme GitHub Actions'ta olur. Derleme hatası çıkarsa Actions günlüğündeki ilgili satırları iletin; küçük düzeltmelerle çözülür. APK'ların cihazlarda denenmiş olduğunu iddia etmiyoruz.
- **Rehber dürüsttür.** Gerçek XMLTV verisi yoksa arayüz program adı uydurmaz. Eşleşme oranı rehber kaynağınıza ve kanal adlarına bağlıdır.
- **Ağ bit hızı** yalnızca ExoPlayer'ın ölçtüğü tahmindir; kaynağın gerçek kapasitesi değildir.
- **Android TV ana ekranı** entegrasyonu, üreticiye göre farklı davranan sistem sağlayıcısını kullanır; bazı cihazlarda satır görünmeyebilir. Uygulamanın geri kalanını etkilemez.
- **PiP ve arka plan sesi** üreticiye/Android sürümüne göre küçük farklar gösterebilir.
- Eski uygulama (`com.redoxtv.app`) ile aynı imza anahtarını kullanmadığınız sürece onun üzerine güncelleme yapılamaz; zaten bağımsız bir uygulamadır.

---

## Güvenlik

- **Katı TLS (varsayılan).** Eski sürümlerdeki uygulama geneli "her sertifikayı kabul et" ayarı kaldırıldı. Yalnızca **yayın akışları**, bölgesel yayıncıların kendinden imzalı sertifikalarını kabul eden ayrı bir istemci kullanır; liste/rehber indirmeleri, güncelleme denetimi ve kanal senkronu sertifikayı doğrular.
- **Yerel sunucu kimlik doğrulaması.** TV'nin web sayfası ve kumanda API'si aynı ağdaki herkese açık değildir: 6 haneli kodla oturum açılır; yanlış kodla 5 denemeden sonra 60 sn beklenir. Telefon eşleşince rastgele bir anahtar (Bearer) alır; TV'den "Tüm eşleşmeleri kaldır" ile iptal edilir. Kod yalnızca TV'nin ekranında görünür, hiçbir mesajda yazılmaz.
- **Ebeveyn PIN'i** ekranda gösterilmez (maskelenir).
- `android:allowBackup="false"`: kodlar/anahtarlar bulut yedeğine girmez. Yedekleme isteğe bağlı olarak dosyaya yapılır (Android'in dosya seçicisiyle).
- **Tanılama günlüğü** yalnızca cihazdadır; kullanıcı isterse paylaşır. Hiçbir veri otomatik gönderilmez.
- Güncelleme denetimi yalnızca GitHub'daki en son sürüm numarasını okur.

---

## Kumanda ve hareket haritası

**Android TV** (Chromecast kumandası gibi yalnızca yön tuşları, `OK` ve `Geri` olan kumandalarla tam çalışır; Menü, kanal ve rakam tuşları gerekmez)

| Ekran | Tuş | İşlev |
|---|---|---|
| Kanal ızgarası | `OK` / `OK basılı tut` | İzle / odağı üstteki panele (İzle, Favori, Kilit, Düzenle, Gizle) götür; `Geri` karta döndürür |
| Özel M3U | `OK` / `Geri` | Eklediğiniz listenin adındaki klasörü aç / klasör listesine dön (kategori çiplerindeyken) |
| Tüm ekranlar | `Geri` | Bir üst düzeye: listeden kategori çiplerine, çiplerden yan menüye; yan menüde ikinci basış uygulamadan çıkar |
| Oynatıcı | `▲ ▼` | Kanal değiştir |
| Oynatıcı | `◄` / `►` | Kanal çekmecesi / sonraki akış kaynağı |
| Oynatıcı | `OK` | Alt çubuk (ekranın ~%15'i): solda kanal ve program, ortada oynatma düğmeleri (önceki/sonraki kanal, 15 sn geri, oynat/duraklat, canlıya dön), sağda kanal listesi, favori, alternatif yayın ve **Seçenekler** (kalite, ses/altyazı, ölçek, gece modu, küçük pencere, 2×2, kanal no, kanalı gizle, çık) |
| Oynatıcı | `0` | Önceki kanal |
| Rehber | Programa `OK` | Yayındaysa izle; ileriyse hatırlatma ekle/kaldır |
| Favoriler | `►` → `▲/▼` → `OK` | Tut ve taşı |

**Telefon / tablet:** kanala dokun = izle, uzun bas = seçenekler (favori, kilit, gizle, düzenle, TV'de oynat); oynatıcıda hareketler için [Neler var?](#neler-var).

---

## Proje yapısı

```text
app/src/
├── main/    ortak çekirdek: veri, ViewModel, oynatıcı fabrikası, tasarım sistemi, ağ ve özellik merkezi
│   └── java/com/example/chromecasttv/
│       ├── data/      M3U/Xtream/XMLTV ayrıştırıcıları, depo, şifreli kanal paketi
│       ├── feature/   rehber, hatırlatıcı, favori listeleri, istatistik, güncelleme, tanılama
│       ├── network/   katı TLS, yerel sunucu, kumanda protokolü ve istemcisi, keşif (NSD)
│       ├── player/    ExoPlayer fabrikası, kalite / gece modu / ölçülen istatistikler
│       ├── theme/     renk belirteçleri, paletler, logo bileşenleri
│       └── viewmodel/ TvViewModel (üç arayüzün ortak ViewModel'i)
├── tv/      Android TV arayüzü (kumanda odaklı) + TV ana ekranı entegrasyonu
├── mobile/  telefon ve tabletin ortak ekranları, oynatma merkezi, arka plan servisi
├── phone/   telefon iskeleti (alt gezinti, mini oynatıcı)
└── tablet/  tablet iskeleti (gezinti rayı)
```

`app/build.gradle.kts` üç ürün çeşidi (`tv`, `tablet`, `phone`) tanımlar; her biri kendi `RedoXApp` giriş bileşenini sağlar.

---

## Wiki

Ayrıntılı kılavuzlar [`wiki/`](wiki/Home.md) klasöründedir (GitHub Wiki sekmesine otomatik eşitlenir): kurulum, kumanda ve dokunmatik kontroller, M3U/Xtream/QR yükleme, çoklu ekran ve oynatıcı, yayın rehberi, telefon kumandası, güvenlik, SSS ve geliştirici rehberi.

---

## Lisans ve katkı

Açık kaynaklı olarak geliştirilmektedir. Öneri ve hata bildirimleri için **Issues**, katkı için **Pull Requests** bölümlerini kullanabilirsiniz.
