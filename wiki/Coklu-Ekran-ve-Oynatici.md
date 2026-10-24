# 📺 Oynatıcı, Yedek Akış, Kalite ve Çoklu Ekran

## 🔁 Çoklu kaynak ve otomatik yedek geçişi (Auto-Failover)

- Bir kanalın altında birden fazla akış olabilir (**Akış 1/2/3…**). Ana akış açılmazsa ya da hata verirse oynatıcı kullanıcı müdahalesi olmadan sıradaki kaynağa geçer; yayın uzun süre gelmezse de (TV'de ~2,5 sn, telefon/tablette ~6 sn) yedeğe döner.
- Sorunsuz açılan kaynak **kanal bazında hatırlanır**; bir sonraki açılış doğrudan oradan başlar.
- Tüm kaynaklar denenip açılmazsa kanal "yanıt vermiyor" işaretlenir; **Alternatif kaynak** / **Tekrar dene** ile elle de denenebilir.

## 🎚️ Kalite seçici

| Seçenek | Davranış |
|---|---|
| **Otomatik** | Sınırsız, uyarlanabilir (ağınıza göre) |
| **1080p** | Bant genişliğine bakmadan 1080p'ye kadar en yüksek kaliteyi seçer (TV varsayılanı) |
| **720p** | 720p ve en çok ~4,5 Mbps |
| **480p** | 480p ve en çok ~1,8 Mbps |
| **Veri Tasarrufu** | 360p ve en çok ~0,9 Mbps |

Telefon/tablette *Mobil veri tasarrufu* açıksa, yalnızca mobil veri kullanılırken kalite otomatik olarak **Veri Tasarrufu**'na düşer (Wi-Fi'de değişmez). Kalite oynatıcıdan (TV: kontrol çubuğundaki **Kalite** simgesi, mobil: ⚙ ayarlar) veya *Ayarlar*'dan seçilir.

## 📊 Gerçek istatistikler

Oynatıcı bilgi alanı **ölçülen** değerleri gösterir: çözünürlük (örn. `1080p`), codec, video bit hızı, ExoPlayer'ın ağ hızı tahmini, düşen kare sayısı ve tampon süresi. Bilinmeyen değerler `—` ile gösterilir; eskiden olduğu gibi tahmini sabitler **gösterilmez**. Ağ hızı yalnızca bir tahmindir, kaynağın gerçek kapasitesi değildir.

## 🌙 Gece modu

Konuşmaları belirginleştirir, ani yüksek sesleri baskılar (Android'in `LoudnessEnhancer` efektiyle; ses seviyesi düşürülüp kazanç artırılır). Desteklemeyen cihazlarda etkisizdir ve uygulama bundan etkilenmez. Bilgi çubuğunda etkinse "Gece modu" etiketi görünür.

## ⏯️ Canlı yayında duraklatma

Duraklat/oynat, **15 sn geri sar** (TV) ve **Canlıya dön**. Duraklatma, oynatıcının tamponu kadar geriye dönüşe izin verir.

## 📱 Mobil oynatıcı özellikleri

- **Hareketler:** sol kenar parlaklık, sağ kenar ses, yatay kaydırma kanal değiştirme, çift dokunma duraklat/oynat ([ayrıntı](Kumanda-ve-Dokunmatik-Kontroller.md)).
- **Mini oynatıcı:** `⌄` ile küçültünce yayın (ses) sürer; alttaki çubuktan duraklatır, tam ekrana döner ya da kapatırsınız.
- **Otomatik PiP:** oynatırken ana ekrana çıkınca küçük pencere (Android 8+; üreticiye göre değişebilir). Elle de başlatılabilir.
- **Arka planda ses:** uygulamadan çıkınca ya da ekran kapanınca ses sürer; bildirimden ve kilit ekranından denetlenir (Media3 oturumu). Açıkken veri/pil tasarrufu için görüntü kapatılır. Ayarlardan kapatılırsa arka plana geçince yayın duraklar.
- **Uyku zamanlayıcı:** 30 / 60 / 90 / 120 dk; süre dolunca oynatma kapanır.
- **Görüntü oranı** (16:9, ekranı doldur, sinema zoom), **ses izi** ve **altyazı** seçimi — kanal bazında hatırlanır.

## 🔲 Bölünmüş ekran (Çoklu Ekran)

- **2'li** ya da **4'lü (2x2)** düzende aynı anda birden çok kanal. Ses yalnızca seçili ekrandadır; ekrana dokunmak/odaklanmak sesi o ekrana alır.
- Her hücrede kanal ±, tam ekran ve (TV'de) yedek akış seçimi bulunur.
- Telefonda 2'li ekranlar üst üste, tablette yan yana dizilir.
- 4 yüksek çözünürlüklü yayın cihaz dekoderini zorlayabilir: hücreler düşük çözünürlüğe sınırlanır ve `TextureView` + dekoder yedeği kullanılır.

## ⭐ Favoriler, kilit ve gizleme

- **Çoklu favori listeleri:** "Favoriler" her zaman vardır (silinemez/adı değişmez); istediğiniz kadar ek liste (Çocuklar, Spor…) oluşturabilir, adlandırabilir, silebilirsiniz. Sıralama: TV'de **tut & taşı**, telefonda ↑/↓; ayrıca A-Z / kanal no / kategori ile otomatik sıralama.
- **PIN korumalı ebeveyn kilidi:** kanallar 4 haneli PIN ile kilitlenir (TV: kanal başlığında **Kilitle**; mobil: uzun basıp **PIN ile kilitle**). Kilitli kanal PIN girilmeden açılmaz ve **TV'ye gönderilemez**. PIN ekranda gösterilmez, yedeğe yazılmaz.
- **Gizle / geri getir:** istenmeyen kanallar listeden gizlenir, *Kaynaklar* (mobil) veya *M3U ve Dosya* (TV) ekranından geri getirilir.
- **Kanal adı / numarası düzenleme:** yalnızca bu cihazda uygulanır, M3U listesi değişmez; **Varsayılana dön** ile geri alınır.
