# RedoX TV 2 — R8 (kod küçültme) kuralları

# Çökme kayıtları okunabilir kalsın
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Android TV ana ekran entegrasyonu (tvhome/TvHomeIntegration) sınıf ve yöntemleri ada göre (reflection) çağırır;
# R8 bunları ada göre bulunamaz hale getirmesin.
-keep class androidx.tvprovider.media.tv.** { *; }

# İsteğe bağlı TLS sağlayıcıları (OkHttp) cihazda bulunmayabilir; bunlar için uyarı verme
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
