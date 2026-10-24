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
keys() { local n="$1"; shift; for _ in $(seq 1 "$n"); do key "$1"; sleep 0.5; done; }
tap()  { python3 ci/ui.py tap "$1" || true; }
launch() { adb shell am start -n "$PKG/$ACTIVITY" >/dev/null; }

adb install -r -g "$APK" || { echo "kurulum başarısız"; exit 1; }
adb shell settings put global window_animation_scale 0 || true
adb shell settings put global transition_animation_scale 0 || true
adb shell settings put global animator_duration_scale 0 || true
adb logcat -c || true

if [ "$KIND" = "tv" ]; then
  # Gerçek cihaza yakın yoğunluk (1080p, 213 dpi ≈ 1440x810 dp) ve küçük ekran (320 dpi ≈ 960x540 dp)
  for DENSITY in 213 320; do
    adb shell wm density "$DENSITY" || true
    adb shell am force-stop "$PKG"; sleep 2
    launch; sleep 22
    shot "d${DENSITY}-01-karsilama"

    key KEYCODE_DPAD_CENTER; sleep 7            # Canlı TV'ye başla
    shot "d${DENSITY}-02-canli"
    keys 2 KEYCODE_DPAD_RIGHT; sleep 1
    shot "d${DENSITY}-03-canli-odak"
    key KEYCODE_DPAD_DOWN; sleep 1
    shot "d${DENSITY}-04-canli-alt-satir"

    key KEYCODE_DPAD_CENTER; sleep 14           # kanalı aç
    shot "d${DENSITY}-05-oynatici"
    key KEYCODE_DPAD_CENTER; sleep 2            # kontrolleri odakla
    shot "d${DENSITY}-06-oynatici-kontroller"
    keys 3 KEYCODE_DPAD_RIGHT; sleep 1
    shot "d${DENSITY}-07-oynatici-odak"
    key KEYCODE_BACK; sleep 1                   # OSD gizle
    key KEYCODE_DPAD_LEFT; sleep 2              # kanal çekmecesi
    shot "d${DENSITY}-08-cekmece"
    key KEYCODE_BACK; sleep 1; key KEYCODE_BACK; sleep 1; key KEYCODE_BACK; sleep 3
    shot "d${DENSITY}-09-geri-liste"

    # yan menüde Yayın Rehberi (5.) ve Ayarlar (8.)
    key KEYCODE_DPAD_LEFT; sleep 1
    keys 4 KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_CENTER; sleep 4
    shot "d${DENSITY}-10-rehber"
    keys 3 KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_CENTER; sleep 4
    shot "d${DENSITY}-11-ayarlar"
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

  # ilk kanal satırı / kartı: ekranın üst üçte birinde, içerik alanında
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

  tap "Ayarlar"; sleep 4;    shot "10-ayarlar"
  adb shell input swipe $((SW / 2)) $((SH * 70 / 100)) $((SW / 2)) $((SH * 25 / 100)) 400; sleep 2
  shot "11-ayarlar-kaydirilmis"

  if [ "$KIND" = "tablet" ]; then
    tap "Ara"; sleep 3;               shot "12-ara"
    tap "Bölünmüş Ekran"; sleep 8;    shot "13-bolunmus-ekran"
    tap "Kaynaklar"; sleep 3;         shot "14-kaynaklar"
  fi
fi

adb logcat -d | grep -E "chromecasttv|redoxtv|AndroidRuntime|FATAL|ANR" | tail -n 400 > "shots/${KIND}-logcat.txt" || true
echo "tamam: $(ls shots | wc -l) dosya"
