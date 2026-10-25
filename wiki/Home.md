# 🐂 RedoX TV 2 Wiki

**RedoX TV 2**, eski RedoX TV'nin **ayrı bir uygulama olarak** yeniden tasarlanmış sürümüdür: aynı sadık arayüz düzeni, logodan ölçülen kızıl-altın-lacivert palet ("Kızıl Halka" tasarım dili), tamamen vektör logo ve yeni özellikler. Üç cihaz sınıfı için **üç ayrı APK** üretir (TV, tablet, telefon); her birinin kendi arayüzü vardır.

> **RedoX = Red + Ox** — "kırmızı boğa". Logoda "RedoX TV" yazar; uygulamanın adı **RedoX TV 2**'dir.

<p align="center"><img src="https://raw.githubusercontent.com/ByReoxXx/RedoX-TV-2/main/docs/brand/readme-hero.jpg" alt="RedoX TV 2 — Android TV, tablet ve telefonda" width="100%"></p>

**Hızlı bağlantılar:** [GitHub deposu](https://github.com/ByReoxXx/RedoX-TV-2) • [APK'ları indir (Releases)](https://github.com/ByReoxXx/RedoX-TV-2/releases) • [Hata / öneri (Issues)](https://github.com/ByReoxXx/RedoX-TV-2/issues)

---

## 📚 İçindekiler

1. **[📸 Ekran Görüntüleri](Ekran-Goruntuleri.md)** — TV, telefon ve tablet arayüzünden görüntüler
2. **[🚀 Kurulum Rehberi](Kurulum-Rehberi.md)** — APK'lar, TV / tablet / telefon kurulumu, GitHub'da derleme ve imza anahtarı
3. **[🎮 Kumanda ve Dokunmatik Kontroller](Kumanda-ve-Dokunmatik-Kontroller.md)** — TV kumanda haritası, telefon/tablet hareketleri
4. **[📚 Kanal Listesi](Kanal-Listesi.md)** — yerleşik 133 kanallık paket: seçim, sıra, güncelleme
5. **[📱 M3U, Xtream ve QR ile Yükleme](M3U-Xtream-ve-QR-Yukleme.md)** — kaynak ekleme, Özel M3U klasörleri, yedekleme
6. **[📅 Yayın Rehberi ve Hatırlatıcılar](Yayin-Rehberi-ve-Hatirlaticilar.md)** — gerçek XMLTV rehberi, eşleştirme, hatırlatma
7. **[📺 Oynatıcı, Yedek Akış ve Çoklu Ekran](Coklu-Ekran-ve-Oynatici.md)** — oynatıcı çubuğu, kalite, gece modu, PiP, arka plan sesi, 2x2
8. **[📲 Telefon = Kumanda](Telefon-Kumandasi.md)** — eşleştirme, tuşlar, "TV'de oynat"
9. **[🔒 Güvenlik ve Gizlilik](Guvenlik-ve-Gizlilik.md)** — katı TLS, kod/anahtar, yedekleme, tanılama
10. **[🛠️ Teknik Mimari ve Geliştirici Rehberi](Teknik-Mimari-ve-Gelistirici-Rehberi.md)** — kaynak kümeleri, derleme, CI
11. **[❓ Sıkça Sorulan Sorular](Sikca-Sorulan-Sorular-SSS.md)** — sorun giderme
12. **[🎨 Tasarım Dili ve Vektör Logo](Tasarim-Dili-ve-Logo.md)** — logodan ölçülen palet, altın odak halkası, vektör simgeler

---

## 🌟 Özet

| Özellik | Açıklama |
| :--- | :--- |
| **Hazır kanal listesi** | Türksat (TKGS) şifresiz kanallarıyla eşleşen **133 kanal / 415 yayın**, denenmiş; açılışta kendiliğinden güncellenir ([ayrıntı](Kanal-Listesi.md)) |
| **Üç ayrı APK** | TV (kumanda odaklı), tablet (gezinti rayı, ızgaralar), telefon (alt gezinti, hareketler, kumanda) |
| **Eski uygulamadan bağımsız** | Paket `com.redoxtv.two`; eski RedoX TV ile yan yana kurulabilir, veriler paylaşılmaz |
| **Tek satırlık oynatıcı çubuğu** | Ekranın ~%15'i; ayarlar **Seçenekler** menüsünde ([ayrıntı](Coklu-Ekran-ve-Oynatici.md)) |
| **Özel M3U klasörleri** | Eklediğiniz her liste verdiğiniz adla bir klasör ([ayrıntı](M3U-Xtream-ve-QR-Yukleme.md)) |
| **Gerçek yayın rehberi** | XMLTV (`.xml` / `.xml.gz`) + hatırlatıcılar; rehber yoksa program adı uydurulmaz |
| **Kalite, istatistik, gece modu** | Otomatik / 1080p / 720p / 480p / Veri Tasarrufu; ölçülen çözünürlük ve bit hızı |
| **Telefon = Kumanda** | Ağda TV bulma, 6 haneli kodla eşleşme, yön tuşları, ses, kanal, "TV'de oynat" |
| **Arama ve akıllı raflar** | Program adlarında, yazım hatasına toleranslı, sesli arama; devam et / en çok izlenen |
| **Çoklu favori listeleri** | Sınırsız liste; kanal adı/numarası düzenleme |
| **Mobil oynatıcı** | Hareketler, otomatik PiP, bildirimli arka plan sesi, mini oynatıcı |
| **Android TV ana ekranı** | Favori Kanallar satırı ve "Devam et" (cihaz destekliyorsa) |
| **Güvenlik ve bakım** | Katı TLS, PIN'li yerel sunucu, güncelleme denetimi, dosyaya yedek, yerel tanılama günlüğü |

> ⚠️ **Not:** Kod GitHub Actions'ta derlenir ve emülatörde otomatik gezilip ekran görüntüsü alınır; ancak gerçek TV / telefon / tablet cihazlarında ve gerçek yayınlarla uzun süreli deneme yapılmadı. Sorun görürseniz Issues'a yazın. Ayrıntı için [Teknik Mimari](Teknik-Mimari-ve-Gelistirici-Rehberi.md) sayfasına bakın.
