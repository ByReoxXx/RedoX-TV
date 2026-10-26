<p align="center">
  <img src="docs/brand/readme-header.png" alt="RedoX TV" width="100%">
</p>

<p align="center">
  <img src="docs/brand/readme-hero.jpg" alt="RedoX TV — Android TV, tablet ve telefonda" width="100%">
</p>

<h1 align="center">RedoX TV</h1>

<p align="center">
  <b>Android TV, tablet ve telefon için canlı TV oynatıcı.</b><br>
  Yalnızca yön tuşlarıyla rahat kullanım · tek satırlık oynatıcı çubuğu · hazır, denenmiş kanal listesi · <b>radyo</b>, <b>yayın rehberi ızgarası</b>, <b>radyo alarmı</b> · klavye ve fare desteği · kendi M3U / Xtream listeleriniz klasör klasör.
</p>

<p align="center">
  <a href="https://github.com/ByReoxXx/RedoX-TV-2/releases/latest"><img alt="Sürüm" src="https://img.shields.io/github/v/release/ByReoxXx/RedoX-TV-2?style=for-the-badge&label=S%C3%BCr%C3%BCm&color=F02848"></a>
  <a href="https://github.com/ByReoxXx/RedoX-TV-2/actions/workflows/build.yml"><img alt="Derleme" src="https://img.shields.io/github/actions/workflow/status/ByReoxXx/RedoX-TV-2/build.yml?style=for-the-badge&label=Derleme"></a>
  <img alt="Platform" src="https://img.shields.io/badge/Platform-Android%20TV%20%7C%20Tablet%20%7C%20Telefon-0A0A14?style=for-the-badge&logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white">
  <img alt="Compose" src="https://img.shields.io/badge/Jetpack%20Compose-TV%20%26%20Mobil-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white">
  <img alt="Media3" src="https://img.shields.io/badge/Media3-ExoPlayer-F3C05D?style=for-the-badge">
</p>

<p align="center">
  <a href="https://github.com/ByReoxXx/RedoX-TV-2/releases/latest"><b>⬇️ İndir</b></a> &nbsp;•&nbsp;
  <a href="https://github.com/ByReoxXx/RedoX-TV-2/wiki"><b>📖 Wiki</b></a> &nbsp;•&nbsp;
  <a href="https://github.com/ByReoxXx/RedoX-TV-2/issues">🐞 Hata / öneri</a>
</p>

---

## 🆕 Yenilikler ve ilk RedoX TV'den farklar

Bu depodaki uygulama, [ilk RedoX TV (v1.x)](https://github.com/ByReoxXx/Redox-TV)'nin **ayrı bir uygulama olarak** (paket `com.redoxtv.two`, ilk sürümle yan yana kurulabilir) yeniden tasarlanmış halidir; adı da **RedoX TV**'dir. İlk RedoX TV 361 kanal / 1184 yayınlık geniş bir liste sunar; bu sürüm ise **daha az ama seçilmiş, Türksat sırasında ve tek tek denenmiş** bir liste, yeni bir arayüz dili ve aşağıdaki yeni özellikleri getirir:

| Yeni | Ne yapar |
|---|---|
| 📻 **Radyo sekmesi** | TRT ve ulusal/yerel istasyonlar; TV'de **kadran** arayüzü, telefonda kaydırılan kadran, tablette liste + oynatıcı paneli. Çalan şarkı bilgisi, şarkı geçmişi, nostaljik istasyon arası cızırtı, uyku zamanlayıcı, arka planda ses, kopan yayında otomatik yedek yayın. |
| ⏰ **Radyo alarmı** | Telefon/tablette saat seçin, istasyon seçin; alarm saatinde bildirimle radyo çalar (günlük ya da tek seferlik). |
| 🎧 **TV'yi sessiz izle, radyo çalsın** | Maç sesi yerine radyo yorumcusunu dinlemek ya da müzik dinlerken görüntüyü izlemek için karma mod. |
| 🗓️ **Yayın rehberi ızgarası** | Kanallar × zaman (30 dk'lık sütunlar); yayındakine dokun = izle, ileridekine dokun = hatırlatıcı. Gerçek XMLTV verisi; veri yoksa program adı uydurulmaz. |
| ⌨️🖱️ **Klavye ve fare** | Boşluk/K oynat-duraklat, ok tuşlarıyla sarma ve ses, N/P kanal, M sessiz, C/A altyazı ve ses izi, Ctrl+1…0 sekmeler, Ctrl+F arama; fare tekerleği ses (telefon/tablet) ya da kanal (TV). |
| 📺 **Daha geniş TV listesi** | Kanallar arasında gezinirken büyük afiş ince şeride iner; ekranda ~2 satır daha kanal görünür. |
| 🆕 **Yeni kanal duyurusu ve "Yenilikler"** | Liste güncellenince eklenen kanallar bir kez duyurulur; uygulama güncellenince kısa bir "Yenilikler" penceresi çıkar. |
| 🔏 **Sabit imza anahtarı** | v2.5.1'den itibaren APK'lar aynı anahtarla imzalanır: bir sonraki sürüm **üzerine kurulur**, eskisini silmeniz gerekmez. |
| 🔁 **Alternatif yayın ve kaynak birleştirme** | Aynı kanalın birden çok yayını tek kanalda; açılmazsa sıradakine otomatik geçer (en çok 8 kaynak). |

İlk sürümden gelenler (bölünmüş ekran, QR ile M3U/Xtream yükleme, PIN kilidi, favori listeleri, telefonu kumanda yapma, EPG) burada da vardır.

---

## ✨ Neden RedoX TV?

<table>
  <tr>
    <td width="33%" valign="top">
      <b>🎮 Kumandaya göre</b><br>
      Chromecast with Google TV kumandası gibi yalnızca yön tuşları, <code>OK</code> ve <code>Geri</code> olan kumandalarla her şey yapılır. Menü, kanal ve rakam tuşu gerekmez.
    </td>
    <td width="33%" valign="top">
      <b>🎛️ Tek satırlık oynatıcı çubuğu</b><br>
      Kanal bilgisi, oynatma ve kısayollar ekranın yalnızca ~%15'inde. Kalite, ses, altyazı, ölçek, gece modu gibi ayarlar <b>Seçenekler</b> menüsünde.
    </td>
    <td width="33%" valign="top">
      <b>📚 Hazır kanal listesi</b><br>
      Türksat'ın şifresiz (TKGS) kanallarıyla eşleşen <b>133 kanal / 417 yayın</b>, tek tek denenmiş. Aynı kanalın alternatif yayınları tek kanalda; liste açılışta kendiliğinden güncellenir.
    </td>
  </tr>
  <tr>
    <td valign="top">
      <b>📁 Özel M3U klasörleri</b><br>
      Eklediğiniz her liste, verdiğiniz adla bir klasör olur. M3U adresi, dosya, Xtream Codes ya da telefondan QR ile ekleyin.
    </td>
    <td valign="top">
      <b>📲 Telefon = kumanda</b><br>
      Telefon TV'yi ağda bulur, ekrandaki 6 haneli kodla eşleşir; yön tuşları, ses, rakamlar ve <b>"TV'de oynat"</b> telefonda.
    </td>
    <td valign="top">
      <b>🌙 Hafif ve gözü yormayan</b><br>
      Sade efektler, OLED modu (saf siyah), altın odak halkası. Zayıf TV işlemcilerinde de akıcı kalması için sonsuz animasyon yok.
    </td>
  </tr>
</table>

---

## 🖼️ Ekran görüntüleri

> Görüntüler gerçek uygulamadan, GitHub Actions'taki otomatik emülatör turuyla alınır (`ci/capture.sh`). **Kanal adları, logolar ve istasyonlar uydurma bir örnek listedir** (`ci/make_demo_channels.py`); gerçek bir kanalın adı ya da logosu görüntülerde yer almaz. Oynatıcı görüntülerinde telifsiz demo yayınlar kullanılır: *Big Buck Bunny* ve *Tears of Steel* © Blender Foundation, [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/).

### 📺 Android TV

<table>
  <tr>
    <td width="50%"><img src="docs/screenshots/tv/02-canli-tv.jpg" alt="Canlı TV"><br><sub><b>Canlı TV</b> — ince afiş şeridi, kategori çipleri; ekranda daha çok kanal (örnek liste)</sub></td>
    <td width="50%"><img src="docs/screenshots/tv/03-kanal-ozellikleri.jpg" alt="Kanal özellikleri"><br><sub><b>Kanal özellikleri</b> — <code>OK</code> basılı tut: izle, favori, kilit, düzenle, gizle</sub></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/tv/08-oynatici-kontroller.jpg" alt="Oynatıcı çubuğu"><br><sub><b>Oynatıcı çubuğu</b> — kanal, oynatma düğmeleri ve kısayollar tek satırda</sub></td>
    <td><img src="docs/screenshots/tv/09-oynatici-secenekler.jpg" alt="Seçenekler menüsü"><br><sub><b>Seçenekler</b> — kalite, ses, altyazı, ölçek, gece modu, PiP, 2×2…</sub></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/tv/07-oynatici-bilgi.jpg" alt="Kanal değişince"><br><sub><b>Kanal değişince</b> — çubuk ve kısayol ipuçları kısa süre görünür</sub></td>
    <td><img src="docs/screenshots/tv/10-kanal-cekmecesi.jpg" alt="Hızlı kanal çekmecesi"><br><sub><b>Hızlı kanal listesi</b> — oynatıcıda <code>◄</code></sub></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/tv/05-ozel-m3u-klasorler.jpg" alt="Özel M3U klasörleri"><br><sub><b>Özel M3U</b> — her liste, verdiğiniz adla bir klasör</sub></td>
    <td><img src="docs/screenshots/tv/16-radyo-kadran.jpg" alt="Radyo kadranı"><br><sub><b>Radyo</b> — kadran, çalan şarkı, ses dalgası</sub></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/tv/12-m3u-ve-dosya.jpg" alt="M3U ve Dosya"><br><sub><b>M3U ve Dosya</b> — dosya, Xtream, QR ile telefondan ekleme</sub></td>
    <td><img src="docs/screenshots/tv/13-ayarlar.jpg" alt="Ayarlar"><br><sub><b>Ayarlar</b> — palet, OLED modu, yazı boyutu, kalite, gece modu</sub></td>
  </tr>
</table>

### 📱 Telefon

<table>
  <tr>
    <td width="20%"><img src="docs/screenshots/phone/01-ana-sayfa.jpg" alt="Ana sayfa"><br><sub><b>Ana sayfa</b></sub></td>
    <td width="20%"><img src="docs/screenshots/phone/03-klasor.jpg" alt="Canlı TV ve klasörler"><br><sub><b>Canlı TV</b> + klasör çipleri</sub></td>
    <td width="20%"><img src="docs/screenshots/phone/05-oynatici-kontroller.jpg" alt="Oynatıcı"><br><sub><b>Oynatıcı</b></sub></td>
    <td width="20%"><img src="docs/screenshots/phone/07-favoriler.jpg" alt="Favoriler"><br><sub><b>Favoriler</b></sub></td>
    <td width="20%"><img src="docs/screenshots/phone/08-kumanda.jpg" alt="Kumanda"><br><sub><b>TV kumandası</b></sub></td>
  </tr>
</table>

### 📻 Radyo (telefon)

<table>
  <tr>
    <td width="25%"><img src="docs/screenshots/phone/11-radyo-liste.jpg" alt="Radyo listesi"><br><sub><b>Radyo</b> — istasyonlar, favoriler, arama</sub></td>
    <td width="25%"><img src="docs/screenshots/phone/12-radyo-oynatici.jpg" alt="Radyo oynatıcı"><br><sub><b>Kadran</b> — çalan şarkı, ses dalgası</sub></td>
    <td width="25%"><img src="docs/screenshots/phone/13-radyo-secenekler.jpg" alt="Radyo seçenekleri"><br><sub><b>Seçenekler</b> — alarm, TV'yi sessiz izle, geçmiş</sub></td>
    <td width="25%"><img src="docs/screenshots/phone/07-favoriler.jpg" alt="Favoriler"><br><sub><b>Favoriler</b> — sıralanabilir listeler</sub></td>
  </tr>
</table>

### 💻 Tablet

<table>
  <tr>
    <td width="50%"><img src="docs/screenshots/tablet/02-canli-tv.jpg" alt="Tablet Canlı TV"><br><sub><b>Canlı TV</b> — gezinti rayı, çok sütunlu ızgara</sub></td>
    <td width="50%"><img src="docs/screenshots/tablet/05-oynatici-kontroller.jpg" alt="Tablet oynatıcı"><br><sub><b>Oynatıcı</b> — dokun: kontroller, çift dokun: duraklat</sub></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/tablet/11-radyo.jpg" alt="Tablet radyo"><br><sub><b>Radyo</b> — solda liste, sağda kadranlı oynatıcı paneli</sub></td>
    <td><img src="docs/screenshots/tablet/07-favoriler.jpg" alt="Tablet favoriler"><br><sub><b>Favoriler</b> — sıralanabilir listeler</sub></td>
  </tr>
</table>

Tüm görüntüler ve ayrıntılar: [📸 Wiki → Ekran Görüntüleri](https://github.com/ByReoxXx/RedoX-TV-2/wiki/Ekran-Goruntuleri).

---

## 🚀 Hızlı başlangıç

**1. İndirin.** [Releases](https://github.com/ByReoxXx/RedoX-TV-2/releases/latest) sayfasından cihazınıza uygun APK'yı alın:

| APK | Cihaz | Arayüz |
|---|---|---|
| `RedoX-TV-v2.5.2-tv.apk` | Android TV, Google TV, TV Box | Kumanda (D-Pad) odaklı "lean-back" arayüz |
| `RedoX-TV-v2.5.2-tablet.apk` | Tabletler | Gezinti rayı, geniş ızgaralar, yan yana bölünmüş ekran |
| `RedoX-TV-v2.5.2-phone.apk` | Telefonlar | Alt gezinti, mini oynatıcı, hareketler — aynı zamanda **TV kumandası** |

**2. Kurun.** Telefon/tablette APK'ya dokunun ("bilinmeyen kaynaklardan yükleme"ye izin verin). Android TV'de USB bellek, *Downloader* uygulaması ya da `adb` ile:

```bash
adb connect <TV-IP>:5555
adb install -r RedoX-TV-v2.5.2-tv.apk
```

**3. İzleyin.** Açılışta 133 kanal hazırdır: `OK` ile izleyin, `OK` basılı tutarak kanal özelliklerine (favori, kilit, düzenle, gizle) gidin. Oynatıcıda `OK` çubuğu açar, `◄` kanal listesini, `►` bir sonraki yayın kaynağını, `▲ ▼` kanal değiştirmeyi yapar.

**4. Kendi listenizi ekleyin.** TV'de **M3U ve Dosya** (ya da telefonda **Kaynaklar**): M3U adresi, dosya, Xtream Codes veya QR ile telefondan gönderme. Listeler **Özel M3U** sekmesinde, verdiğiniz adla klasör olur.

> ⚠️ **Kurulum notu:** v2.5.0 ve öncesi, her derlemede değişen bir debug anahtarıyla imzalanıyordu. **v2.5.1'den itibaren sabit bir anahtar kullanılır.** Eski bir sürüm kuruluysa **bir kez** kaldırıp yenisini kurun (önce *M3U ve Dosya → Yedek al* ile yedek alabilirsiniz); bundan sonraki sürümler üzerine güncellenir → [Kurulum Rehberi](https://github.com/ByReoxXx/RedoX-TV-2/wiki/Kurulum-Rehberi).

---

## 🎮 Kumanda haritası

**Android TV** — yalnızca yön tuşları, `OK` ve `Geri` olan kumandalarla tam çalışır.

| Ekran | Tuş | İşlev |
|---|---|---|
| Kanal ızgarası | `OK` / `OK` basılı tut | İzle / odağı üstteki panele (İzle, Favori, Kilit, Düzenle, Gizle) götür |
| Tüm ekranlar | `Geri` | Bir üst düzeye: listeden kategori çiplerine, çiplerden yan menüye; yan menüde ikinci basış uygulamadan çıkar |
| Oynatıcı | `▲ ▼` | Kanal değiştir |
| Oynatıcı | `◄` / `►` | Kanal listesi / sonraki yayın kaynağı |
| Oynatıcı | `OK` | Alt çubuk: kanal ve program, oynatma düğmeleri, kanal listesi, favori, alternatif yayın, **Seçenekler** |
| Oynatıcı | `0` | Önceki kanal |
| Özel M3U | `OK` / `Geri` | Klasörü aç / klasör listesine dön |
| Favoriler | `►` → `▲ ▼` → `OK` | Kanalı tut, taşı, bırak |

**Klavye ve fare (telefon/tablet/ChromeOS ve TV):**

| Tuş | İşlev |
|---|---|
| `Boşluk` / `K` | Oynat – duraklat |
| `←` `→` | 10 saniye geri / ileri (canlı yayında yok) |
| `↑` `↓` / fare tekerleği | Ses (TV oynatıcısında tekerlek kanal değiştirir) |
| `M` | Sessiz |
| `PgUp` `PgDn` / `N` `P` | Sonraki / önceki kanal |
| `C` `A` `L` | Altyazı / ses izi / canlıya dön |
| `Enter` | Denetimleri aç/kapat |
| `Esc` | Oynatıcıyı küçült / geri (TV'de `Geri` gibi) |
| `Ctrl+1…0` · `Ctrl+F` | Sekmelere geç · aramaya git (telefon/tablet) |

**Telefon / tablet:** kanala dokun = izle, uzun bas = seçenekler (favori, kilit, gizle, düzenle, TV'de oynat). Oynatıcıda sol kenarda dikey kaydır = parlaklık, sağ kenarda = ses, yatay kaydır = kanal değiştir, çift dokun = duraklat/oynat. Ayrıntı: [Kumanda ve Dokunmatik Kontroller](https://github.com/ByReoxXx/RedoX-TV-2/wiki/Kumanda-ve-Dokunmatik-Kontroller).

---

## 📚 Kanal listesi nasıl çalışır?

- Yerleşik liste `channel/channels.enc` dosyasındadır (AES-256 şifreli JSON). Uygulama her açılışta bu dosyayı bu depodan indirip yeniler; internet yoksa son indirilen ya da APK içindeki kopya kullanılır. Böylece kanal listesi, **uygulamayı güncellemeden** düzelebilir.
- Liste, Türksat'ın resmi frekans listesindeki **şifresiz (TKGS)** kanallarla eşleşen ve çalışan yayını olan kanallardan oluşur; sıra Türksat frekans listesine göre dizilidir. Aynı kanalın tekrarları tek kanalda **alternatif akış** olarak birleştirilmiştir; bir akış açılmazsa uygulama kendiliğinden sıradakine geçer.
- Dilerseniz kendi M3U / Xtream listelerinizi eklersiniz; yerleşik liste yerine değil, yanında durur.

Ayrıntı: [Wiki → Kanal Listesi](https://github.com/ByReoxXx/RedoX-TV-2/wiki/Kanal-Listesi).

---

<details>
<summary><b>🧩 Tüm özellikler</b></summary>

### Her üç cihazda
- **📻 Radyo.** Ayrı bir sekme: TRT radyoları, ulusal ve yerel istasyonlar (şifreli liste, açılışta güncellenir). TV'de **kadran** arayüzü (`◄ ►` istasyon çevirir, `OK` duraklatır, `▲` istasyon çekmecesi), telefonda kaydırılan kadran, tablette liste + oynatıcı paneli. Kopan yayın kendini toparlar ve yedek yayına geçer; yayında varsa çalan şarkı gösterilir (telefon/tablette şarkı geçmişi); istasyon değişirken isteğe bağlı nostaljik cızırtı; uyku zamanlayıcısı; telefon ve tablette arka planda ses. Ayrıntı: [Radyo](https://github.com/ByReoxXx/RedoX-TV-2/wiki/Radyo).
- **Yeni kanal duyurusu.** Kanal listesi güncellenince eklenen kanallar karşılama ekranında bir kez gösterilir.
- **Gerçek yayın rehberi (XMLTV) ve hatırlatıcılar.** XMLTV adresi (`.xml` / `.xml.gz`) eklenir; M3U listelerinizdeki rehber adresleri de otomatik kullanılır. Kanallar rehberle `tvg-id` ve ad benzerliğiyle eşleşir. *Uydurma program adı gösterilmez:* rehberi olmayan kanalda yalnızca "Canlı yayın" yazar. Programa dokunarak hatırlatma eklersiniz.
- **Kalite seçici, gerçek istatistikler, gece modu.** Otomatik / 1080p / 720p / 480p / Veri Tasarrufu. Çözünürlük, bit hızı, ağ tahmini ve düşen kare sayısı **ölçülen** değerlerdir. Gece modu konuşmaları belirginleştirir, ani yüksek sesleri baskılar.
- **Arama.** Kanal adı, kategori, numara ve gerçek program adları içinde; küçük yazım hatalarını tolere eder.
- **Akıllı raflar.** "İzlemeye devam et", "En çok izlediklerin", "Favorilerinde şimdi yayında".
- **Çoklu favori listeleri.** İstediğiniz kadar liste; sıralama, yeniden adlandırma, silme. **Kanal adı / numarası düzenleme** (yalnızca bu cihazda, geri alınabilir).
- **Çoklu kaynak ve otomatik yedek akış geçişi**, canlı yayında duraklatma, ebeveyn PIN kilidi, kanal gizleme, 2'li ve 4'lü bölünmüş ekran, uyku zamanlayıcı.
- **Tema.** Kızıl & Altın (varsayılan), Okyanus, Orman, Mono paletleri; OLED modu (saf siyah); yazı boyutu.
- **Güvenlik ve bakım.** Katı TLS, PIN'li yerel sunucu, güncelleme denetimi, dosyaya yedekleme, yerel tanılama günlüğü ve tek dokunuşla paylaşılabilir **hata raporu** (sürüm, cihaz bilgisi, kayıtlı hatalar; kişisel veri içermez).

### Android TV
- **Android TV ana ekranı entegrasyonu:** Favori Kanallar satırı ve "Devam et" (Watch Next) — TV'nizin ana ekranı destekliyorsa.
- **Telefon = kumanda sunucusu:** telefon/tablet uygulaması TV'yi ağda bulur, ekrandaki 6 haneli kodla eşleşir.
- QR kodlu web sayfasıyla telefondan M3U / Xtream / yedek gönderimi (kodla korunur).

### Telefon ve tablet
- **Hareketler:** sol kenarda dikey kaydır = parlaklık, sağ kenarda = ses, yatay kaydır = kanal değiştir, çift dokun = duraklat/oynat.
- **Otomatik PiP** ve **arka planda ses** (bildirim ve kilit ekranı kontrolleriyle).
- **Telefon = Kumanda:** yön tuşları, ses, kanal, rakamlar, TV'deki kanal listesi; **"TV'de oynat"** ile telefondaki bir kanalı TV'ye gönderme.
- Tablet: gezinti rayı, çok sütunlu ızgara, yan yana 2'li / 2×2 bölünmüş ekran.

</details>

<details>
<summary><b>🎨 Tasarım dili "Kızıl Halka" ve vektör logo</b></summary>

**RedoX = Red + Ox** — "kırmızı boğa". Logo, kırmızı faset (low-poly) boğa başı, altın halka ve parlayan gözlerden oluşur; "RedoX" kırmızı italik, "TV" altın yazılır. Logo **tamamen vektördür** (`VectorDrawable`): uygulama simgesi (Android 13+ temalı simge dahil), bildirim simgesi, TV afişi ve uygulama içi işaret her ekran yoğunluğunda keskin kalır. SVG dosyaları: [`docs/brand/`](docs/brand).

Arayüz paleti doğrudan logodan ölçülmüştür:

| Renk | Değer | Kullanım |
|---|---|---|
| Kızıl (şarap → mercan) | `#590823` `#96112F` `#BD1436` `#F02848` `#FA6465` | Eylemler, canlı rozeti, seçili öğeler |
| Altın | `#A17844` `#DAA24F` `#F3C05D` `#FDDE7A` | **Odak halkası**, favori yıldızı, ilerleme çubukları |
| Lacivert-siyah | `#0A0A14` `#161621` `#1F1D2A` | Zemin, kart, yükseltilmiş yüzey (OLED modunda saf siyah) |

Logodaki altın halka arayüzün **odak göstergesidir**: TV'de odaktaki öğe 3 dp altın çerçeve ve yumuşak ışımayla belirir; kırmızı yalnızca eylem ve canlı yayın içindir. Ayrıntı: [Tasarım Dili ve Logo](https://github.com/ByReoxXx/RedoX-TV-2/wiki/Tasarim-Dili-ve-Logo).

</details>

<details>
<summary><b>🔒 Güvenlik ve gizlilik</b></summary>

- **Katı TLS (varsayılan).** Yalnızca *yayın akışları*, bölgesel yayıncıların kendinden imzalı sertifikalarını kabul eden ayrı bir istemci kullanır; liste/rehber indirmeleri, güncelleme denetimi ve kanal senkronu sertifikayı doğrular.
- **Yerel sunucu kimlik doğrulaması.** TV'nin web sayfası ve kumanda API'si aynı ağdaki herkese açık değildir: 6 haneli kodla oturum açılır; yanlış kodla 5 denemeden sonra 60 sn beklenir. Kod yalnızca TV'de görünür.
- **Ebeveyn PIN'i** ekranda gösterilmez; yedeğe yazılmaz. `android:allowBackup="false"`.
- **Tanılama günlüğü** yalnızca cihazdadır; paylaşmak sizin elinizdedir. Hiçbir veri otomatik gönderilmez. Güncelleme denetimi yalnızca GitHub'daki en son sürüm numarasını okur.

Ayrıntı: [Güvenlik ve Gizlilik](https://github.com/ByReoxXx/RedoX-TV-2/wiki/Guvenlik-ve-Gizlilik).

</details>

<details>
<summary><b>🛠️ Geliştiriciler için</b></summary>

Bilgisayarınıza Android Studio / JDK / SDK kurmanız **gerekmez**; derleme GitHub Actions'ta yapılır.

1. Depoyu kendi hesabınıza çatallayın ya da yeni bir depoya yükleyin.
2. **Actions** sekmesinde *RedoX TV - APK derle* çalışır; bitince **Artifacts** altında üç APK görünür.
3. **Release için:** `git tag v2.5.2 && git push origin v2.5.2`. APK'lar Releases sayfasına yüklenir; uygulamadaki güncelleme denetimi bu sayfaya bakar. Sürüme özel "Yenilikler" metni `.github/release-notes/<etiket>.md` dosyasından eklenir.
4. **Sabit imza anahtarı** için `REDOX_KEYSTORE_BASE64`, `REDOX_KEYSTORE_PASSWORD`, `REDOX_KEY_ALIAS`, `REDOX_KEY_PASSWORD` gizli değerlerini ekleyin ([adımlar](https://github.com/ByReoxXx/RedoX-TV-2/wiki/Kurulum-Rehberi)).
5. `ui-*` dallarına her push'ta telefon, tablet ve Android TV emülatörlerinde uygulama açılıp gezilir, ekran görüntüleri alınır (`ci/capture.sh`); görüntüler `shots-phone` / `shots-tablet` / `shots-tv` dallarına yüklenir.

```text
app/src/
├── main/    ortak çekirdek: veri, ViewModel, oynatıcı fabrikası, tasarım sistemi, ağ ve özellik merkezi
├── tv/      Android TV arayüzü (kumanda odaklı) + TV ana ekranı entegrasyonu
├── mobile/  telefon ve tabletin ortak ekranları, oynatma merkezi, arka plan servisi
├── phone/   telefon iskeleti (alt gezinti, mini oynatıcı)
└── tablet/  tablet iskeleti (gezinti rayı)
```

`app/build.gradle.kts` üç ürün çeşidi (`tv`, `tablet`, `phone`) tanımlar. Teknik ayrıntı: [Teknik Mimari ve Geliştirici Rehberi](https://github.com/ByReoxXx/RedoX-TV-2/wiki/Teknik-Mimari-ve-Gelistirici-Rehberi).

</details>

---

## 🧭 Dürüstlük notları

- **Gerçek cihaz denemesi sınırlıdır.** Kod GitHub Actions'ta derlenir ve emülatörlerde (telefon, tablet, Android TV) otomatik gezilir; ancak gerçek TV / telefon / tablet cihazlarında ve gerçek yayınlarla uzun süreli deneme yapılmadı. Sorun görürseniz [Issues](https://github.com/ByReoxXx/RedoX-TV-2/issues)'a yazın.
- **Yayın rehberi herkese açık bir kaynaktan gelir.** Ayarlar'dan adres eklemediyseniz varsayılan olarak üçüncü taraf bir Türkiye rehberi (iptv-epg.org, ~100 kanal; yaklaşık 45'iyle eşleşir) kullanılır; veri tasarrufu açıkken mobil veride otomatik yenilenmez. Dilediğiniz zaman Ayarlar'dan silebilir ya da kendi XMLTV adresinizi ekleyebilirsiniz. Rehberi olmayan kanalda arayüz program adı uydurmaz.
- **Ağ bit hızı** yalnızca ExoPlayer'ın ölçtüğü tahmindir; kaynağın gerçek kapasitesi değildir.
- **Android TV ana ekranı**, **PiP** ve **arka plan sesi** üreticiye / Android sürümüne göre küçük farklar gösterebilir.
- **Radyo alarmı, karma mod ve klavye/fare kısayolları** emülatörde derlenip gezildi; gerçek cihazda henüz denenmedi. Radyo alarmı bildirim iznine bağlıdır.

## 🙏 Atıflar ve sorumluluk reddi

- **RedoX TV bir oynatıcıdır; içerik barındırmaz veya sağlamaz.** Yerleşik liste, internette herkese açık yayın adreslerine işaret eder. Yayınların telif ve erişim koşulları ilgili yayıncılara aittir; kullanım sorumluluğu size aittir, lütfen yerel mevzuata uyun.
- **Telif hakkı:** Ekran görüntülerinde bir televizyon ya da radyo yayınından kare, gerçek kanal adı ya da logo **kullanılmaz**; kanal ve istasyon listeleri uydurma örnektir, oynatıcı görüntüleri telifsiz demo yayınlardandır. Uygulamadaki gerçek kanal adları ve logoları ilgili yayıncıların ticari markalarıdır; RedoX TV bu kuruluşlarla bağlantılı ya da onlarca onaylı değildir.
- Ekran görüntülerindeki demo yayın: *Big Buck Bunny* © Blender Foundation | [www.blender.org](https://www.blender.org), [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/).
- Kullanılan açık kaynak bileşenler: [Jetpack Compose](https://developer.android.com/jetpack/compose), [Media3 / ExoPlayer](https://developer.android.com/media/media3), [Coil](https://coil-kt.github.io/coil/), [OkHttp](https://square.github.io/okhttp/).

Öneri ve hata bildirimleri için **Issues**, katkı için **Pull Requests** bölümlerini kullanabilirsiniz.
