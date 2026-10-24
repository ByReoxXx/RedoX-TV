package com.example.chromecasttv

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * RedoX "Çift ton" simge seti (24x24 ızgara): 1,9 birimlik yuvarlak uçlu çizgi + kapalı gövdelerde yumuşak dolgu.
 * Renk, kullanım yerinde `Icon(tint = ...)` ile verilir; dolgu o rengin %22'si olarak görünür.
 * `Star` yumuşak dolgulu, `StarFilled` dolu (favori açık), `Pause` / `PlayArrow` / `Stop` dolu çizilir.
 * Simge çizimleri `lab/icons_b.py` kaynağından üretilir.
 */
object RedoXIcons {
    val Refresh: ImageVector by lazy {
        duo("Refresh") {
            line("M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8")
            line("M21 3v5h-5")
            line("M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16")
            line("M8 16H3v5")
        }
    }

    val Star: ImageVector by lazy {
        duo("Star") {
            body("M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26Z")
        }
    }

    val StarFilled: ImageVector by lazy {
        duo("StarFilled") {
            solid("M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26Z")
        }
    }

    val StarBorder: ImageVector by lazy {
        duo("StarBorder") {
            line("M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26Z")
        }
    }

    val Clear: ImageVector by lazy {
        duo("Clear") {
            line("M18 6 6 18")
            line("m6 6 12 12")
        }
    }

    val Close: ImageVector by lazy {
        duo("Close") {
            line("M18 6 6 18")
            line("m6 6 12 12")
        }
    }

    val DeleteOutline: ImageVector by lazy {
        duo("DeleteOutline") {
            body("M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6")
            line("M3 6h18")
            line("M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2")
            line("M10 11v6")
            line("M14 11v6")
        }
    }

    val LiveTv: ImageVector by lazy {
        duo("LiveTv") {
            body("M4 7H20a2 2 0 0 1 2 2V20a2 2 0 0 1 -2 2H4a2 2 0 0 1 -2 -2V9a2 2 0 0 1 2 -2Z")
            line("M17 2 12 7 7 2")
            solid("M10.2 11.2L15.4 14.5L10.2 17.8Z")
        }
    }

    val Tv: ImageVector by lazy {
        duo("Tv") {
            body("M4 7H20a2 2 0 0 1 2 2V20a2 2 0 0 1 -2 2H4a2 2 0 0 1 -2 -2V9a2 2 0 0 1 2 -2Z")
            line("M17 2 12 7 7 2")
        }
    }

    val GridView: ImageVector by lazy {
        duo("GridView") {
            body("M4.4 3H8.6a1.4 1.4 0 0 1 1.4 1.4V8.6a1.4 1.4 0 0 1 -1.4 1.4H4.4a1.4 1.4 0 0 1 -1.4 -1.4V4.4a1.4 1.4 0 0 1 1.4 -1.4Z")
            body("M15.4 3H19.6a1.4 1.4 0 0 1 1.4 1.4V8.6a1.4 1.4 0 0 1 -1.4 1.4H15.4a1.4 1.4 0 0 1 -1.4 -1.4V4.4a1.4 1.4 0 0 1 1.4 -1.4Z")
            body("M15.4 14H19.6a1.4 1.4 0 0 1 1.4 1.4V19.6a1.4 1.4 0 0 1 -1.4 1.4H15.4a1.4 1.4 0 0 1 -1.4 -1.4V15.4a1.4 1.4 0 0 1 1.4 -1.4Z")
            body("M4.4 14H8.6a1.4 1.4 0 0 1 1.4 1.4V19.6a1.4 1.4 0 0 1 -1.4 1.4H4.4a1.4 1.4 0 0 1 -1.4 -1.4V15.4a1.4 1.4 0 0 1 1.4 -1.4Z")
        }
    }

    val PlayArrow: ImageVector by lazy {
        duo("PlayArrow") {
            solid("M6 3L20 12L6 21Z")
        }
    }

    val Add: ImageVector by lazy {
        duo("Add") {
            line("M5 12h14")
            line("M12 5v14")
        }
    }

    val Remove: ImageVector by lazy {
        duo("Remove") {
            line("M5 12h14")
        }
    }

    val Tag: ImageVector by lazy {
        duo("Tag") {
            line("M4 9h16")
            line("M4 15h16")
            line("M10 3 8 21")
            line("M16 3l-2 18")
        }
    }

    val SettingsRemote: ImageVector by lazy {
        duo("SettingsRemote") {
            body("M10 8H14a3 3 0 0 1 3 3V19a3 3 0 0 1 -3 3H10a3 3 0 0 1 -3 -3V11a3 3 0 0 1 3 -3Z")
            dot(12f, 13f, 1.3f)
            line("M10 18h4")
            line("M9.6 5.3a3.4 3.4 0 0 1 4.8 0")
            line("M7.6 3.2a6.2 6.2 0 0 1 8.8 0")
        }
    }

    val Settings: ImageVector by lazy {
        duo("Settings") {
            line("M21 4h-7")
            line("M10 4H3")
            line("M21 12h-9")
            line("M8 12H3")
            line("M21 20h-5")
            line("M12 20H3")
            line("M14 2v4")
            line("M8 10v4")
            line("M16 18v4")
        }
    }

    val Tune: ImageVector by lazy {
        duo("Tune") {
            line("M4 21v-7")
            line("M4 10V3")
            line("M12 21v-9")
            line("M12 8V3")
            line("M20 21v-5")
            line("M20 12V3")
            line("M2 14h4")
            line("M10 8h4")
            line("M18 16h4")
        }
    }

    val Search: ImageVector by lazy {
        duo("Search") {
            body("M3 11a8 8 0 1 0 16 0a8 8 0 1 0 -16 0Z")
            line("m21 21-4.3-4.3")
        }
    }

    val FolderOpen: ImageVector by lazy {
        duo("FolderOpen") {
            body("M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z")
        }
    }

    val VolumeUp: ImageVector by lazy {
        duo("VolumeUp") {
            body("M11 5L6 9L2 9L2 15L6 15L11 19Z")
            line("M15.54 8.46a5 5 0 0 1 0 7.07")
            line("M19.07 4.93a10 10 0 0 1 0 14.14")
        }
    }

    val VolumeDown: ImageVector by lazy {
        duo("VolumeDown") {
            body("M11 5L6 9L2 9L2 15L6 15L11 19Z")
            line("M15.54 8.46a5 5 0 0 1 0 7.07")
        }
    }

    val VolumeOff: ImageVector by lazy {
        duo("VolumeOff") {
            body("M11 5L6 9L2 9L2 15L6 15L11 19Z")
            line("M22 9l-6 6")
            line("M16 9l6 6")
        }
    }

    val Verified: ImageVector by lazy {
        duo("Verified") {
            body("M3.85 8.62a4 4 0 0 1 4.78-4.77 4 4 0 0 1 6.74 0 4 4 0 0 1 4.78 4.78 4 4 0 0 1 0 6.74 4 4 0 0 1-4.77 4.78 4 4 0 0 1-6.75 0 4 4 0 0 1-4.78-4.77 4 4 0 0 1 0-6.76Z")
            line("m9 12 2 2 4-4")
        }
    }

    val NotificationsActive: ImageVector by lazy {
        duo("NotificationsActive") {
            body("M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9")
            line("M10.3 21a1.94 1.94 0 0 0 3.4 0")
            line("M2 8c0-2.2.7-4.3 2-6")
            line("M22 8a10 10 0 0 0-2-6")
        }
    }

    val Lock: ImageVector by lazy {
        duo("Lock") {
            body("M5 11H19a2 2 0 0 1 2 2V20a2 2 0 0 1 -2 2H5a2 2 0 0 1 -2 -2V13a2 2 0 0 1 2 -2Z")
            line("M7 11V7a5 5 0 0 1 10 0v4")
        }
    }

    val LockOpen: ImageVector by lazy {
        duo("LockOpen") {
            body("M5 11H19a2 2 0 0 1 2 2V20a2 2 0 0 1 -2 2H5a2 2 0 0 1 -2 -2V13a2 2 0 0 1 2 -2Z")
            line("M7 11V7a5 5 0 0 1 9.9-1")
        }
    }

    val CloudDownload: ImageVector by lazy {
        duo("CloudDownload") {
            line("M12 13v8l-4-4")
            line("m12 21 4-4")
            line("M4.393 15.269A7 7 0 1 1 15.71 8h1.79a4.5 4.5 0 0 1 2.436 8.284")
        }
    }

    val CalendarMonth: ImageVector by lazy {
        duo("CalendarMonth") {
            body("M5 4H19a2 2 0 0 1 2 2V20a2 2 0 0 1 -2 2H5a2 2 0 0 1 -2 -2V6a2 2 0 0 1 2 -2Z")
            line("M16 2v4")
            line("M8 2v4")
            line("M3 10h18")
            line("M8 14h.01")
            line("M12 14h.01")
            line("M16 14h.01")
            line("M8 18h.01")
            line("M12 18h.01")
            line("M16 18h.01")
        }
    }

    val UploadFile: ImageVector by lazy {
        duo("UploadFile") {
            body("M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z")
            line("M14 2v4a2 2 0 0 0 2 2h4")
            line("M12 18v-6")
            line("m9 15 3-3 3 3")
        }
    }

    val InsertDriveFile: ImageVector by lazy {
        duo("InsertDriveFile") {
            body("M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z")
            line("M14 2v4a2 2 0 0 0 2 2h4")
        }
    }

    val Public: ImageVector by lazy {
        duo("Public") {
            body("M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0Z")
            line("M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20")
            line("M2 12h20")
        }
    }

    val Language: ImageVector by lazy {
        duo("Language") {
            body("M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0Z")
            line("M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20")
            line("M2 12h20")
        }
    }

    val Pause: ImageVector by lazy {
        duo("Pause") {
            solid("M15.2 4H16.8a1.2 1.2 0 0 1 1.2 1.2V18.8a1.2 1.2 0 0 1 -1.2 1.2H15.2a1.2 1.2 0 0 1 -1.2 -1.2V5.2a1.2 1.2 0 0 1 1.2 -1.2Z")
            solid("M7.2 4H8.8a1.2 1.2 0 0 1 1.2 1.2V18.8a1.2 1.2 0 0 1 -1.2 1.2H7.2a1.2 1.2 0 0 1 -1.2 -1.2V5.2a1.2 1.2 0 0 1 1.2 -1.2Z")
        }
    }

    val Info: ImageVector by lazy {
        duo("Info") {
            body("M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0Z")
            line("M12 16v-4")
            line("M12 8h.01")
        }
    }

    val Edit: ImageVector by lazy {
        duo("Edit") {
            body("M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z")
            line("m15 5 4 4")
        }
    }

    val CheckCircle: ImageVector by lazy {
        duo("CheckCircle") {
            body("M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0Z")
            line("m9 12 2 2 4-4")
        }
    }

    val SkipPrevious: ImageVector by lazy {
        duo("SkipPrevious") {
            body("M19 20L9 12L19 4Z")
            line("M5 19V5")
        }
    }

    val SkipNext: ImageVector by lazy {
        duo("SkipNext") {
            body("M5 4L15 12L5 20Z")
            line("M19 5v14")
        }
    }

    val Sensors: ImageVector by lazy {
        duo("Sensors") {
            line("M4.9 19.1C1 15.2 1 8.8 4.9 4.9")
            line("M7.8 16.2c-2.3-2.3-2.3-6.1 0-8.5")
            solid("M10 12a2 2 0 1 0 4 0a2 2 0 1 0 -4 0Z")
            line("M16.2 7.8c2.3 2.3 2.3 6.1 0 8.5")
            line("M19.1 4.9C23 8.8 23 15.1 19.1 19")
        }
    }

    val Palette: ImageVector by lazy {
        duo("Palette") {
            body("M12 22a1 1 0 0 1 0-20 10 9 0 0 1 10 9 5 5 0 0 1-5 5h-2.25a1.75 1.75 0 0 0-1.4 2.8l.3.4a1.75 1.75 0 0 1-1.4 2.8z")
            dot(13.5f, 6.5f, 1.2f)
            dot(17.5f, 10.5f, 1.2f)
            dot(6.5f, 12.5f, 1.2f)
            dot(8.5f, 7.5f, 1.2f)
        }
    }

    val History: ImageVector by lazy {
        duo("History") {
            line("M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8")
            line("M3 3v5h5")
            line("M12 7v5l4 2")
        }
    }

    val Bedtime: ImageVector by lazy {
        duo("Bedtime") {
            body("M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z")
        }
    }

    val WarningAmber: ImageVector by lazy {
        duo("WarningAmber") {
            body("m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z")
            line("M12 9v4")
            line("M12 17h.01")
        }
    }

    val SystemUpdate: ImageVector by lazy {
        duo("SystemUpdate") {
            line("M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4")
            line("M7 10l5 5 5-5")
            line("M12 15V3")
        }
    }

    val SmartDisplay: ImageVector by lazy {
        duo("SmartDisplay") {
            body("M4 3H20a2 2 0 0 1 2 2V15a2 2 0 0 1 -2 2H4a2 2 0 0 1 -2 -2V5a2 2 0 0 1 2 -2Z")
            line("M8 21h8")
            line("M12 17v4")
            solid("M10.2 7.2L14.8 10L10.2 12.8Z")
        }
    }

    val PlayCircle: ImageVector by lazy {
        duo("PlayCircle") {
            body("M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0Z")
            solid("M10 8L16 12L10 16Z")
        }
    }

    val PlayCircleFilled: ImageVector by lazy {
        duo("PlayCircleFilled") {
            body("M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0Z", 0.55f)
            solid("M10 8L16 12L10 16Z")
        }
    }

    val Mic: ImageVector by lazy {
        duo("Mic") {
            body("M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z")
            line("M19 10v2a7 7 0 0 1-14 0v-2")
            line("M12 19v3")
        }
    }

    val ManageSearch: ImageVector by lazy {
        duo("ManageSearch") {
            line("M21 6H3")
            line("M10 12H3")
            line("M10 18H3")
            body("M14 15a3 3 0 1 0 6 0a3 3 0 1 0 -6 0Z")
            line("m21 19-1.9-1.9")
        }
    }

    val Home: ImageVector by lazy {
        duo("Home") {
            body("M3 10a2 2 0 0 1 .709-1.528l7-5.999a2 2 0 0 1 2.582 0l7 5.999A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z")
            line("M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8")
        }
    }

    val FiberManualRecord: ImageVector by lazy {
        duo("FiberManualRecord") {
            line("M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0Z")
            dot(12f, 12f, 3.6f)
        }
    }

    val Category: ImageVector by lazy {
        duo("Category") {
            body("M8.3 10a.7.7 0 0 1-.626-1.079L11.4 3a.7.7 0 0 1 1.198-.043L16.3 8.9a.7.7 0 0 1-.572 1.1Z")
            body("M4 14H9a1 1 0 0 1 1 1V20a1 1 0 0 1 -1 1H4a1 1 0 0 1 -1 -1V15a1 1 0 0 1 1 -1Z")
            body("M14.0 17.5a3.5 3.5 0 1 0 7.0 0a3.5 3.5 0 1 0 -7.0 0Z")
        }
    }

    val Cast: ImageVector by lazy {
        duo("Cast") {
            line("M2 8V6a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-6")
            line("M2 12a9 9 0 0 1 8 8")
            line("M2 16a5 5 0 0 1 4 4")
            line("M2 20h.01")
        }
    }

    val Build: ImageVector by lazy {
        duo("Build") {
            body("M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z")
        }
    }

    val Bolt: ImageVector by lazy {
        duo("Bolt") {
            body("M4 14a1 1 0 0 1-.78-1.63l9.9-10.2a.5.5 0 0 1 .86.46l-1.92 6.02A1 1 0 0 0 13 10h7a1 1 0 0 1 .78 1.63l-9.9 10.2a.5.5 0 0 1-.86-.46l1.92-6.02A1 1 0 0 0 11 14z")
        }
    }

    val ArrowBack: ImageVector by lazy {
        duo("ArrowBack") {
            line("m12 19-7-7 7-7")
            line("M19 12H5")
        }
    }

    val ArrowUpward: ImageVector by lazy {
        duo("ArrowUpward") {
            line("m5 12 7-7 7 7")
            line("M12 19V5")
        }
    }

    val ArrowDownward: ImageVector by lazy {
        duo("ArrowDownward") {
            line("M12 5v14")
            line("m19 12-7 7-7-7")
        }
    }

    val SwapHoriz: ImageVector by lazy {
        duo("SwapHoriz") {
            line("M8 3 4 7l4 4")
            line("M4 7h16")
            line("m16 21 4-4-4-4")
            line("M20 17H4")
        }
    }

    val Wifi: ImageVector by lazy {
        duo("Wifi") {
            line("M12 20h.01")
            line("M2 8.82a15 15 0 0 1 20 0")
            line("M5 12.859a10 10 0 0 1 14 0")
            line("M8.5 16.429a5 5 0 0 1 7 0")
        }
    }

    val WifiOff: ImageVector by lazy {
        duo("WifiOff") {
            line("M12 20h.01")
            line("M8.5 16.429a5 5 0 0 1 7 0")
            line("M5 12.859a10 10 0 0 1 5.17-2.69")
            line("M19 12.859a10 10 0 0 0-2.007-1.523")
            line("M2 8.82a15 15 0 0 1 4.177-2.643")
            line("M22 8.82a15 15 0 0 0-11.288-3.764")
            line("m2 2 20 20")
        }
    }

    val Stop: ImageVector by lazy {
        duo("Stop") {
            solid("M8 6H16a2 2 0 0 1 2 2V16a2 2 0 0 1 -2 2H8a2 2 0 0 1 -2 -2V8a2 2 0 0 1 2 -2Z")
        }
    }

    val SportsSoccer: ImageVector by lazy {
        duo("SportsSoccer") {
            body("M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0Z")
            line("M12 7L16.5 10.3L14.8 15.5L9.2 15.5L7.5 10.3Z")
            line("M12 7V2.2")
            line("M16.5 10.3l4.4-1.5")
            line("M14.8 15.5l2.6 3.8")
            line("M9.2 15.5l-2.6 3.8")
            line("M7.5 10.3 3.1 8.8")
        }
    }

    val Schedule: ImageVector by lazy {
        duo("Schedule") {
            body("M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0Z")
            line("M12 6v6l4 2")
        }
    }

    val Save: ImageVector by lazy {
        duo("Save") {
            body("M15.2 3a2 2 0 0 1 1.4.6l3.8 3.8a2 2 0 0 1 .6 1.4V19a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z")
            line("M17 21v-7a1 1 0 0 0-1-1H8a1 1 0 0 0-1 1v7")
            line("M7 3v4a1 1 0 0 0 1 1h7")
        }
    }

    val QrCode2: ImageVector by lazy {
        duo("QrCode2") {
            body("M4 3H7a1 1 0 0 1 1 1V7a1 1 0 0 1 -1 1H4a1 1 0 0 1 -1 -1V4a1 1 0 0 1 1 -1Z")
            body("M17 3H20a1 1 0 0 1 1 1V7a1 1 0 0 1 -1 1H17a1 1 0 0 1 -1 -1V4a1 1 0 0 1 1 -1Z")
            body("M4 16H7a1 1 0 0 1 1 1V20a1 1 0 0 1 -1 1H4a1 1 0 0 1 -1 -1V17a1 1 0 0 1 1 -1Z")
            line("M21 16h-3a2 2 0 0 0-2 2v3")
            line("M21 21v.01")
            line("M12 7v3a2 2 0 0 1-2 2H7")
            line("M3 12h.01")
            line("M12 3h.01")
            line("M12 16v.01")
            line("M16 12h1")
            line("M21 12v.01")
            line("M12 21v-1")
        }
    }

    val PictureInPictureAlt: ImageVector by lazy {
        duo("PictureInPictureAlt") {
            line("M21 9V6a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v10c0 1.1.9 2 2 2h4")
            body("M14 13H20a2 2 0 0 1 2 2V18a2 2 0 0 1 -2 2H14a2 2 0 0 1 -2 -2V15a2 2 0 0 1 2 -2Z")
        }
    }

    val Newspaper: ImageVector by lazy {
        duo("Newspaper") {
            line("M4 22h16a2 2 0 0 0 2-2V4a2 2 0 0 0-2-2H8a2 2 0 0 0-2 2v16a2 2 0 0 1-4 0v-9a2 2 0 0 1 2-2h2")
            line("M15 18h-5")
            line("M18 14h-8")
            body("M11 6H17a1 1 0 0 1 1 1V9a1 1 0 0 1 -1 1H11a1 1 0 0 1 -1 -1V7a1 1 0 0 1 1 -1Z")
        }
    }

    val MusicNote: ImageVector by lazy {
        duo("MusicNote") {
            line("M9 18V5l12-2v13")
            body("M3 18a3 3 0 1 0 6 0a3 3 0 1 0 -6 0Z")
            body("M15 16a3 3 0 1 0 6 0a3 3 0 1 0 -6 0Z")
        }
    }

    val Movie: ImageVector by lazy {
        duo("Movie") {
            body("M4.2 2H19.8a2.2 2.2 0 0 1 2.2 2.2V19.8a2.2 2.2 0 0 1 -2.2 2.2H4.2a2.2 2.2 0 0 1 -2.2 -2.2V4.2a2.2 2.2 0 0 1 2.2 -2.2Z")
            line("M7 2v20")
            line("M17 2v20")
            line("M2 12h20")
            line("M2 7h5")
            line("M2 17h5")
            line("M17 17h5")
            line("M17 7h5")
        }
    }

    val MoreHoriz: ImageVector by lazy {
        duo("MoreHoriz") {
            dot(12f, 12f, 1.7f)
            dot(19f, 12f, 1.7f)
            dot(5f, 12f, 1.7f)
        }
    }

    val Menu: ImageVector by lazy {
        duo("Menu") {
            line("M4 12h16")
            line("M4 6h16")
            line("M4 18h16")
        }
    }

    val KeyboardArrowUp: ImageVector by lazy {
        duo("KeyboardArrowUp") {
            line("m18 15-6-6-6 6")
        }
    }

    val KeyboardArrowDown: ImageVector by lazy {
        duo("KeyboardArrowDown") {
            line("m6 9 6 6 6-6")
        }
    }

    val KeyboardArrowLeft: ImageVector by lazy {
        duo("KeyboardArrowLeft") {
            line("m15 18-6-6 6-6")
        }
    }

    val KeyboardArrowRight: ImageVector by lazy {
        duo("KeyboardArrowRight") {
            line("m9 18 6-6-6-6")
        }
    }

    val ExpandMore: ImageVector by lazy {
        duo("ExpandMore") {
            line("m6 9 6 6 6-6")
        }
    }

    val HighQuality: ImageVector by lazy {
        duo("HighQuality") {
            body("M5 5H19a2 2 0 0 1 2 2V17a2 2 0 0 1 -2 2H5a2 2 0 0 1 -2 -2V7a2 2 0 0 1 2 -2Z")
            line("M7.5 9.5v5")
            line("M7.5 12h3")
            line("M10.5 9.5v5")
            line("M13.5 9.5v5h1.5a2.5 2.5 0 0 0 0-5z")
        }
    }

    val Fullscreen: ImageVector by lazy {
        duo("Fullscreen") {
            line("M8 3H5a2 2 0 0 0-2 2v3")
            line("M21 8V5a2 2 0 0 0-2-2h-3")
            line("M3 16v3a2 2 0 0 0 2 2h3")
            line("M16 21h3a2 2 0 0 0 2-2v-3")
        }
    }

    val FilterList: ImageVector by lazy {
        duo("FilterList") {
            line("M3 6h18")
            line("M7 12h10")
            line("M10 18h4")
        }
    }

    val Dns: ImageVector by lazy {
        duo("Dns") {
            body("M4 2H20a2 2 0 0 1 2 2V8a2 2 0 0 1 -2 2H4a2 2 0 0 1 -2 -2V4a2 2 0 0 1 2 -2Z")
            body("M4 14H20a2 2 0 0 1 2 2V20a2 2 0 0 1 -2 2H4a2 2 0 0 1 -2 -2V16a2 2 0 0 1 2 -2Z")
            line("M6 6h.01")
            line("M6 18h.01")
        }
    }

    val ClosedCaption: ImageVector by lazy {
        duo("ClosedCaption") {
            body("M5 5H19a2 2 0 0 1 2 2V17a2 2 0 0 1 -2 2H5a2 2 0 0 1 -2 -2V7a2 2 0 0 1 2 -2Z")
            line("M7 15h4")
            line("M15 15h2")
            line("M7 11h2")
            line("M13 11h4")
        }
    }

    val ChildCare: ImageVector by lazy {
        duo("ChildCare") {
            line("M9 12h.01")
            line("M15 12h.01")
            line("M10 16c.5.3 1.2.5 2 .5s1.5-.2 2-.5")
            line("M19 6.3a9 9 0 0 1 1.8 3.9 2 2 0 0 1 0 3.6 9 9 0 0 1-17.6 0 2 2 0 0 1 0-3.6A9 9 0 0 1 12 3c2 0 3.5 1.1 3.5 2.5s-.9 2.5-2 2.5c-.8 0-1.5-.4-1.5-1")
        }
    }

    val AspectRatio: ImageVector by lazy {
        duo("AspectRatio") {
            body("M4 6H20a2 2 0 0 1 2 2V16a2 2 0 0 1 -2 2H4a2 2 0 0 1 -2 -2V8a2 2 0 0 1 2 -2Z")
            line("M6 14v-3h3")
            line("M18 10v3h-3")
        }
    }

    val VisibilityOff: ImageVector by lazy {
        duo("VisibilityOff") {
            line("M9.88 9.88a3 3 0 1 0 4.24 4.24")
            line("M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68")
            line("M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61")
            line("m2 2 20 20")
        }
    }
}

private const val STROKE = 1.9f

private class DuoIconBuilder(name: String) {
    private val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    )
    private val ink = SolidColor(Color.Black)

    private fun nodes(d: String) = PathParser().parsePathString(d).toNodes()

    /** Kapalı gövde: yumuşak dolgu + çizgi. */
    fun body(d: String, fillAlpha: Float = 0.22f) {
        builder.addPath(
            pathData = nodes(d),
            fill = ink,
            fillAlpha = fillAlpha,
            stroke = ink,
            strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }

    /** Dolu gövde (oynat, duraklat, favori açık...). */
    fun solid(d: String) = body(d, 0.9f)

    /** Açık çizgi. */
    fun line(d: String) {
        builder.addPath(
            pathData = nodes(d),
            fill = null,
            stroke = ink,
            strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }

    /** Dolu nokta. */
    fun dot(cx: Float, cy: Float, r: Float) {
        builder.addPath(
            pathData = nodes("M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0Z"),
            fill = ink,
            stroke = null
        )
    }

    fun build(): ImageVector = builder.build()
}

private fun duo(name: String, draw: DuoIconBuilder.() -> Unit): ImageVector {
    val b = DuoIconBuilder(name)
    b.draw()
    return b.build()
}
