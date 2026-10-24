# 📱 M3U, Xtream Codes ve QR ile Yükleme

Kendi kanal listelerinizi üç yoldan ekleyebilirsiniz: **TV'nin kendi ekranından**, **telefonun tarayıcısından (QR)** ya da **telefon/tablet uygulamasından**.

## 📺 TV'de: "M3U ve Dosya" sekmesi

| Alt sekme | Ne yapar? |
|---|---|
| **Dosya & İçe Aktar** | Dahili dosya gezgini: `Download`, ana dizin ve USB bellekten `.m3u` / `.m3u8` seçer. Dosya erişimi için TV'de depolama izni istenir. |
| **Xtream Codes API** | Sunucu adresi, kullanıcı adı ve şifre (şifre ekranda gizlenir) ile hesabı bağlar. |
| **M3U URL** | M3U/M3U8 adresi (+ isteğe bağlı XMLTV rehber adresi) ekler. |
| **Telefon QR & PIN** | QR kodu ve **6 haneli bağlantı kodunu** gösterir (aşağıya bakın). Ebeveyn PIN'ini de buradan değiştirirsiniz (PIN ekranda gösterilmez). |

Eklenen listeler *Özel M3U* sekmesinde, **eklerken verdiğiniz adla bir klasör** olarak görünür: `OK` klasörü açar ve kanalları kategori çipleriyle listeler, `Geri` klasör listesine döner. Telefon ve tabletteki Canlı TV ekranında her liste, verdiğiniz adla bir klasör çipidir. Aynı adlı kanalların alternatif akışları tek kanal altında birleştirilir. Listelerin XMLTV adresleri gerçek yayın rehberi için otomatik kullanılır ([Rehber](Yayin-Rehberi-ve-Hatirlaticilar.md)).

## 🌐 Telefonun tarayıcısından (QR)

1. TV'de **M3U ve Dosya → Telefon QR & PIN** sekmesini açın (telefon ve TV aynı Wi-Fi'de olmalı).
2. Telefon kamerasıyla QR'ı okutun ya da tarayıcıya `http://<TV-IP>:8585` yazın (8585 doluysa 8586…8595).
3. Açılan sayfa **bağlantı kodunu** ister: TV ekranında görünen 6 haneyi girin. Yanlış kodla 5 denemeden sonra 60 saniye beklenir.
4. Sayfadan M3U adresi veya dosya içeriği, Xtream hesabı gönderebilir, yedeği indirebilir ya da geri yükleyebilirsiniz.

> Kod yalnızca TV ekranında görünür ve **Kodu Yenile** ile değiştirilebilir. Kod olmadan hiçbir işlem yapılamaz; böylece aynı ağdaki başkaları TV'nize liste gönderemez.

## 📲 Telefon / tablet uygulamasında: "Kaynaklar"

- **M3U / M3U8 adresi** (+ XMLTV adresi) ve **Xtream Codes** formları.
- **Dosyadan içe aktar:** Android'in dosya seçicisi açılır; ek izin gerekmez.
- **Eklenen listeler:** silme ve yenileme; gizlenen kanalları geri getirme.
- **Yedekle / geri yükle (SAF):** favoriler, favori listeleri, M3U listeleri, kanal adları/numaraları ve rehber adresleri tek bir metin dosyasına yazılır; dosyayı Drive, Dosyalar vb. istediğiniz yere kaydedersiniz.

## 💾 Yedek içeriği ve gizlilik

- Yedeğe **ebeveyn PIN'i yazılmaz** (eski yedekler geri yüklenirken yine okunur).
- Yedek, M3U adreslerinizi içerir; Xtream kullanıcı adı/şifresi adreste gömülü olabilir. Dosyayı güvenli yerde saklayın.
- TV'de *Yedek Al (Download)* `redox_tv_backup.txt` dosyasını `Download` klasörüne yazar.
