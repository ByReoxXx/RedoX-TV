# 🔒 Güvenlik ve Gizlilik

## Katı TLS (sertifika doğrulama)

İlk RedoX TV (v1.x), uygulama genelinde **her sertifikayı kabul ediyordu**. Bu sürümde:

| Trafik | Doğrulama |
|---|---|
| Güncelleme denetimi, GitHub kanal paketi senkronu | **Her zaman katı** |
| Eklediğiniz M3U / XMLTV adresleri | Önce katı; yalnızca **sertifika hatası olursa** gevşek modda yeniden denenir |
| Canlı yayın akışları | Bölgesel yayıncıların kendinden imzalı sertifikaları için gevşek; **yalnızca oynatıcının kendi HTTP istemcisinde** — uygulamanın geri kalanını etkilemez |

## Yerel sunucu ve kumanda

TV APK'sı yerel ağda bir HTTP sunucusu çalıştırır (8585–8595 arası ilk boş port). Sunucu kimlik doğrulamasızdır **değildir**:

- **Tarayıcı sayfaları** (QR): 6 haneli kod girilmeden hiçbir işlem yapılamaz; kod doğrulanınca çerezli oturum açılır (HttpOnly, SameSite=Lax; en fazla 8 oturum).
- **Telefon kumandası (`/api/*`)**: eşleşmede TV'nin ürettiği 192 bitlik rastgele **Bearer anahtarı** gerekir (en fazla 12 eşleşmiş cihaz).
- Yanlış kodla 5 denemede 60 sn kilit. Kod karşılaştırması sabit zamanlıdır. **Kodu Yenile** tarayıcı oturumlarını sonlandırır; **Tüm Eşleşmeleri Kaldır** anahtarları siler.
- Kod yalnızca TV ekranında gösterilir; durum mesajlarına, günlüğe veya bildirimlere yazılmaz.
- İstek gövdeleri 8 MB ile sınırlıdır; yanıtlarda `Cache-Control: no-store` kullanılır.

> Not: Yerel trafik düz HTTP'dir (sertifikasız); aynı ağdaki biri trafiği dinleyebilir. Bu yüzden kod/anahtar sistemi vardır, ama güvenmediğiniz (misafir/halka açık) ağlarda kumanda özelliğini kullanmayın.

## Veriler

- **Hiçbir kişisel veri, kullanım istatistiği veya hata raporu otomatik gönderilmez.** İzleme istatistikleri (raflar için) ve tanılama günlüğü yalnızca cihazdadır.
- Güncelleme denetimi yalnızca GitHub'ın "latest release" sürüm numarasını okur (*Ayarlar*'dan kapatılabilir).
- `android:allowBackup="false"`: eşleşme anahtarları ve kodlar Android bulut yedeğine girmez.
- **Ebeveyn PIN'i** ekranda maskelenir ve yedeğe yazılmaz. Xtream şifresi ekranda gizlenir.
- **Yedek dosyası**, M3U adreslerinizi (Xtream kullanıcı adı/şifresi gömülü olabilir) içerir; güvenli yerde saklayın.
- Tanılama günlüğü (*Ayarlar → Bakım*) yalnızca isterseniz paylaşılır.

## İzinler

| İzin | Neden |
|---|---|
| İnternet / ağ durumu | Yayın, liste ve rehber indirme |
| Wi-Fi çoklu yayın durumu | TV ↔ telefon keşfi (mDNS) |
| Bildirimler (Android 13+) | Program hatırlatmaları, arka plan sesi bildirimi |
| Yeniden başlatma tamamlandı | Hatırlatıcı alarmlarını yeniden kurmak |
| Ön plan servisi (medya) | Arka planda ses (telefon/tablet) |
| Harici depolama okuma (TV) | Dahili dosya gezgini ile `.m3u` seçmek |
| TV sağlayıcı okuma/yazma (TV) | Android TV ana ekranı satırları |

## Sürüm denetimi ve imza

APK'lar sabit bir imza anahtarıyla imzalanırsa güncellemeler yalnızca aynı anahtarla gelir; kendi anahtarınızı üretin ve gizli tutun ([Kurulum](Kurulum-Rehberi.md)).
