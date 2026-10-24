# 🎨 Tasarım Dili ("Kızıl Halka") ve Vektör Logo

## Fikir

Logodaki **altın halka** arayüzün **odak göstergesidir**; **kırmızı** yalnızca eylem ve canlı yayın içindir. Zemin logodaki lacivert-siyahtır. Üç cihazda (TV, tablet, telefon) aynı belirteçler kullanılır; yerleşim cihaza göre değişir.

## Palet (logodan ölçüldü)

| Aile | Değerler | Kullanım |
|---|---|---|
| **Kızıl** | `#590823` şarap • `#96112F` derin • `#BD1436` koyu kızıl • `#F02848` ana • `#FA6465` mercan | Eylem düğmeleri (`#F02848` → `#BD1436` gradyanı), canlı rozeti, seçili öğe tonu; küçük kırmızı yazı için mercan |
| **Altın** | `#A17844` bronz • `#DAA24F` derin • `#F3C05D` altın • `#FDDE7A` açık | **Odak halkası**, favori yıldızı, ilerleme çubukları, "TV" yazısı |
| **Zemin** | `#0A0A14` zemin • `#0F0F1A` yan menü • `#161621` kart • `#1F1D2A` yükseltilmiş • `#2A2838` alan • `#2E2C3E` çizgi | Yüzeyler; AMOLED seçeneğinde saf siyah |
| **Metin** | `#F5F4F2` ana • `#B9BAC8` ikincil • `#8C8EA3` üçüncül | Başlık / açıklama / ipucu |

Metin–zemin oranları: ana metin 16,3:1, ikincil 9,3:1, üçüncül 5,5:1, altın/zemin 11,7:1, mercan/kart 6,0:1, beyaz/koyu kızıl 6,3:1.

## Odak halkası (TV)

- Odaktaki öğe: **3 dp altın çerçeve + yumuşak altın ışıma + hafif büyüme** (`tvSurface`).
- Seçili ama odakta olmayan öğe (ör. yan menüdeki sekme): kırmızı dolgu tonu.
- Yan menü iki bölümlüdür: üstte ana gezinti (Canlı TV, Favorilerim, Özel M3U, Bölünmüş Ekran, Yayın Rehberi, Kanal Ara), ince bir çizginin altında menünün dibinde **Kaynaklar** (TV'de "M3U ve Dosya"), **Ayarlar** ve TV'de Uyku zamanlayıcısı. Tablette de Kaynaklar ve Ayarlar rayın dibinde durur. Alçak ekranlarda (540 dp'lik 1080p TV) boşluklar daralır, menü kaydırmadan sığar.
- Yalnızca simgeli düğmeler (`TvIconPill`): odaklanınca adı yanında açılır; oynatıcıda odaktaki düğmenin adı kontrol çubuğunun altında tek satırda gösterilir.
- Kısayol ipuçları (`OK`, `OK basılı tut`, `Geri`) kanal ızgarasının altında sabit bir çubukta durur; kartların içindeki yazı odakla değişmez. İpuçları yalnızca yön tuşları, `OK` ve `Geri` olan kumandaya göre yazılır.

## Hareket (hafif animasyonlar)

Hareket yalnızca durumu anlatmak içindir; hepsi kısa ve sakindir (80–500 ms), GPU katmanında çalışır.

- **Odak geçişi (TV):** halka, ışıma ve zemin 130 ms'de yumuşakça açılıp kapanır; kumandayla gezerken sıçrama olmaz.
- **Basma geri bildirimi:** OK tuşuna ya da dokunuşa basılıyken düğme, kart ve çip %3–5 küçülür, bırakınca geri yaylanır.
- **Canlı göstergesi:** "CANLI" rozetindeki nokta saniyede bir yavaşça nabız atar; çalan kanalın ekolayzer çubukları hafifçe oynar.
- **İlerleme çubukları:** program ilerlemesi değişince akıcı uzar.
- **Karşılama:** içerik yukarı kayarak belirir, logonun arkasında yavaş bir altın ışıma nefes alır.
- **Uyarılar:** alttan kayarak girer, süre dolunca kayarak çıkar.
- Sekme geçişleri 140 ms çapraz solmadır; oynatıcı çubuğu ve çekmece kayarak açılır.

## Yazı ve boyutlar

- TV: başlık 26–32 sp, kanal adı 16–18 sp, açıklama 13–15 sp, en küçük 12 sp (eski 9–10 sp'lik yazılar kaldırıldı).
- Telefon/tablet: başlık 22–32 sp, gövde 14–16 sp, en küçük 12 sp; dokunma hedefleri ≥ 44 dp.
- Emoji yok. Simgeler uygulamaya özel **"Çift ton" setidir** (`RedoXIcons`): 24'lük ızgarada 1,9 birimlik yuvarlak uçlu çizgi ve kapalı gövdelerde simge renginin %22'si kadar yumuşak dolgu. Oynat, duraklat, durdur ve açık favori yıldızı dolu çizilir. Renk her yerde `tint` ile verilir (boşta gri, odakta altın, seçili kırmızı), böylece tek bir çizim her duruma uyar.

## Vektör logo

Logo tamamen **vektördür**: uygulama simgesi (uyarlanabilir ön plan + Android 13+ temalı tek renkli simge), bildirim küçük simgesi, Android TV afişi ve uygulama içindeki işaret/yazı birer `VectorDrawable`'dır. Böylece her ekran yoğunluğunda ve Android sürümünde (üst sürümlerdeki temalı simge ve maskeler dahil) keskin kalır.

| Dosya | Görev |
|---|---|
| `drawable/redox_mark.xml` | Boğa + altın halka (uygulama içi) |
| `drawable/redox_wordmark.xml` | "RedoX TV" yazısı |
| `drawable/ic_launcher_foreground.xml` / `ic_launcher_background.xml` | Uyarlanabilir simge (108 dp tuval, güvenli bölgeye sığar) |
| `drawable/ic_launcher_monochrome.xml` | Android 13+ temalı simge (parlaklık → saydamlık) |
| `drawable/ic_stat_redox.xml` | Bildirim küçük simgesi (silüet) |
| `tv: drawable/ic_tv_banner.xml` | TV afişi (vektör zemin + işaret + yazı) |

Tek bit eşlem istisnası: Android TV ana ekranı (başka bir süreç) poster yedeği olarak `tv_poster_fallback.png` ister.

## Görme ve deneme

Cihazınız yoksa: GitHub'da **Actions → "RedoX TV 2 - Arayüz önizleme"** iş akışı telefon, tablet ve Android TV emülatörlerinde uygulamayı açıp gezer ve ekran görüntüleri üretir (Artifacts ve `shots-<tür>` dalları).
