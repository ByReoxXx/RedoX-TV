# -*- coding: utf-8 -*-
"""Herkese açık ekran görüntüleri için UYDURMA kanal listesi üretir (gerçek kanal adı/logosu içermez).
Yayınlar telifsiz demo akışlardır: Big Buck Bunny ve Tears of Steel (© Blender Foundation, CC BY 3.0).
Kullanım: python ci/make_demo_channels.py  ->  ci/demo-channels.plain.json (şifreleme: node tools/enc_tool.js enc ...)"""
import json
import os

BBB = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
TOS = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"

# (ad, kategori, rozet)
CH = [
    ("Zümrüt TV", "Ulusal", "FHD"), ("Kuzgun Ekran", "Ulusal", "FHD"), ("Meşale TV", "Ulusal", "HD"),
    ("Gökada TV", "Ulusal", "FHD"), ("Pınar Kanal", "Ulusal", "HD"), ("Sedir TV", "Ulusal", "FHD"),
    ("Pusula Haber", "Haber", "FHD"), ("Kıvılcım Haber", "Haber", "HD"), ("Ufuk 24", "Haber", "FHD"),
    ("Çınar Haber", "Haber", "HD"), ("Işık Gündem", "Haber", "FHD"),
    ("Şimşek Spor", "Spor", "FHD"), ("Kaplan Spor", "Spor", "HD"), ("Rüzgar Spor", "Spor", "FHD"),
    ("Fırtına Spor", "Spor", "HD"),
    ("Karınca Belgesel", "Belgesel", "FHD"), ("Atlas Keşif", "Belgesel", "HD"), ("Yaban Belgesel", "Belgesel", "FHD"),
    ("Sedef Müzik", "Müzik", "HD"), ("Nağme TV", "Müzik", "HD"), ("Akord TV", "Müzik", "FHD"),
    ("Lale Çocuk", "Çocuk", "HD"), ("Pamuk Masal", "Çocuk", "HD"), ("Cıvıl Çocuk", "Çocuk", "FHD"),
    ("Orkide Dünya", "Dünya", "HD"), ("Küre Haber", "Dünya", "HD"), ("Mercan TV", "Dünya", "FHD"),
    ("Ala Yerel TV", "Yerel", "HD"), ("Bozkır TV", "Yerel", "HD"), ("Kıyı TV", "Yerel", "HD"),
    ("Yayla Kanalı", "Yerel", "HD"), ("Vadi TV", "Yerel", "HD"), ("Dere TV", "Yerel", "HD"),
]
PALETTE = [("0xFFB91C1C", "0xFFEF4444"), ("0xFF1E3A8A", "0xFF3B82F6"), ("0xFF166534", "0xFF22C55E"), ("0xFF6B21A8", "0xFFA855F7"),
           ("0xFF9A3412", "0xFFF97316"), ("0xFF0C4A6E", "0xFF0EA5E9"), ("0xFF134E4A", "0xFF14B8A6"), ("0xFF9F1239", "0xFFE11D48")]


def main():
    out = []
    for i, (name, cat, badge) in enumerate(CH, 1):
        c1, c2 = PALETTE[i % len(PALETTE)]
        words = name.upper().split()
        slug = "".join(ch for ch in name.lower() if ch.isascii() and ch.isalnum()) or f"k{i}"
        first, second = (BBB, TOS) if i % 2 else (TOS, BBB)
        out.append({"id": f"ch_demo_{i:02d}", "number": i, "name": name, "logoText": "\n".join(words[:2]), "c1": c1, "c2": c2,
                    "cat": cat, "badge": badge, "logoUrl": "",
                    "sources": [{"label": "Ana Yayın", "url": first, "type": "HLS"},
                                {"label": "Alternatif Akış 2", "url": second, "type": "HLS"}]})
    here = os.path.dirname(os.path.abspath(__file__))
    path = os.path.join(here, "demo-channels.plain.json")
    json.dump({"version": "demo", "updatedAt": "", "description": "Uydurma örnek liste (ekran görüntüleri için)", "channels": out},
              open(path, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    print(len(out), "kanal ->", path)


if __name__ == "__main__":
    main()
