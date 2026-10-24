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

shot() { adb exec-out screencap -p > "shots/${KIND}-$1.png" && echo "görüntü: $1"; }
key()  { adb shell input keyevent "$1"; }
keys() { local n="$1"; shift; for _ in $(seq 1 "$n"); do key "$1"; sleep 0.4; done; }
tap()  { python3 ci/ui.py tap "$1" || true; }
launch() { adb shell am start -n "$PKG/$ACTIVITY" >/dev/null; }

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

    key KEYCODE_DPAD_CENTER; sleep 6            # kanalı aç
    shot "${P}-04-oynatici"
    key KEYCODE_DPAD_CENTER; sleep 1            # kontrolleri odakla
    keys 3 KEYCODE_DPAD_RIGHT; sleep 1
    shot "${P}-05-oynatici-kontroller"
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
  done
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
fi

# Emülatördeki Chrome/Widevine kayıtları Google'ın herkese açık "AIza..." sağlama anahtarını URL içinde yazabilir;
# depoya gönderilen dosyada GitHub'ın gizli anahtar uyarısı çıkmasın diye anahtar ve "key=" değerleri gizlenir.
adb logcat -d | grep -E "chromecasttv|redoxtv|AndroidRuntime: (FATAL|Shutting)|FATAL|ANR" \
  | sed -E 's/AIza[0-9A-Za-z_-]{35}/GIZLENDI/g; s/([?&]key=)[^& "]+/\1GIZLENDI/g' \
  | tail -n 400 > "shots/${KIND}-logcat.txt" || true
echo "tamam: $(ls shots | wc -l) dosya"
