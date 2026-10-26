# 🚀 Kurulum Rehberi

RedoX TV 2 üç ayrı APK olarak dağıtılır. **Cihazınıza uygun olanı** kurun:

| APK | Cihaz |
|---|---|
| `RedoX-TV-2-v2.1.0-tv.apk` | Android TV, Google TV (Chromecast with Google TV), TV Box, akıllı TV |
| `RedoX-TV-2-v2.1.0-tablet.apk` | Android tablet |
| `RedoX-TV-2-v2.1.0-phone.apk` | Android telefon (aynı zamanda TV kumandası) |

Uygulama eski RedoX TV'den **bağımsızdır** (paket `com.redoxtv.two`); ikisi aynı cihazda yan yana durabilir.

---

## 📺 Android TV / Google TV / TV Box

**Yöntem 1 — adb (bilgisayardan):**
```bash
adb connect <TV-IP>:5555
adb install -r RedoX-TV-2-v2.1.0-tv.apk
```
TV'de *Ayarlar → Cihaz Tercihleri → Hakkında → Derleme* üzerine 7 kez basarak geliştirici seçeneklerini açın, ardından *Ağ üzerinden hata ayıklama*yı etkinleştirin.

**Yöntem 2 — USB bellek:** APK'yı belleğe kopyalayın, TV'deki bir dosya yöneticisiyle açıp kurun ("bilinmeyen kaynaklardan yükleme"ye izin verin).

**Yöntem 3 — Downloader uygulaması:** Downloader'a Releases sayfasındaki APK adresini girin.

Kurulumdan sonra uygulama TV ana ekranında "RedoX TV 2" olarak görünür (TV banner'ı logodandır).

## 📱 Telefon ve tablet

APK'yı indirip açın; "bilinmeyen kaynaklardan yükleme" izni isteyecektir. Bildirimli hatırlatmalar için Android 13+ cihazlarda bildirim iznini (ilk hatırlatma eklerken) verin.

---

## 🛠️ Kendi APK'larınızı GitHub'da derleyin

Bilgisayara Android Studio kurmanız gerekmez.

1. Yeni bir GitHub deposu oluşturun ve proje dosyalarını yükleyin (`git push` önerilir).
2. **Actions** sekmesinde *RedoX TV 2 - APK derle* çalışır; bitince **Artifacts** altında `RedoX-TV-2-tv`, `RedoX-TV-2-tablet`, `RedoX-TV-2-phone` indirilebilir.
3. Release için `v2.1.0` gibi bir etiket itin: `git tag v2.1.0 && git push origin v2.1.0`. APK'lar Releases sayfasına yüklenir.

### İmza anahtarı (güncellenebilir APK)
Android yalnızca **aynı anahtarla imzalanmış** güncellemeleri kabul eder. Sabit anahtar için:
```bash
keytool -genkeypair -v -keystore redox-release.keystore -alias redox -keyalg RSA -keysize 2048 -validity 36500
base64 -w0 redox-release.keystore > redox-release.keystore.b64
```
Depo *Settings → Secrets and variables → Actions* bölümüne ekleyin: `REDOX_KEYSTORE_BASE64`, `REDOX_KEYSTORE_PASSWORD`, `REDOX_KEY_ALIAS`, `REDOX_KEY_PASSWORD`. Tanımlamazsanız APK her derlemede yeni bir debug anahtarıyla imzalanır: kurulur ama önceki derlemenin üzerine güncellenemez (önce kaldırmanız gerekir). **Bu depodaki resmi sürümler v2.5.1'den itibaren sabit anahtarla imzalıdır;** v2.5.0 ve öncesi kuruluysa yeni sürüme geçerken eski uygulamayı bir kez kaldırmanız gerekir, sonrası üzerine güncellenir. Keystore dosyasını depoya koymayın ve yedeğini saklayın.

---

## ✅ İlk açılış

<p align="center"><img src="https://raw.githubusercontent.com/ByReoxXx/RedoX-TV-2/main/docs/screenshots/tv/02-canli-tv.jpg" alt="Canlı TV" width="80%"><br><sub>İlk açılışta kanal listesi hazırdır; <code>OK</code> ile izleyin.</sub></p>

1. **TV:** *Ayarlar → Yayın Rehberi*'ne XMLTV adresi, *M3U ve Dosya*'ye kendi listenizi ekleyin ([rehber](M3U-Xtream-ve-QR-Yukleme.md)).
2. **Telefon:** *Kumanda* sekmesini açın, TV'yi seçin, TV ekranındaki 6 haneli kodu girin ([rehber](Telefon-Kumandasi.md)).
