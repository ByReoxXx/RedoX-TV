# ❓ Sıkça Sorulan Sorular ve Sorun Giderme

### İlk RedoX TV'nin (v1.x) üzerine mi kuruluyor?
Hayır. Bu sürüm **ayrı bir uygulamadır** (`com.redoxtv.two`). Eski uygulama yerinde kalır; ayarlar/favoriler paylaşılmaz. Eski favorilerinizi taşımak için eski sürümde yedek alıp (`redox_tv_backup.txt`) yeni uygulamada **Yedekten geri yükle** diyebilirsiniz (eski ebeveyn PIN'i de okunur).

### Üç APK'dan hangisini kurmalıyım?
TV/TV box → `…-tv.apk`; tablet → `…-tablet.apk`; telefon → `…-phone.apk`. Her biri farklı bir arayüzdür.

### "Uygulama yüklenmedi" / güncellenmiyor
Aynı APK'nın üzerine yalnızca **aynı anahtarla imzalanmış** sürüm yüklenebilir. GitHub Actions'ta imza anahtarı tanımlamadıysanız her derleme farklı debug anahtarı kullanır; önce eski sürümü kaldırın veya sabit anahtar ekleyin ([Kurulum](Kurulum-Rehberi.md)).

### Yayın açılmıyor / siyah ekran
- Başka bir **akış kaynağı** deneyin (TV: `►` tuşu, mobil: ⚙ → Akış kaynağı). Uygulama zaten otomatik yedeğe geçmeyi dener.
- Kalite çok yüksekse takılma olabilir: Kalite'yi **720p** veya **Veri Tasarrufu**'na alın.
- 4'lü bölünmüş ekranda siyah hücre: cihaz dekoderi sınırıdır; 2'li görünüme geçin.
- Listeniz çalışmıyorsa sağlayıcıda süresi dolmuş olabilir.

### Program adları neden hep "Canlı yayın"?
RedoX TV program adı **uydurmaz**. *Ayarlar → Yayın Rehberi*'ne bir XMLTV adresi ekleyin; eşleşen kanallarda gerçek program adı görünür. Bkz. [Rehber](Yayin-Rehberi-ve-Hatirlaticilar.md).

### Rehber adresi ekledim ama hiçbir kanal eşleşmedi
Rehberdeki kanal adları/`tvg-id`'leri sizin listenizdekilerle uyuşmuyor olabilir. Aynı sağlayıcının hem M3U'sunu hem XMLTV'sini kullanmak en iyi sonucu verir. Durum satırı eşleşen kanal sayısını gösterir.

### Hatırlatma bildirimi gelmedi
Android 13+ için bildirim izni verin; pil tasarrufu bazı üreticilerde alarmı geciktirir (tam zamanlı alarm kullanılmaz). Cihaz yeniden başlayınca alarmlar otomatik yeniden kurulur.

### Telefon, TV'yi bulamıyor
Aynı Wi-Fi'de olun; "misafir ağı"/istemci yalıtımı keşfi engeller. **Adresi elle gir** ile TV'nin IP'sini yazın. Bkz. [Telefon = Kumanda](Telefon-Kumandasi.md).

### Bağlantı kodunu unuttum / yanlış girdim
Kod yalnızca TV ekranında görünür. 5 yanlış denemeden sonra 60 sn beklenir; TV'de **Kodu Yenile** yeni kod üretir.

### Arka planda ses çalışmıyor
*Ayarlar → Oynatma → Arka planda ses* açık olmalı; bildirim izni (Android 13+) verilmeli. Bazı üreticiler pil optimizasyonuyla arka plan servislerini kapatır: uygulamayı pil kısıtlamasından muaf tutun.

### PiP (küçük pencere) açılmıyor
Android 8+ gerekir ve *Ayarlar → Otomatik küçük pencere* açık olmalıdır. Bazı cihazlarda sistem ayarlarından uygulamaya "Resim içinde resim" izni verilmelidir.

### TV ana ekranında Favori Kanallar satırı yok
Android TV ana ekranı üreticiye göre bu özelliği farklı destekler; satır görünmezse uygulamanın geri kalanı etkilenmez. Satırı ana ekran ayarlarından görünür yapmanız gerekebilir.

### Dosya seçici (TV) dosyaları göstermiyor
TV'de depolama erişim iznini verin (*Dosya & İçe Aktar* ilk açılışta ister). Telefon/tablette dosya seçici Android'in kendi seçicisidir, izin gerekmez.

### Hata bildirimi nasıl yaparım?
*Ayarlar → Bakım → Tanılama günlüğü*'nü açıp **Paylaş** ile günlüğü kopyalayın ve GitHub Issues'a yapıştırın (hiçbir veri otomatik gönderilmez).
