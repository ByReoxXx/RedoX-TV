# Radyo sekmesi — hazırlık ve plan

> **Durum: taslak.** Bu dalda (`ui-radio`) uygulama kodu yoktur; yalnızca radyo listesi verisi ve plan bulunur.
> `channel/radios.enc` ana dala alınmadıkça uygulama bu listeyi görmez.
> Kullanıcı yönergesi (2026-10-24): **radyo henüz yayınlanmayacak; şimdilik yalnızca arayüz tasarımı.**

## 1. Hedef

TV, tablet ve telefonda **Radyo** sekmesi:

- Türkiye'den yaklaşık 130 canlı istasyon, tür çipleriyle süzme, favoriler, arama
- Çalan şarkı adı (yayının ICY üst verisinden: "Sanatçı – Şarkı")
- Telefon/tabletde uygulama kapalıyken de çalma (bildirim + kilit ekranı kontrolleri); TV'de de hedef
- Uyku zamanlayıcısı radyoyu da durdurur
- Liste, kanal listesi gibi GitHub'dan güncellenir (uygulama sürümü beklemeden)

## 2. Veri (hazır)

| | |
|---|---|
| Kaynak | Radio Browser (topluluk dizini), Türkiye kayıtları; oy + tıklama puanı. Resmî/ulusal istasyonlar puan eşiğinden bağımsız aday |
| Sınama | Her yayın adresi iki ayrı turda Türkiye çıkışlı bağlantıdan sınandı: HTTP durumu, gerçekten ses verisi geliyor mu, HLS ise ilk parça. 302 adresin 286 / 284'ü çalıştı; iki turda da çalışan yayınlar listeye girdi |
| Sonuç | **134 istasyon, 204 yayın** — 123 HTTPS, 81 HTTP; 60 HLS; 49 istasyonda yedek yayın; 43'ünün logosu var (kalanı baş harf karosu) |
| Türler | Pop 40 · Türkü & Halk 15 · Arabesk & Fantezi 5 · Nostalji 9 · Rock & Alternatif 7 · Dans & Elektronik 15 · Klasik & Sanat 11 · Haber & Spor 11 · Dini 9 · Karışık 12 |
| Biçim | Kanal paketiyle aynı (`REDOX_ENC_V1` + AES-256-CBC); JSON'da `radios` dizisi; alanlar `id, number, name, logoText, c1, c2, cat, badge:"RADYO", logoUrl, sources[]` |
| Güncelleme | `channel/radios.enc`, kanal listesi gibi main'den her açılışta indirilir; önbellek + uygulama içi yedek dosya |

Liste yalnızca şifreli dosyada (`channel/radios.enc`) tutulur; okunur kopyası ve üretim betikleri depoya konmaz (kanal listesinde olduğu gibi).

**Dışlananlar:** siyasi ya da doğrulanamayan yayıncılar, yabancı devlet yayın organları, TV kanalının sesi olan kayıtlar ve tekrarlar listeye alınmadı; ayrıntı depoda tutulmaz.

## 3. Tasarım (kullanıcı kararı bekliyor)

- **TV — Seçenek A "vitrin + ızgara"**: Canlı TV ile aynı düzen (üstte "Çalıyor" kartı, altta tür çipleri ve kart ızgarası).
- **TV — Seçenek B "liste + büyük panel"** *(öneri)*: solda hızlı kaydırılan liste, sağda büyük "Şimdi çalıyor" paneli (sanatçı – şarkı, ses dalgası, kontroller). 130 adı taramak için daha elverişli.
- **TV — diğer sekmelerde**: radyo çalarken altta ince "Şimdi çalıyor" çubuğu (▼ ile odaklanır); yan menüde "X çalıyor". Bir TV kanalı başlayınca radyo susar.
- **Telefon**: alt gezinti `Ana Sayfa · Canlı TV · Radyo · Favoriler · Kumanda`. **Rehber** köke bağlı kalmaz; Ana Sayfa'daki "Rehber" çipinden ve Canlı TV üst çubuğundan açılır (gerçek rehber verisi olmadığında zaten boş durum gösteriyor). Mini oynatıcı + tam ekran "Şimdi çalıyor" + kilit ekranı bildirimi.
- **Tablet**: yan rayda Canlı TV'nin altında "Radyo"; iki bölme (liste + sürekli açık "Şimdi çalıyor" paneli).

### Radyo oynatıcı arayüzü (video oynatıcıdan farklı)

Kullanıcı yönergesi (2026-10-24): **radyo şimdilik yayınlanmayacak; yalnızca arayüzü tasarlanacak ve oynatıcısı video oynatıcıdan farklı olacak.**
Bu yüzden radyo, video oynatıcının alt çubuğunu/OSD'sini kullanmaz; kendi tam ekran "Şimdi çalıyor" ekranı olur: listeden OK ile açılır, Geri listeye döner (ses sürer), diğer sekmelerde altta ince mini çubuk görünür.

Üç konsept (kullanıcı seçecek):

- **K1 "Halka"** — altın halka (logodaki halka) içinde istasyon amblemi, çevresinde canlı radyal ses dalgası; altta önceki/sonraki istasyon kartları.
- **K2 "Kadran"** — yatay tuner kadranı: ibre sabit, istasyonlar kayar; ◄ ► kadranı çevirir (zapping).
- **K3 "Sahne"** — müzik uygulaması düzeni: büyük kapak, şarkı/sanatçı, yayının şarkı bilgisinden **"Son çalanlar"** geçmişi.

Uzaktan kumanda eşlemesi (yalnız yön tuşları, OK, Geri): ◄ ► önceki/sonraki istasyon · OK duraklat/sürdür · ▲ istasyon listesi · ▼ düğmeler (K3'te geçmiş) · OK basılı tut: seçenekler (favori, kaynak, uyku, gizle) · Geri: listeye dön (ses sürer).

**Seçilen oynatıcı: K2 "Kadran" (2026-10-24).** Tamamlanmış tasarım (TV, telefon, tablet) kullanıcıya panolarla gösterildi:

- **TV oynatıcı:** ◄ ► kadranı çevirir (ibre sabit, istasyonlar kayar); yeni istasyon 0,7 sn sonra otomatik çalar (OK hemen çalar; ses, yeni yayın bağlanana kadar kesilmez). OK duraklat/sürdür · ▲ istasyon çekmecesi (tür çipleri + liste) · OK basılı tut: Seçenekler (favori, yayın kaynağı, uyku zamanlayıcı, ses dalgası açık/kapalı, yayın bilgisi, istasyonu gizle, radyoyu kapat) · ▼ düğmeler · Geri radyo listesine (ses sürer).
- **Durumlar:** gezinirken (eski istasyon çalmaya devam eder, üstteki ad solar) · bağlanılıyor · yayın kesildi → alternatif yayın (n/m) · çalışmıyor (Yeniden dene / Sonraki istasyon / Radyo listesi). Uyku zamanlayıcısı üstte saat yanında "29 dk" olarak görünür.
- **TV Radyo listesi** *(varsayılan; kullanıcı onayı bekliyor)*: solda hızlı liste; sağda "Çalıyor" kartı (küçük kadran, ► ile tam ekran oynatıcı) ve "Seçili istasyon" kartı. Seçenek A (ızgara) bırakıldı.
- **Telefon** *(Rehber'in köke bağlı kalmaması varsayılan; onay bekliyor)*: Radyo listesi + mini oynatıcı → tam ekran K2 (kadran kaydırılır) → Seçenekler sayfası (kaynak, uyku, favori, gizle, kapat) → kilit ekranı bildirimi.
- **Tablet:** iki bölme; sağda sürekli açık K2 paneli.
- K3'teki "Son çalanlar" geçmişi K2'ye alınmadı; istenirse Seçenekler > Yayın bilgisi içinde gösterilebilir.

Sonraki adım (onay bekliyor): tasarımı yalnızca `ui-radio` dalında, örnek veriyle ve CI emülatör ekran görüntüleriyle kodlamak. Ana dala/sürüme alınmaz.

## 4. Teknik plan (dilimler)

Hepsi `ui-radio` dalında; her dilim ayrı işlem + CI. Yayın ve birleştirme yalnızca onayla.

1. **Veri ve model** — `DynamicChannelsJsonService`'in çözme/ayrıştırma kodu `radios` anahtarına genelleştirilir; `radios.enc` asset + önbellek (`github_radios_v1_cached.enc`); istasyonlar `TvChannel` olarak (tür = `category`, `qualityBadge = "RADYO"`) tutulur, böylece oynatma, favori, kaynak seçimi ve bildirim kodu yeniden kullanılır. Birim testi: dosya çözülür, ≥100 istasyon, `id` tekil, her adres http(s).
2. **Oynatma çekirdeği** — ses odaklı oynatıcı: video izi kapalı, `AUDIO_CONTENT_TYPE_MUSIC`, radyoya uygun arabellek, **tarayıcı gibi görünmeyen kullanıcı aracısı** (Shoutcast sunucuları "Mozilla" içeren istemciye ses yerine HTML durum sayfası döndürür; sınamalar bu yüzden oynatıcı kimliğiyle yapıldı). ICY başlığı `onMediaMetadataChanged`'den okunur. Yayın hatasında sıradaki yayına otomatik geçiş. Telefon/tablet: mevcut `PlaybackHub` + `PlaybackService`; **TV: ikisi `main`e taşınır** (TV manifestine servis + `FOREGROUND_SERVICE_MEDIA_PLAYBACK`); TV oynatıcı ekranı başlayınca radyo susar.
3. **Telefon + tablet arayüzü** — `MobileTab.RADIO`; `PhoneRootTabs` güncellenir, Rehber `push` hedefi olur; `MobileRadio.kt` (liste, çipler, arama, mini oynatıcı, tam ekran panel, favori, uyku, kaynak).
4. **TV arayüzü** — `MainNavTab.RADIO` + yeni `RedoXIcons.Radio` (üst çubuktaki "Tümünü test et" simgesinden farklı); seçilen tasarım; mini çubuk; D-pad odak yolu; uyku zamanlayıcısı (`TvViewModel.cycleSleepTimer`) radyoyu da durdurur. Yan menü 7 satıra çıkar: 540 dp'lik ekranda sığması için satır yüksekliği küçülür, CI ekran görüntüsüyle doğrulanır.
5. **CI, belgeler, yayın** — `ci/capture.sh` Radyo adımları; **yayın dumanı testi** (emülatörde 5 istasyonun `STATE_READY` olması); README + wiki "Radyo" sayfası; sürüm 2.2.0 (yalnız onayla).

## 5. Riskler ve önlemler

- **Yayın adresleri eskir** (sınamada ~%6 ölü çıktı): liste GitHub'dan güncellenir; uygulama yedek yayına geçer, çalışmayanı işaretler. İleride zamanlanmış sağlık denetimi (Actions) düşünülebilir.
- **HTTP (şifresiz) yayınlar** (81/204): manifestte `usesCleartextTraffic` açık; yalnızca ses akışı etkilenir. Mümkünse HTTPS adres önce denenir (listede öyle sıralı).
- **Yayıncı hakları**: uygulama yalnızca herkese açık yayın adreslerine bağlanır; kanal listesindeki gibi "kaldırma isteği" notu README'de yer alır.
- **Pil/veri**: video izi kapalı, ses odaklı arabellek; uyku zamanlayıcısı.
- **TV yan menü kapasitesi**: yukarıda (4. dilim).

## 6. Açık sorular

1. TV tasarımı A mı, B mi?
2. Telefonda Rehber'in köke bağlı kalmaması kabul mü?
3. TV'de arka planda çalma (servis) ilk sürümde mi, sonra mı?
4. Dışlanan/şüpheli istasyonlar için son söz (özellikle Dini ve yerel istasyonlar).
5. ~~Radyo oynatıcı konsepti~~ — **K2 Kadran seçildi.** Kalan: TV listesi düzeni, telefonda Rehber'in taşınması ve tasarımı `ui-radio` dalında kodlamaya başlama onayı.
