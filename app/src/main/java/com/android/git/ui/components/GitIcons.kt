package com.android.git.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import io.github.lyxnx.compose.ui.tablericons.TablerIcons
import io.github.lyxnx.compose.ui.tablericons.outline.AlertCircle
import io.github.lyxnx.compose.ui.tablericons.outline.AlertTriangle
import io.github.lyxnx.compose.ui.tablericons.outline.Archive
import io.github.lyxnx.compose.ui.tablericons.outline.ArrowBackUp
import io.github.lyxnx.compose.ui.tablericons.outline.ArrowDown
import io.github.lyxnx.compose.ui.tablericons.outline.ArrowLeft
import io.github.lyxnx.compose.ui.tablericons.outline.ArrowUp
import io.github.lyxnx.compose.ui.tablericons.outline.ArrowsExchange
import io.github.lyxnx.compose.ui.tablericons.outline.BrandAndroid
import io.github.lyxnx.compose.ui.tablericons.outline.BrandGithub
import io.github.lyxnx.compose.ui.tablericons.outline.Calendar
import io.github.lyxnx.compose.ui.tablericons.outline.Check
import io.github.lyxnx.compose.ui.tablericons.outline.ChevronDown
import io.github.lyxnx.compose.ui.tablericons.outline.ChevronRight
import io.github.lyxnx.compose.ui.tablericons.outline.CircleCheck
import io.github.lyxnx.compose.ui.tablericons.outline.CirclePlay
import io.github.lyxnx.compose.ui.tablericons.outline.CirclePlus
import io.github.lyxnx.compose.ui.tablericons.outline.Clipboard
import io.github.lyxnx.compose.ui.tablericons.outline.Cloud
import io.github.lyxnx.compose.ui.tablericons.outline.CloudCheck
import io.github.lyxnx.compose.ui.tablericons.outline.CloudDownload
import io.github.lyxnx.compose.ui.tablericons.outline.CloudOff
import io.github.lyxnx.compose.ui.tablericons.outline.CloudUpload
import io.github.lyxnx.compose.ui.tablericons.outline.Code
import io.github.lyxnx.compose.ui.tablericons.outline.Copy
import io.github.lyxnx.compose.ui.tablericons.outline.Deselect
import io.github.lyxnx.compose.ui.tablericons.outline.DeviceDesktop
import io.github.lyxnx.compose.ui.tablericons.outline.DeviceFloppy
import io.github.lyxnx.compose.ui.tablericons.outline.DeviceMobile
import io.github.lyxnx.compose.ui.tablericons.outline.DotsVertical
import io.github.lyxnx.compose.ui.tablericons.outline.Download
import io.github.lyxnx.compose.ui.tablericons.outline.Edit
import io.github.lyxnx.compose.ui.tablericons.outline.Eye
import io.github.lyxnx.compose.ui.tablericons.outline.EyeOff
import io.github.lyxnx.compose.ui.tablericons.outline.Filter
import io.github.lyxnx.compose.ui.tablericons.outline.Filter2
import io.github.lyxnx.compose.ui.tablericons.outline.Folder
import io.github.lyxnx.compose.ui.tablericons.outline.FolderOpen
import io.github.lyxnx.compose.ui.tablericons.outline.FolderPlus
import io.github.lyxnx.compose.ui.tablericons.outline.GitBranch
import io.github.lyxnx.compose.ui.tablericons.outline.GitCommit
import io.github.lyxnx.compose.ui.tablericons.outline.GitMerge
import io.github.lyxnx.compose.ui.tablericons.outline.History
import io.github.lyxnx.compose.ui.tablericons.outline.InfoCircle
import io.github.lyxnx.compose.ui.tablericons.outline.Key
import io.github.lyxnx.compose.ui.tablericons.outline.Link
import io.github.lyxnx.compose.ui.tablericons.outline.Lock
import io.github.lyxnx.compose.ui.tablericons.outline.Mail
import io.github.lyxnx.compose.ui.tablericons.outline.Maximize
import io.github.lyxnx.compose.ui.tablericons.outline.Password
import io.github.lyxnx.compose.ui.tablericons.outline.Pencil
import io.github.lyxnx.compose.ui.tablericons.outline.PlayerPlay
import io.github.lyxnx.compose.ui.tablericons.outline.PlayerStop
import io.github.lyxnx.compose.ui.tablericons.outline.PlaylistAdd
import io.github.lyxnx.compose.ui.tablericons.outline.Plus
import io.github.lyxnx.compose.ui.tablericons.outline.Refresh
import io.github.lyxnx.compose.ui.tablericons.outline.Restore
import io.github.lyxnx.compose.ui.tablericons.outline.Search
import io.github.lyxnx.compose.ui.tablericons.outline.SelectAll
import io.github.lyxnx.compose.ui.tablericons.outline.Settings2
import io.github.lyxnx.compose.ui.tablericons.outline.ShieldCheck
import io.github.lyxnx.compose.ui.tablericons.outline.Sparkles
import io.github.lyxnx.compose.ui.tablericons.outline.Tag
import io.github.lyxnx.compose.ui.tablericons.outline.Trash
import io.github.lyxnx.compose.ui.tablericons.outline.Upload
import io.github.lyxnx.compose.ui.tablericons.outline.User
import io.github.lyxnx.compose.ui.tablericons.outline.Wand
import io.github.lyxnx.compose.ui.tablericons.outline.World
import io.github.lyxnx.compose.ui.tablericons.outline.X

/** Tabler outline glyphs keep the same visual weight across every Git screen. */
object GitIcons {
    object Default {
        val Add: ImageVector get() = TablerIcons.Outline.Plus
        val AddCircleOutline: ImageVector get() = TablerIcons.Outline.CirclePlus
        val Android: ImageVector get() = TablerIcons.Outline.BrandAndroid
        val Archive: ImageVector get() = TablerIcons.Outline.Archive
        val ArrowBack: ImageVector get() = TablerIcons.Outline.ArrowLeft
        val ArrowDownward: ImageVector get() = TablerIcons.Outline.ArrowDown
        val ArrowDropDown: ImageVector get() = TablerIcons.Outline.ChevronDown
        val ArrowUpward: ImageVector get() = TablerIcons.Outline.ArrowUp
        val AutoAwesome: ImageVector get() = TablerIcons.Outline.Sparkles
        val AutoFixHigh: ImageVector get() = TablerIcons.Outline.Wand
        val CalendarToday: ImageVector get() = TablerIcons.Outline.Calendar
        val CallMerge: ImageVector get() = TablerIcons.Outline.GitMerge
        val CallSplit: ImageVector get() = TablerIcons.Outline.GitBranch
        val Check: ImageVector get() = TablerIcons.Outline.Check
        val CheckCircle: ImageVector get() = TablerIcons.Outline.CircleCheck
        val CheckCircleOutline: ImageVector get() = TablerIcons.Outline.CircleCheck
        val ChevronRight: ImageVector get() = TablerIcons.Outline.ChevronRight
        val Close: ImageVector get() = TablerIcons.Outline.X
        val Cloud: ImageVector get() = TablerIcons.Outline.Cloud
        val CloudDone: ImageVector get() = TablerIcons.Outline.CloudCheck
        val CloudDownload: ImageVector get() = TablerIcons.Outline.CloudDownload
        val CloudOff: ImageVector get() = TablerIcons.Outline.CloudOff
        val CloudUpload: ImageVector get() = TablerIcons.Outline.CloudUpload
        val Code: ImageVector get() = TablerIcons.Outline.Code
        val Commit: ImageVector get() = TablerIcons.Outline.GitCommit
        val Computer: ImageVector get() = TablerIcons.Outline.DeviceDesktop
        val ContentCopy: ImageVector get() = TablerIcons.Outline.Copy
        val ContentPaste: ImageVector get() = TablerIcons.Outline.Clipboard
        val Create: ImageVector get() = TablerIcons.Outline.Pencil
        val CreateNewFolder: ImageVector get() = TablerIcons.Outline.FolderPlus
        val Delete: ImageVector get() = TablerIcons.Outline.Trash
        val DeleteOutline: ImageVector get() = TablerIcons.Outline.Trash
        val Deselect: ImageVector get() = TablerIcons.Outline.Deselect
        val Download: ImageVector get() = TablerIcons.Outline.Download
        val Edit: ImageVector get() = TablerIcons.Outline.Edit
        val Email: ImageVector get() = TablerIcons.Outline.Mail
        val Error: ImageVector get() = TablerIcons.Outline.AlertCircle
        val FilterAlt: ImageVector get() = TablerIcons.Outline.Filter
        val FilterList: ImageVector get() = TablerIcons.Outline.Filter2
        val Folder: ImageVector get() = TablerIcons.Outline.Folder
        val FolderOpen: ImageVector get() = TablerIcons.Outline.FolderOpen
        val Fullscreen: ImageVector get() = TablerIcons.Outline.Maximize
        val History: ImageVector get() = TablerIcons.Outline.History
        val Info: ImageVector get() = TablerIcons.Outline.InfoCircle
        val Key: ImageVector get() = TablerIcons.Outline.Key
        val Language: ImageVector get() = TablerIcons.Outline.World
        val Link: ImageVector get() = TablerIcons.Outline.Link
        val Lock: ImageVector get() = TablerIcons.Outline.Lock
        val MoreVert: ImageVector get() = TablerIcons.Outline.DotsVertical
        val Password: ImageVector get() = TablerIcons.Outline.Password
        val Person: ImageVector get() = TablerIcons.Outline.User
        val PlayArrow: ImageVector get() = TablerIcons.Outline.PlayerPlay
        val PlayCircle: ImageVector get() = TablerIcons.Outline.CirclePlay
        val PlaylistAdd: ImageVector get() = TablerIcons.Outline.PlaylistAdd
        val Public: ImageVector get() = TablerIcons.Outline.World
        val Refresh: ImageVector get() = TablerIcons.Outline.Refresh
        val Restore: ImageVector get() = TablerIcons.Outline.Restore
        val Save: ImageVector get() = TablerIcons.Outline.DeviceFloppy
        val Search: ImageVector get() = TablerIcons.Outline.Search
        val SelectAll: ImageVector get() = TablerIcons.Outline.SelectAll
        val Settings: ImageVector get() = TablerIcons.Outline.Settings2
        val Smartphone: ImageVector get() = TablerIcons.Outline.DeviceMobile
        val Source: ImageVector get() = TablerIcons.Outline.BrandGithub
        val Stop: ImageVector get() = TablerIcons.Outline.PlayerStop
        val Sync: ImageVector get() = TablerIcons.Outline.Refresh
        val SyncAlt: ImageVector get() = TablerIcons.Outline.ArrowsExchange
        val SystemUpdate: ImageVector get() = TablerIcons.Outline.Download
        val Tag: ImageVector get() = TablerIcons.Outline.Tag
        val Undo: ImageVector get() = TablerIcons.Outline.ArrowBackUp
        val Upload: ImageVector get() = TablerIcons.Outline.Upload
        val VerifiedUser: ImageVector get() = TablerIcons.Outline.ShieldCheck
        val Visibility: ImageVector get() = TablerIcons.Outline.Eye
        val VisibilityOff: ImageVector get() = TablerIcons.Outline.EyeOff
        val VpnKey: ImageVector get() = TablerIcons.Outline.Key
        val Warning: ImageVector get() = TablerIcons.Outline.AlertTriangle
    }

    object AutoMirrored {
        object Filled {
            val ArrowBack: ImageVector get() = TablerIcons.Outline.ArrowLeft
            val CallMerge: ImageVector get() = TablerIcons.Outline.GitMerge
            val CallSplit: ImageVector get() = TablerIcons.Outline.GitBranch
            val PlaylistAdd: ImageVector get() = TablerIcons.Outline.PlaylistAdd
            val Undo: ImageVector get() = TablerIcons.Outline.ArrowBackUp
        }
    }
}
