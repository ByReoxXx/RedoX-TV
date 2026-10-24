#!/usr/bin/env python3
"""Emülatörde ekran öğelerini metne / açıklamaya göre bulup dokunur (uiautomator dump ile).

Kullanım:
  ui.py tap "Canlı TV"      -> metni (ya da content-desc'i) "Canlı TV" içeren ilk öğeye dokunur
  ui.py has "Canlı TV"      -> öğe varsa 0, yoksa 1 döner
"""
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET


def adb(*args):
    return subprocess.run(["adb", *args], capture_output=True, text=True).stdout


def dump():
    for _ in range(4):
        adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
        xml = adb("exec-out", "cat", "/sdcard/ui.xml")
        if "<hierarchy" in xml:
            return xml
        time.sleep(1.5)
    return ""


def find(label):
    xml = dump()
    if not xml:
        return None
    root = ET.fromstring(xml[xml.index("<hierarchy"):])
    for node in root.iter("node"):
        for attr in ("text", "content-desc"):
            value = node.get(attr, "")
            if value and label in value:
                m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.get("bounds", ""))
                if m:
                    x0, y0, x1, y1 = map(int, m.groups())
                    return (x0 + x1) // 2, (y0 + y1) // 2
    return None


if __name__ == "__main__":
    action, label = sys.argv[1], sys.argv[2]
    pos = find(label)
    if action == "has":
        sys.exit(0 if pos else 1)
    if not pos:
        print("bulunamadi:", label)
        sys.exit(1)
    adb("shell", "input", "tap", str(pos[0]), str(pos[1]))
    print("dokunuldu:", label, pos)
