# 📚 Kanal Listesi (yerleşik paket)

RedoX TV ilk açılışta hazır bir kanal listesiyle gelir. Bu sayfa listenin **neyi içerdiğini**, **nasıl seçildiğini** ve **nasıl güncellendiğini** anlatır.

## Ne içerir?

**133 kanal / 415 yayın.** 109 kanalın birden fazla (en çok 6) yayın kaynağı vardır; bir kaynak açılmazsa uygulama kendiliğinden sıradakine geçer ([Oynatıcı ve yedek akış](Coklu-Ekran-ve-Oynatici.md)).

| Kategori | Kanal |
|---|---|
| Ulusal | 27 |
| Haber | 17 |
| Spor | 6 |
| Belgesel | 3 |
| Müzik | 4 |
| Çocuk | 7 |
| Dini | 14 |
| Kıbrıs | 7 |
| Dünya | 11 |
| Yerel | 37 |

Kategori çipleri her zaman bu sıradadır (Ulusal, Haber, Spor, Belgesel, Müzik, Çocuk, Dini, Kıbrıs, Dünya, Yerel); kanalların kendi sırası ise aşağıdaki gibidir.

## Nasıl seçildi?

1. **Türksat (TKGS) şifresiz kanallarıyla eşleşme.** Türksat'ın resmî frekans listesinde şifresiz yayın yapan kanallar esas alındı; listede olmayan ya da yalnızca şifreli olan kanallar çıkarıldı.
2. **Yayın sağlığı denemesi.** Her yayın adresi, oynatıcıyla aynı istek başlıklarıyla ve üç ayrı geçişte denendi (geçerli HLS çalma listesi + son parçaya erişim). Çalışan yayını kalmayan kanallar listeye alınmadı.
3. **Aynı kanalın tekrarları birleştirildi.** Aynı kanalın farklı adlarla yazılmış kayıtları (örn. *TV 4* / *TV4*) tek kanal olur; yayınları **alternatif akış** olarak altına dizilir. Kanal kimlikleri korunur: favorileriniz ve kilitleriniz sürümler arasında bozulmaz.

## Sıra

Kanal numaraları ve sırası **Türksat frekans listesi tablosundaki** (T4A) satır sırasını izler; tabloda bulunmayan kanallar sona eklenir. Bu tablo, uydu alıcılarındaki TKGS kanal numaralandırmasının (LCN) kendisi **değildir**; yani numaralar alıcınızdakilerle aynı olmayabilir. Kanal numarası/adını bu cihazda değiştirmek için kanal özelliklerindeki **Düzenle**'yi kullanabilirsiniz ([Kumanda](Kumanda-ve-Dokunmatik-Kontroller.md)).

## Nasıl güncellenir?

- Liste, bu deponun `channel/channels.enc` dosyasındadır (AES-256 şifreli JSON). **Uygulama her açılışta** bu dosyayı indirip yeniler; böylece kanal listesi **uygulamayı güncellemeden** düzelir.
- İnternet yoksa son indirilen kopya, o da yoksa APK'nın içindeki kopya (`assets/channels.enc`) kullanılır.
- Şifreleme, düz metin listenin depoda aranabilir biçimde durmaması içindir; bir gizlilik ya da DRM önlemi değildir.

## Kendi listeniz

Yerleşik listeye kanal eklenmez ama **yanına** istediğiniz kadar kendi listenizi ekleyebilirsiniz: TV'de **M3U ve Dosya**, telefonda **Kaynaklar**. Eklenen her liste **Özel M3U** sekmesinde verdiğiniz adla bir klasör olur ([M3U, Xtream ve QR](M3U-Xtream-ve-QR-Yukleme.md)). Beğenmediğiniz yerleşik kanalı **Gizle** ile listeden kaldırabilirsiniz.

## Bir yayın çalışmıyorsa

Yayınlar üçüncü taraf sunuculardadır ve zamanla kapanabilir ya da adresi değişebilir. Önce başka bir **akış kaynağını** deneyin (TV: `►`). Sürekli sorun varsa kanal adını ve saatini yazarak [Issues](https://github.com/ByReoxXx/RedoX-TV/issues)'a bildirin; liste güncellenir ve herkese kendiliğinden ulaşır.

> **Sorumluluk reddi:** RedoX TV bir oynatıcıdır; içerik barındırmaz veya sağlamaz. Liste, internette herkese açık yayın adreslerine işaret eder. Yayınların telif ve erişim koşulları ilgili yayıncılara aittir.
