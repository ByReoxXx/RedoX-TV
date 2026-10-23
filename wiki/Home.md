# 🐂 RedoX TV 2 Wiki

**RedoX TV 2**, eski RedoX TV'nin **ayrı bir uygulama olarak** yeniden tasarlanmış sürümüdür: aynı sadık arayüz düzeni, yeni kırmızı-altın-lacivert görünüm ve yeni özellikler. Üç cihaz sınıfı için **üç ayrı APK** üretir (TV, tablet, telefon); her birinin kendi arayüzü vardır.

> **RedoX = Red + Ox** — "kırmızı boğa". Logoda "RedoX TV" yazar; uygulamanın adı **RedoX TV 2**'dir.

---

## 📚 İçindekiler

1. **[🚀 Kurulum Rehberi](Kurulum-Rehberi.md)** — APK'lar, TV / tablet / telefon kurulumu, GitHub'da derleme ve imza anahtarı
2. **[🎮 Kumanda ve Dokunmatik Kontroller](Kumanda-ve-Dokunmatik-Kontroller.md)** — TV kumanda haritası, telefon/tablet hareketleri
3. **[📱 M3U, Xtream ve QR ile Yükleme](M3U-Xtream-ve-QR-Yukleme.md)** — kaynak ekleme, dosyadan içe aktarma, yedekleme
4. **[📅 Yayın Rehberi ve Hatırlatıcılar](Yayin-Rehberi-ve-Hatirlaticilar.md)** — gerçek XMLTV rehberi, eşleştirme, hatırlatma
5. **[📺 Oynatıcı, Yedek Akış ve Çoklu Ekran](Coklu-Ekran-ve-Oynatici.md)** — kalite, gece modu, istatistikler, PiP, arka plan sesi, 2x2
6. **[📲 Telefon = Kumanda](Telefon-Kumandasi.md)** — eşleştirme, tuşlar, "TV'de oynat"
7. **[🔒 Güvenlik ve Gizlilik](Guvenlik-ve-Gizlilik.md)** — katı TLS, kod/anahtar, yedekleme, tanılama
8. **[🛠️ Teknik Mimari ve Geliştirici Rehberi](Teknik-Mimari-ve-Gelistirici-Rehberi.md)** — kaynak kümeleri, derleme, CI
9. **[❓ Sıkça Sorulan Sorular](Sikca-Sorulan-Sorular-SSS.md)** — sorun giderme

---

## 🌟 Özet

| Özellik | Açıklama |
| :--- | :--- |
| **Üç ayrı APK** | TV (kumanda odaklı), tablet (gezinti rayı, ızgaralar), telefon (alt gezinti, hareketler, kumanda) |
| **Eski uygulamadan bağımsız** | Paket `com.redoxtv.two`; eski RedoX TV ile yan yana kurulabilir, veriler paylaşılmaz |
| **Gerçek yayın rehberi** | XMLTV (`.xml` / `.xml.gz`) + hatırlatıcılar; rehber yoksa program adı uydurulmaz |
| **Kalite, istatistik, gece modu** | Otomatik / 1080p / 720p / 480p / Veri Tasarrufu; ölçülen çözünürlük ve bit hızı |
| **Telefon = Kumanda** | Ağda TV bulma, 6 haneli kodla eşleşme, yön tuşları, ses, kanal, "TV'de oynat" |
| **Arama ve akıllı raflar** | Program adlarında, yazım hatasına toleranslı, sesli arama; devam et / en çok izlenen |
| **Çoklu favori listeleri** | Sınırsız liste; kanal adı/numarası düzenleme |
| **Mobil oynatıcı** | Hareketler, otomatik PiP, bildirimli arka plan sesi, mini oynatıcı |
| **Android TV ana ekranı** | Favori Kanallar satırı ve "Devam et" (cihaz destekliyorsa) |
| **Güvenlik ve bakım** | Katı TLS, PIN'li yerel sunucu, güncelleme denetimi, dosyaya yedek, yerel tanılama günlüğü |

> ⚠️ **Not:** Bu sürümün kodu geliştirme ortamında Android derleyicisinden geçirilmeden hazırlandı; ilk gerçek derleme GitHub Actions'ta yapılır. Ayrıntı için [Teknik Mimari](Teknik-Mimari-ve-Gelistirici-Rehberi.md) sayfasına bakın.
