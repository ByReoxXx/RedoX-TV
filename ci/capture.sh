#!/usr/bin/env bash
# Emülatörde uygulamayı açıp gezinir ve ekran görüntüleri alır.
# Kullanım: bash ci/capture.sh phone|tablet|tv   (APK önceden derlenmiş olmalı: assemble<Flavor>Debug)
set -u
KIND="${1:?tür gerekli: phone | tablet | tv}"
mkdir -p shots

case "$KIND" in
  phone)  PKG="com.redoxtv.two.phone" ;;
  tablet) PKG="com.redoxtv.two.tablet" ;;
  tv)     PKG="com.redoxtv.two" ;;
  *) echo "bilinmeyen tür: $KIND"; exit 2 ;;
esac
ACTIVITY="com.example.chromecasttv.MainActivity"
APK="$(ls app/build/outputs/apk/"$KIND"/debug/*.apk | head -n1)"
echo "APK: $APK  paket: $PKG"

# Radyo (yalnız ui-radio dalı): TV yan menüsüne "Radyo" satırı eklenir; sıralı tuş adımları buna göre kayar
RADIO=0
[ "${GITHUB_REF_NAME:-}" = "ui-radio" ] && RADIO=1

shot() { adb exec-out screencap -p > "shots/${KIND}-$1.png" && echo "görüntü: $1"; }
key()  { adb shell input keyevent "$1"; }
keys() { local n="$1"; shift; for _ in $(seq 1 "$n"); do key "$1"; sleep 0.4; done; }
tap()  { python3 ci/ui.py tap "$1" || true; }
launch() { adb shell am start -n "$PKG/$ACTIVITY" >/dev/null; }

# ---- Vitrin (README / wiki görselleri) ---------------------------------------------------------------------------------
# Temiz bir durum kurar: adlandırılmış üç özel liste (biri telifsiz "Big Buck Bunny" yayını içerir; © Blender Foundation,
# CC BY 3.0) ve örnek favoriler. Hata ayıklama sürümünde run-as ile yazılır; uygulama kapalıyken çağrılır.
vitrin_seed() {
  adb shell am force-stop "$PKG"; sleep 1
  printf '#EXTM3U\n#EXTINF:-1 group-title="Çizgi Film",Big Buck Bunny\nhttps://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8\n#EXTINF:-1 group-title="Çizgi Film",Çizgi Film 2\nhttps://example.com/cocuk2.m3u8\n' > shots/_a.m3u
  printf '#EXTM3U\n#EXTINF:-1 group-title="Futbol",Maç Kanalı 1\nhttps://example.com/spor1.m3u8\n#EXTINF:-1 group-title="Futbol",Maç Kanalı 2\nhttps://example.com/spor2.m3u8\n#EXTINF:-1 group-title="Haber",Haber Kanalı\nhttps://example.com/haber.m3u8\n' > shots/_b.m3u
  printf '#EXTM3U\n#EXTINF:-1 group-title="Kısa Film",Tears of Steel\nhttps://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8\n' > shots/_c.m3u
  local f
  for f in a b c; do adb push "shots/_$f.m3u" "/data/local/tmp/redox_$f.m3u" >/dev/null || true; done
  local dir="/data/data/$PKG"
  adb shell "run-as $PKG sh -c 'mkdir -p files/imported_playlists shared_prefs && cp /data/local/tmp/redox_a.m3u files/imported_playlists/a.m3u && cp /data/local/tmp/redox_b.m3u files/imported_playlists/b.m3u && cp /data/local/tmp/redox_c.m3u files/imported_playlists/c.m3u'" || true
  printf "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n    <string name=\"custom_m3u_playlists_str\">pl-a|Çocuk ve Aile|$dir/files/imported_playlists/a.m3u||true;;;pl-b|Spor Listem|$dir/files/imported_playlists/b.m3u||true;;;pl-c|Açık Lisanslı Filmler|$dir/files/imported_playlists/c.m3u||true</string>\n    <string name=\"fav_order_csv\">ch_27_trt_1,ch_20_show_tv,ch_3_atv,ch_16_kanal_d,ch_21_star_tv,ch_64_ntv,ch_70_trt_haber,ch_50_trt_spor</string>\n</map>\n" > shots/_prefs.xml
  adb push shots/_prefs.xml /data/local/tmp/redox_prefs.xml >/dev/null || true
  adb shell "run-as $PKG sh -c 'cp /data/local/tmp/redox_prefs.xml shared_prefs/redoxtv_clean_prefs_v5.xml'" || true
  rm -f shots/_a.m3u shots/_b.m3u shots/_c.m3u shots/_prefs.xml
}

adb install -r -g "$APK" || { echo "kurulum başarısız"; exit 1; }
adb shell settings put global window_animation_scale 0 || true
adb shell settings put global transition_animation_scale 0 || true
adb shell settings put global animator_duration_scale 0 || true
# Tam ekrana ilk girişte çıkan sistem açıklamasını ("Viewing full screen") kapat
adb shell settings put secure immersive_mode_confirmations confirmed || true
adb logcat -c || true

if [ "$KIND" = "tv" ]; then
  # Gerçek cihaza yakın yoğunluk (1080p, 213 dpi ≈ 1440x810 dp) ve küçük ekran (320 dpi ≈ 960x540 dp)
  for DENSITY in 213 320; do
    P="d${DENSITY}"
    adb shell wm density "$DENSITY" || true
    adb shell am force-stop "$PKG"; sleep 2
    launch; sleep 22
    shot "${P}-01-karsilama"

    key KEYCODE_DPAD_CENTER; sleep 7            # Canlı TV'ye başla
    shot "${P}-02-canli"
    keys 2 KEYCODE_DPAD_RIGHT; key KEYCODE_DPAD_DOWN; sleep 1
    shot "${P}-03-canli-odak"

    # Kumandada Menü tuşu yok: OK'a basılı tutmak odağı üstteki "İzle" düğmesine götürmeli,
    # aşağı yön tuşu Favori / Kilit / Düzenle / Gizle düğmelerine, Geri ise karta döndürmeli
    adb shell input keyevent --longpress KEYCODE_DPAD_CENTER; sleep 2
    shot "${P}-03b-uzun-basis-izle"
    key KEYCODE_DPAD_DOWN; sleep 1
    shot "${P}-03c-ozellik-dugmeleri"
    key KEYCODE_BACK; sleep 2
    shot "${P}-03d-geri-karta"

    key KEYCODE_DPAD_CENTER; sleep 2            # kanalı aç
    shot "${P}-04a-oynatici-bilgi"              # alt çubuk + kısayol ipuçları (odak videoda)
    sleep 4
    shot "${P}-04-oynatici"
    key KEYCODE_DPAD_CENTER; sleep 1            # kontrolleri odakla (odak oynat/duraklatta)
    shot "${P}-05-oynatici-kontroller"
    keys 6 KEYCODE_DPAD_RIGHT; key KEYCODE_DPAD_CENTER; sleep 1   # "Seçenekler" düğmesine git, menüyü aç
    shot "${P}-05b-oynatici-secenekler"
    key KEYCODE_BACK; sleep 1                   # menüyü kapat (odak "Seçenekler" düğmesine döner)
    sleep 8                                     # çubuk kendiliğinden gizlenir
    key KEYCODE_DPAD_LEFT; sleep 2              # hızlı kanal çekmecesi
    shot "${P}-06-cekmece"
    key KEYCODE_BACK; sleep 1                   # çekmeceyi kapat
    key KEYCODE_BACK; sleep 3                   # bilgi çubuğu açıksa gizler, değilse oynatıcıdan çıkar
    # Emülatörde akış alternatife geçince bilgi çubuğu kendiliğinden yeniden açılabilir:
    # liste ekranı (yan menü) görünene kadar Geri (en çok 2 kez daha)
    for _ in 1 2; do
      if python3 ci/ui.py has "Favorilerim"; then break; fi
      key KEYCODE_BACK; sleep 3
    done
    shot "${P}-07-geri-liste"

    # kategori çipleri: Geri, listeden seçili çipe (Tümü) götürür; sağ ok "Son izlenenler"e geçer.
    # OK ile seçilince odak çipte kalmalı, liste yalnızca izlenenleri göstermeli
    key KEYCODE_BACK; sleep 1; key KEYCODE_DPAD_RIGHT; key KEYCODE_DPAD_CENTER; sleep 2
    shot "${P}-07b-son-izlenenler"

    # ---- Radyo (K2 Kadran; yalnız ui-radio dalında): liste, oynatıcı, katmanlar, durumlar ve mini çubuk ----
    if [ "$RADIO" = 1 ]; then
      # Yeniden başlatma son kanalı oynatıcıda otomatik açtığı için sıfırlanmaz: 07b'de liste ekranındayız,
      # (kategori çipinden) Geri ile yan menüye çıkılır. Oynatıcı açıksa liste görünene kadar Geri.
      for _ in 1 2 3; do
        if python3 ci/ui.py has "Favorilerim"; then break; fi
        key KEYCODE_BACK; sleep 3
      done
      key KEYCODE_BACK; sleep 1                                  # içerikten yan menüye
      key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_CENTER; sleep 3    # Radyo (Canlı TV'nin altında)
      shot "${P}-r01-radyo-liste"
      key KEYCODE_DPAD_CENTER; sleep 3                           # ilk istasyonu çal
      shot "${P}-r02-radyo-calan"
      key KEYCODE_DPAD_CENTER; sleep 2                           # çalan satıra yeniden OK: tam ekran oynatıcı
      shot "${P}-r03-oynatici"
      adb shell input keyevent --longpress KEYCODE_DPAD_CENTER; sleep 2    # Seçenekler
      shot "${P}-r04-secenekler"
      key KEYCODE_BACK; sleep 1
      key KEYCODE_DPAD_UP; sleep 2                               # istasyon çekmecesi
      shot "${P}-r05-cekmece"
      key KEYCODE_BACK; sleep 1
      key KEYCODE_DPAD_LEFT; shot "${P}-r06-gezinirken"          # kadran: 0,7 sn içinde alınır; liste başından sola sarınca son (demo) istasyona gider
      sleep 3.5; shot "${P}-r07-calismiyor"
      key KEYCODE_DPAD_LEFT; sleep 3.5; shot "${P}-r08-yedek-yayin"
      key KEYCODE_DPAD_LEFT; sleep 3; shot "${P}-r09-baglaniyor"
      keys 3 KEYCODE_DPAD_RIGHT; sleep 3                         # başa sar: ilk istasyon çalar
      key KEYCODE_BACK; sleep 2                                  # oynatıcıdan radyo listesine (ses sürer)
      shot "${P}-r10-liste-calan"
      key KEYCODE_BACK; sleep 1; key KEYCODE_DPAD_UP; key KEYCODE_DPAD_CENTER; sleep 3   # Canlı TV: ince mini çubuk
      shot "${P}-r11-mini-cubuk"
      key KEYCODE_BACK; sleep 1; key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_CENTER; sleep 3   # Radyo'ya dön (içerik odakta)
    fi

    # yan menü: Geri, odağı seçili sekmeye (başlangıçta Canlı TV) götürür; aşağı yön tuşuyla istenen sekmeye inilir.
    # Sıra: Canlı TV, Favorilerim, Özel M3U, Bölünmüş Ekran, Yayın Rehberi, Kanal Ara, M3U ve Dosya, Ayarlar.
    # Sekme açılınca odak içeriğe geçer; bir sonraki sekme için yine Geri ile başlanır.
    key KEYCODE_BACK; sleep 1
    key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_CENTER; sleep 3
    shot "${P}-08-favoriler"
    key KEYCODE_BACK; sleep 1; keys 2 KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_CENTER; sleep 8
    shot "${P}-09-bolunmus-ekran"
    key KEYCODE_BACK; sleep 1; key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_CENTER; sleep 4
    shot "${P}-10-rehber"
    key KEYCODE_BACK; sleep 1; key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_CENTER; sleep 3
    shot "${P}-11-ara"
    key KEYCODE_BACK; sleep 1; key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_CENTER; sleep 3
    shot "${P}-12-m3u"
    key KEYCODE_BACK; sleep 1; key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_CENTER; sleep 3
    shot "${P}-13-ayarlar"

    # Geri: içerikten yan menüye, ikinci basışta çıkış uyarısı (uygulamadan hemen çıkmaz).
    # Ayarlar açılınca odak yan menüde kalır: önce sağ ok içeriğe geçer, sonra Geri yan menüye döndürür.
    key KEYCODE_DPAD_RIGHT; sleep 1
    key KEYCODE_BACK; sleep 2
    shot "${P}-14-geri-yan-menu"
    key KEYCODE_BACK; sleep 1
    shot "${P}-15-cikis-uyarisi"

    # Özel M3U klasörleri: adlandırılmış iki liste ekle (hata ayıklama sürümünde run-as ile), uygulamayı yeniden aç,
    # Özel M3U sekmesine in; klasör listesi, klasörün içi ve Geri ile dönüş
    adb shell am force-stop "$PKG"; sleep 1
    printf '#EXTM3U\n#EXTINF:-1 group-title="Spor",Mac Kanali 1\nhttps://example.com/spor1.m3u8\n#EXTINF:-1 group-title="Spor",Mac Kanali 2\nhttps://example.com/spor2.m3u8\n#EXTINF:-1 group-title="Haber",Haber Kanali\nhttps://example.com/haber.m3u8\n' > shots/_a.m3u
    printf '#EXTM3U\n#EXTINF:-1 group-title="Cocuk",Cizgi Film 1\nhttps://example.com/cocuk1.m3u8\n#EXTINF:-1 group-title="Cocuk",Cizgi Film 2\nhttps://example.com/cocuk2.m3u8\n' > shots/_b.m3u
    adb push shots/_a.m3u /data/local/tmp/redox_a.m3u >/dev/null || true
    adb push shots/_b.m3u /data/local/tmp/redox_b.m3u >/dev/null || true
    DIR="/data/data/$PKG"
    adb shell "run-as $PKG sh -c 'mkdir -p files/imported_playlists shared_prefs && cp /data/local/tmp/redox_a.m3u files/imported_playlists/a.m3u && cp /data/local/tmp/redox_b.m3u files/imported_playlists/b.m3u'" || true
    printf "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n    <string name=\"custom_m3u_playlists_str\">pl-a|Spor Listem|$DIR/files/imported_playlists/a.m3u||true;;;pl-b|Cocuk ve Aile|$DIR/files/imported_playlists/b.m3u||true</string>\n</map>\n" > shots/_prefs.xml
    adb push shots/_prefs.xml /data/local/tmp/redox_prefs.xml >/dev/null || true
    adb shell "run-as $PKG sh -c 'cp /data/local/tmp/redox_prefs.xml shared_prefs/redoxtv_clean_prefs_v5.xml'" || true
    rm -f shots/_a.m3u shots/_b.m3u shots/_prefs.xml
    launch; sleep 22
    key KEYCODE_DPAD_CENTER; sleep 7            # Canlı TV'ye başla
    key KEYCODE_BACK; sleep 1; key KEYCODE_BACK; sleep 1         # ızgaradan çiplere, çiplerden yan menüye
    keys $((2 + RADIO)) KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_CENTER; sleep 3   # Özel M3U
    shot "${P}-16-klasorler"
    key KEYCODE_DPAD_CENTER; sleep 3            # klasörü aç
    shot "${P}-17-klasor-ici"
    key KEYCODE_BACK; sleep 1; key KEYCODE_BACK; sleep 2         # ızgaradan çiplere, çiplerden klasör listesine
    shot "${P}-18-klasorlere-geri"
  done

  # ---- Vitrin: temiz durum, örnek favoriler, telifsiz demo yayın (README / wiki görselleri) ----
  adb shell wm density 320 || true
  vitrin_seed
  launch; sleep 22
  shot "v-01-karsilama"
  key KEYCODE_DPAD_CENTER; sleep 7            # Canlı TV'ye başla
  shot "v-02-canli"
  adb shell input keyevent --longpress KEYCODE_DPAD_CENTER; sleep 2
  key KEYCODE_DPAD_DOWN; sleep 0.5
  keys 3 KEYCODE_DPAD_LEFT; sleep 1           # odak "Favori" düğmesine
  shot "v-03-kanal-ozellikleri"
  key KEYCODE_BACK; sleep 1; key KEYCODE_BACK; sleep 1; key KEYCODE_BACK; sleep 1   # düğmelerden karta, çiplere, yan menüye
  keys $((1 + RADIO)) KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_CENTER; sleep 3           # Favorilerim
  shot "v-04-favoriler"
  key KEYCODE_BACK; sleep 1; key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_CENTER; sleep 3   # Özel M3U
  shot "v-05-ozel-m3u"
  key KEYCODE_DPAD_CENTER; sleep 3            # ilk klasör: "Çocuk ve Aile"
  shot "v-06-klasor-ici"
  key KEYCODE_DPAD_CENTER; sleep 1.5          # "Big Buck Bunny"yi oynat (alt çubuk + kısayol ipuçları görünür)
  shot "v-07a-oynatici-bilgi"
  shot "v-07b-oynatici-bilgi"
  sleep 8                                     # çubuk gizlenir (odak videoda), yayın oturur
  key KEYCODE_DPAD_LEFT; sleep 2              # hızlı kanal çekmecesi (odak oynatıcıdayken sol ok)
  shot "v-10-kanal-cekmecesi"
  key KEYCODE_BACK; sleep 1
  shot "v-x1-cekmece-geri"                    # tanı: Geri çekmeceyi kapatıyor mu?
  key KEYCODE_DPAD_RIGHT; sleep 1             # çekmece hâlâ açıksa kapanır (satırda sağ ok çekmeceyi kapatır)
  sleep 5                                     # çubuk (varsa) gizlenir
  key KEYCODE_BACK; sleep 3                   # oynatıcıdan çık (klasör ızgarasına dön)
  # Kontroller ve Seçenekler ayrı, yeni bir oynatmada alınır: çubuk kapalıyken başlanır ve tuşlar ekran görüntüsü araya
  # girmeden verilir (çubuk kapanınca sağ ok "sonraki yayın kaynağı" olur ve yayın baştan başlar).
  key KEYCODE_DPAD_CENTER; sleep 10           # "Big Buck Bunny"yi yeniden oynat; çubuk gizlensin
  key KEYCODE_DPAD_CENTER; sleep 1            # kontrolleri odakla
  keys 6 KEYCODE_DPAD_RIGHT; key KEYCODE_DPAD_CENTER; sleep 1   # "Seçenekler"
  shot "v-09-oynatici-secenekler"
  key KEYCODE_BACK; sleep 1                   # menü kapanır; odak "Seçenekler" düğmesinde
  keys 6 KEYCODE_DPAD_LEFT; sleep 0.5         # odak oynat/duraklatta
  shot "v-08-oynatici-kontroller"
  sleep 6                                     # çubuk gizlenir
  key KEYCODE_BACK; sleep 3                   # oynatıcıdan çık (klasör ızgarasına dön)
  key KEYCODE_BACK; sleep 1; key KEYCODE_BACK; sleep 1; key KEYCODE_BACK; sleep 1   # çiplere, klasör listesine, yan menüye
  keys 3 KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_CENTER; sleep 3    # Kanal Ara
  shot "v-11-ara"
  key KEYCODE_BACK; sleep 1; key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_CENTER; sleep 3   # M3U ve Dosya
  shot "v-12-m3u"
  key KEYCODE_BACK; sleep 1; key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_CENTER; sleep 3   # Ayarlar
  shot "v-13-ayarlar"
  adb shell wm density reset || true
else
  adb shell am force-stop "$PKG"; sleep 1
  launch; sleep 24
  shot "01-ana-sayfa"

  tap "Canlı TV"; sleep 5;   shot "02-canli"
  tap "Rehber"; sleep 4;     shot "03-rehber"
  tap "Favoriler"; sleep 3;  shot "04-favoriler"
  tap "Kumanda"; sleep 3;    shot "05-kumanda"
  tap "Canlı TV"; sleep 4

  # ilk kanal satırı / kartı: ekranın üst bölümünde, içerik alanında
  W="$(adb shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -n1)"
  SW="${W%x*}"; SH="${W#*x}"
  if [ "$KIND" = "phone" ]; then
    adb shell input tap $((SW / 2)) $((SH * 22 / 100))
  else
    adb shell input tap $((SW * 45 / 100)) $((SH * 24 / 100))
  fi
  sleep 12; shot "06-oynatici"
  sleep 7;  shot "07-oynatici-kontroller-gizli"
  adb shell input tap $((SW / 2)) $((SH / 2)); sleep 2; shot "08-oynatici-kontroller"
  key KEYCODE_BACK; sleep 3; shot "09-mini-oynatici"

  tap "Ana Sayfa"; sleep 4;  shot "10-ana-sayfa-devam"
  tap "Ayarlar"; sleep 4;    shot "11-ayarlar"
  adb shell input swipe $((SW / 2)) $((SH * 70 / 100)) $((SW / 2)) $((SH * 25 / 100)) 400; sleep 2
  shot "12-ayarlar-kaydirilmis"

  if [ "$KIND" = "tablet" ]; then
    tap "Ara"; sleep 3;               shot "13-ara"
    tap "Bölünmüş Ekran"; sleep 8;    shot "14-bolunmus-ekran"
    tap "Kaynaklar"; sleep 3;         shot "15-kaynaklar"
  fi

  # ---- Radyo (K2 Kadran; yalnız ui-radio dalında): liste, oynatıcı, sayfalar, durumlar ve mini çubuk ----
  if [ "$RADIO" = 1 ]; then
    adb shell am force-stop "$PKG"; sleep 1
    launch; sleep 24
    tap "Radyo"; sleep 4;              shot "r01-radyo-liste"
    tap "Yıldız Pop"; sleep 3;         shot "r02-radyo-calan"             # istasyonu çal (telefonda mini çubuk, tablette sağ panel)
    if [ "$KIND" = "phone" ]; then
      tap "Yıldız Pop"; sleep 2;       shot "r03-oynatici"                # çalan satıra dokun: tam ekran oynatıcı
    fi
    tap "Seçenekler"; sleep 2;         shot "r04-secenekler"
    key KEYCODE_BACK; sleep 1                                             # sayfayı kapat
    tap "İstasyonlar"; sleep 2;        shot "r05-istasyonlar"
    key KEYCODE_BACK; sleep 1
    if [ "$KIND" = "phone" ]; then key KEYCODE_BACK; sleep 2; fi          # tam ekran oynatıcıyı kapat (ses sürer)
    tap "Demo ("; sleep 2                                                 # Demo kategorisi: durumları denemek için üç istasyon
    tap "Çalışmayan"; sleep 4;         shot "r06-calismiyor"
    tap "Yedek yayına"; sleep 3;       shot "r07-yedek-yayin"
    tap "Yavaş bağlanan"; sleep 3;     shot "r08-baglaniyor"
    if [ "$KIND" = "phone" ]; then
      tap "Yavaş bağlanan"; sleep 2;   shot "r09-oynatici-baglaniyor"     # çalan satıra dokun: oynatıcıda durum
      key KEYCODE_BACK; sleep 2
    fi
    tap "Gerçek yayın"; sleep 15;      shot "r11-gercek-yayin"            # gerçek ses hattı: bağlanır, "Çalıyor" + ICY şarkı bilgisi
    tap "Canlı TV"; sleep 4;           shot "r10-mini-cubuk"
  fi

  # ---- Vitrin: temiz durum, örnek favoriler, telifsiz demo yayın (README / wiki görselleri) ----
  vitrin_seed
  launch; sleep 24
  shot "v-01-ana-sayfa"
  tap "Canlı TV"; sleep 5;        shot "v-02-canli"
  tap "Çocuk ve Aile"; sleep 3;   shot "v-03-klasor"
  tap "Big Buck Bunny"; sleep 12; shot "v-04-oynatici"
  adb shell input tap $((SW / 2)) $((SH / 2)); sleep 2; shot "v-05-oynatici-kontroller"
  key KEYCODE_BACK; sleep 3;      shot "v-06-mini-oynatici"
  tap "Favoriler"; sleep 3;       shot "v-07-favoriler"
  tap "Kumanda"; sleep 3;         shot "v-08-kumanda"
  tap "Ana Sayfa"; sleep 4;       shot "v-09-ana-sayfa-devam"
  tap "Ayarlar"; sleep 4;         shot "v-10-ayarlar"
fi

# Emülatördeki Chrome/Widevine kayıtları Google'ın herkese açık "AIza..." sağlama anahtarını URL içinde yazabilir;
# depoya gönderilen dosyada GitHub'ın gizli anahtar uyarısı çıkmasın diye anahtar ve "key=" değerleri gizlenir.
adb logcat -d | grep -E "chromecasttv|redoxtv|AndroidRuntime: (FATAL|Shutting)|FATAL|ANR" \
  | sed -E 's/AIza[0-9A-Za-z_-]{35}/GIZLENDI/g; s/([?&]key=)[^& "]+/\1GIZLENDI/g' \
  | tail -n 400 > "shots/${KIND}-logcat.txt" || true
echo "tamam: $(ls shots | wc -l) dosya"
