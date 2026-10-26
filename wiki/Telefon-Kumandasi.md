# 📲 Telefon = Kumanda

Telefon veya tablet uygulaması, aynı Wi-Fi ağındaki RedoX TV (TV APK'sı) için kumandaya dönüşür.

## 1) Eşleştirme

1. TV'de **Ayarlar → Telefon Kumandası & Güvenlik**'e (ya da *M3U ve Dosya → Telefon QR & PIN*'e) girin: TV'nin adresi ve **6 haneli bağlantı kodu** görünür.
2. Telefonda **Kumanda** sekmesini açın. Uygulama ağda TV'leri otomatik bulur (mDNS/NSD). Bulamazsa **Adresi elle gir** ile TV'nin IP'sini yazın (varsayılan port 8585).
3. TV'nin yanındaki **Eşleştir** düğmesine basıp ekrandaki kodu girin.

Eşleşme sonrası telefon, TV'nin verdiği rastgele bir **erişim anahtarını** saklar; kodu bir daha sormaz. Birden fazla TV eşleştirip aralarında geçiş yapabilirsiniz.

> Güvenlik: kod yalnızca TV ekranında görünür. Yanlış kodla 5 denemeden sonra 60 sn beklenir. TV'de **Kodu Yenile** yeni kod üretir; **Tüm Eşleşmeleri Kaldır** bütün telefonların erişimini iptal eder. Ayrıntı: [Güvenlik ve Gizlilik](Guvenlik-ve-Gizlilik.md).

## 2) Kumanda ekranı

<p align="center"><img src="https://raw.githubusercontent.com/ByReoxXx/RedoX-TV-2/main/docs/screenshots/phone/08-kumanda.jpg" alt="Telefondaki kumanda ekranı" width="280"></p>

- **Yön tuşları + OK, Geri, Menü** (TV'de Menü, kanal kartında `OK` basılı tutmayla aynı kısayoldur; Chromecast kumandasında Menü tuşu olmadığı için uygulama onu şart koşmaz)
- **Ses −/+** (TV'nin medya sesi; mevcut seviye gösterilir)
- **Duraklat/oynat, Durdur, Kanal −/+**
- **0-9 rakam tuşları**
- Bağlı TV'nin **şu an ne oynattığı** üstte görünür (3 sn'de bir yenilenir); TV'ye ulaşılamıyorsa kırmızı gösterge çıkar.
- **TV'de kanal aç:** "TV kanallarını yükle" ile TV'deki kanal listesini alır, arayıp bir kanala dokunarak TV'de açarsınız. TV'de PIN ile kilitli kanallar uzaktan açılmaz.

## 3) TV'de oynat ("Send to TV")

Telefondaki bir kanalı TV'ye göndermek için: kanala **uzun basın → TV'de oynat**, ya da tam ekran oynatıcıda sağ üstteki **Cast** simgesi. Kanalın akış adresi TV'ye gönderilir ve TV'de açılır (etkin TV, *Kumanda* sekmesinde seçili olandır). Telefonda PIN ile kilitli kanallar TV'ye gönderilemez.

## Sorun giderme

- TV listede görünmüyor: iki cihaz aynı ağda mı? Bazı yönlendiriciler istemci yalıtımı yapar (misafir ağı); **Adresi elle gir** deneyin.
- "TV'ye ulaşılamadı": TV uyku modunda olabilir; uygulamayı açın. IP adresi değiştiyse yeniden eşleştirin.
- İlk RedoX TV (v1.x) bu özelliği desteklemez; yalnızca bu uygulamanın **TV APK'sı** kumanda sunucusu çalıştırır.
