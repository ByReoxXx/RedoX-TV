# 📅 Yayın Rehberi (EPG) ve Hatırlatıcılar

Eski sürümlerde rehber, kategori şablonlarından üretilen **uydurma** program adlarıydı. RedoX TV 2'de rehber **gerçek XMLTV verisinden** gelir; veri yoksa uygulama program adı **uydurmaz**, yalnızca "Canlı yayın" yazar.

## Varsayılan rehber

Ayarlar'dan hiç adres kaydetmediyseniz uygulama varsayılan olarak herkese açık bir Türkiye rehberini (`iptv-epg.org`, ~100 kanal, 7 günlük) kullanır; bizim kanalların yaklaşık yarısıyla eşleşir, yerel kanalların çoğunda rehber verisi yoktur. Bu üçüncü taraf bir kaynaktır: kapanırsa rehber boş kalır, program adı yine uydurulmaz. **Veri tasarrufu** açıkken mobil veride otomatik yenilenmez (elle yenileme çalışır). Adresi Ayarlar'dan silebilir ya da kendi XMLTV adresinizle değiştirebilirsiniz.

## Rehberi açmak

<p align="center"><img src="https://raw.githubusercontent.com/ByReoxXx/RedoX-TV-2/main/docs/screenshots/tv/14-yayin-rehberi.jpg" alt="Rehber yüklenmediğinde" width="80%"><br><sub>XMLTV adresi eklenmediyse rehber ekranı böyle görünür; program adı uydurulmaz.</sub></p>

1. XMLTV adresini ekleyin:
   - **TV:** *Ayarlar → Yayın Rehberi (EPG) → Adresi Ekle ve Rehberi Yükle*
   - **Telefon/tablet:** *Ayarlar → Yayın rehberi (EPG)*
   - Ya da M3U eklerken "XMLTV rehber adresi" alanını doldurun; M3U listelerindeki rehber adresleri otomatik kullanılır.
2. Adres `http://` veya `https://` ile başlamalı; `.xml` ve `.xml.gz` desteklenir. Dosya akışla okunur (büyük dosyalar belleği şişirmez), son 3 saat ile sonraki 48 saatin programları tutulur ve diskte önbelleğe alınır.
3. Durum satırı kaç kanalın eşleştiğini, kaç program yüklendiğini ve son güncelleme zamanını gösterir. Liste yenilendiğinde ve rehber 6 saatten eskiyse otomatik güncellenir; elle de güncellenebilir.

## Kanal eşleştirme

Kanallar XMLTV kanallarıyla sırasıyla şunlara göre eşleşir: M3U'daki `tvg-id`, kanal kimliği, kimliğin/adın **normalleştirilmiş** hali (Türkçe karakterler, "HD/FHD/TV/TR" gibi ekler, parantez içleri yok sayılır), son olarak ad benzerliği. Eşleşme oranı kaynağınıza ve kanal adlarına bağlıdır; eşleşmeyen kanallarda "Canlı yayın" görünür.

## Rehber ekranı

- Yalnızca rehberi olan kanallar varsayılan olarak gösterilir ("Hepsini göster" ile tümü).
- Her kanal için sıradaki programlar listelenir: **yayındakine** dokunmak kanalı açar, **ileridekine** dokunmak hatırlatma ekler/kaldırır.
- Kanal kartlarındaki ilerleme çubuğu ve "Şu An / Sıradaki" satırı yalnızca gerçek rehber verisi olduğunda görünür.

## Hatırlatıcılar

- Bir programa hatırlatma eklenince, başlamadan **2 dakika önce** bildirim gelir; bildirime dokunmak kanalı açar.
- Hatırlatıcılar kalıcıdır: cihaz yeniden başlasa da alarmlar otomatik yeniden kurulur; geçmiş olanlar temizlenir.
- Android'in güç tasarrufu nedeniyle alarm **birkaç dakika sapabilir** (tam zamanlı alarm izni kullanılmaz).
- Android 13+ cihazlarda bildirim izni gerekir; uygulama hatırlatma eklerken ister (*Ayarlar → Bildirim izni iste* ile de verilebilir).
- TV'de uygulama açıkken hatırlatma ekranda kısa bir bilgi çubuğu olarak da gösterilir.
- Tüm hatırlatıcılar *Ayarlar*'da listelenir ve silinebilir.

## Arama ile bağlantısı

Arama, gerçek program adlarını da tarar: "haber" yazınca yayın akışında "haber" geçen programların kanalları da listelenir.
