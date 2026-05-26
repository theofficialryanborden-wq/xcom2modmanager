package com.xcom2modmanager

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipInputStream

class MainActivity : Activity() {
    private lateinit var store: ModStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = ModStore(this)
        render()
    }

    @Deprecated("Deprecated by Android, but keeps this starter app dependency-free.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) return

        when (requestCode) {
            REQUEST_IMPORT_MOD -> importMod(data.data!!, data.flags)
            REQUEST_PICK_DRIVE_FOLDER -> rememberDriveFolder(data.data!!, data.flags)
        }
    }

    private fun render() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(XCOM_DARK)
            isFillViewport = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(28))
        }

        content.addView(header())
        content.addView(modActionsPanel())
        content.addView(installedModsPanel())
        content.addView(drivePanel())
        content.addView(troubleshootingPanel())
        content.addView(cheatsPanel())
        content.addView(inGameMenuPanel())

        scroll.addView(content)
        setContentView(scroll)
    }

    private fun header(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, 0, 0, dp(14))

        addView(TextView(context).apply {
            text = "XCOM 2 MOBILE MOD MANAGER"
            setTextColor(XCOM_CYAN)
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.06f
        })
        addView(TextView(context).apply {
            text = "Safe starter app for importing, tracking, enabling, exporting, and testing mod files for XCOM 2 Collection on Android."
            setTextColor(XCOM_MUTED)
            textSize = 14f
            setPadding(0, dp(8), 0, 0)
        })
    }

    private fun modActionsPanel(): View = card("MOD OPERATIONS").apply {
        addView(primaryButton("Import mod ZIP into manager") {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/zip", "application/octet-stream"))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
            startActivityForResult(intent, REQUEST_IMPORT_MOD)
        })

        addView(secondaryButton("Download online mods from Nexus Mods") {
            openUri(Uri.parse(NEXUS_MODS_URL))
        })

        addView(helpText("Important: importing a ZIP only copies it into this manager. To make a PC mod usable, export enabled mods as extracted folders into the game-visible layout you use on Android."))
        addView(helpText("Nexus Mods downloads usually require you to sign in and download through their site. Download a mod ZIP first, then return here and tap Import."))
    }

    private fun installedModsPanel(): View = card("IMPORTED MODS").apply {
        val mods = store.loadMods()
        if (mods.isEmpty()) {
            addView(helpText("No mods imported yet. Tap Import mod ZIP into manager to copy one into this app."))
        } else {
            addView(helpText("These mods are stored in this manager only. Enabled means they will be included in exports; it does not mean XCOM has loaded them."))
            mods.forEach { mod ->
                addView(modRow(mod))
            }
            addView(secondaryButton("Export enabled-mod list locally") {
                val file = writeEnabledManifest()
                showMessage("Manifest exported", "Saved enabled mod list to:\n${file.absolutePath}")
            })
        }
    }

    private fun drivePanel(): View = card("GOOGLE DRIVE SYNC WORKAROUND").apply {
        val savedFolder = store.loadDriveTreeUri()
        addView(helpText("Android apps cannot force XCOM 2 Collection to show its Google Drive sync prompt. This app can export enabled mods to a Drive or device folder in PC-style XCOM layouts, then open XCOM so you can test the sync/install path you already know works."))

        addView(secondaryButton(if (savedFolder == null) "Choose Google Drive folder" else "Change Google Drive folder") {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                )
            }
            startActivityForResult(intent, REQUEST_PICK_DRIVE_FOLDER)
        })

        if (savedFolder != null) {
            addView(helpText("Drive folder selected: $savedFolder"))
            addView(primaryButton("Extract enabled mods to WOTC layout") {
                writeExtractedEnabledModsToDrive(savedFolder, WOTC_EXPORT_LAYOUT)
            })
            addView(secondaryButton("Extract enabled mods to base-game layout") {
                writeExtractedEnabledModsToDrive(savedFolder, BASE_GAME_EXPORT_LAYOUT)
            })
            addView(secondaryButton("Backup enabled ZIPs + manifest") {
                writeEnabledBundleToDrive(savedFolder)
            })
        }

        addView(secondaryButton("Open Google Drive") {
            openPackageOrStore("com.google.android.apps.docs", "Google Drive")
        })
        addView(primaryButton("Prepare sync, then open XCOM 2") {
            savedFolder?.let { writeExtractedEnabledModsToDrive(it, WOTC_EXPORT_LAYOUT, showSuccess = false) }
            openXcomOrExplain()
        })
    }

    private fun troubleshootingPanel(): View = card("WHY DIDN'T MY MOD WORK?").apply {
        addView(helpText("The first APK imported your mod into this app, but it did not extract or place it into an XCOM mod folder. That is why you did not see the mod in-game."))
        addView(helpText("Because you confirmed many PC mods do work on Android, this build now extracts enabled ZIPs into PC-style XCOM folders. Try the WOTC layout first for XCOM 2 Collection."))
        addView(helpText("If your proven Android path is different, use the exported folder as a staging folder for now. A later build can add a custom path/root installer once the exact working path is confirmed."))
    }

    private fun cheatsPanel(): View = card("CONSOLE COMMAND CHEAT SHEET").apply {
        addView(helpText("XCOM 2 console commands only work when the game exposes a console or a mod enables one. Mobile support is not guaranteed."))
        CHEATS.forEach { cheat ->
            addView(TextView(context).apply {
                text = "${cheat.command}\n${cheat.description}"
                setTextColor(Color.WHITE)
                textSize = 14f
                setPadding(0, dp(8), 0, dp(8))
            })
        }
    }

    private fun inGameMenuPanel(): View = card("IN-GAME MOD MENU STATUS").apply {
        addView(helpText("A true in-game overlay requires support from XCOM itself, root-level file access, or a platform-specific hooking layer. This starter app does not inject code into the game process."))
        addView(helpText("Planned safe path: generate mod configuration files that XCOM can sync/import, then document any verified mobile file paths or game-supported menu hooks after device testing."))
    }

    private fun modRow(mod: ModRecord): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(10), 0, dp(10))

        addView(TextView(context).apply {
            text = "${if (mod.enabled) "[EXPORT ENABLED]" else "[EXPORT DISABLED]"} ${mod.name}\nImported ${formatDate(mod.installedAt)}\nStored in manager only; not applied to XCOM yet."
            setTextColor(if (mod.enabled) XCOM_CYAN else XCOM_MUTED)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
        })

        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        actions.addView(smallButton(if (mod.enabled) "Disable" else "Enable") {
            store.saveMod(mod.copy(enabled = !mod.enabled))
            writeEnabledManifest()
            render()
        })
        actions.addView(smallButton("Uninstall") {
            confirmUninstall(mod)
        })
        addView(actions)
    }

    private fun importMod(uri: Uri, intentFlags: Int) {
        try {
            try {
                contentResolver.takePersistableUriPermission(uri, intentFlags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // Some file pickers do not offer persistable permissions; copying still works.
            }

            val displayName = queryDisplayName(uri) ?: "xcom2-mod-${System.currentTimeMillis()}.zip"
            val destinationDir = getExternalFilesDir("mods") ?: File(filesDir, "mods")
            destinationDir.mkdirs()
            val destination = File(destinationDir, "${System.currentTimeMillis()}-${sanitizeFileName(displayName)}")

            contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Unable to open selected mod file." }
                destination.outputStream().use { output -> input.copyTo(output) }
            }

            store.saveMod(
                ModRecord(
                    id = UUID.randomUUID().toString(),
                    name = displayName.removeSuffix(".zip"),
                    fileName = destination.name,
                    filePath = destination.absolutePath,
                    sourceUri = uri.toString(),
                    enabled = false,
                    installedAt = System.currentTimeMillis()
                )
            )
            writeEnabledManifest()
            showMessage("Mod imported", "$displayName was copied into this app's storage. It is not installed into XCOM yet. Enable it if you want it included in Drive exports.")
            render()
        } catch (error: Exception) {
            showMessage("Import failed", error.message ?: "Unknown error")
        }
    }

    private fun rememberDriveFolder(uri: Uri, intentFlags: Int) {
        try {
            val flags = intentFlags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) {
            // The URI is still useful during this session even if persistence was denied.
        }
        store.saveDriveTreeUri(uri.toString())
        showMessage("Drive folder saved", "Enabled mod ZIPs and the manifest can now be exported to this folder for testing.")
        render()
    }

    private fun confirmUninstall(mod: ModRecord) {
        AlertDialog.Builder(this)
            .setTitle("Uninstall ${mod.name}?")
            .setMessage("This removes the copy stored inside this mod manager. It does not delete the original download.")
            .setPositiveButton("Uninstall") { _, _ ->
                store.deleteMod(mod.id)
                File(mod.filePath).delete()
                writeEnabledManifest()
                render()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun writeEnabledManifest(): File {
        val manifestDir = getExternalFilesDir("manifests") ?: File(filesDir, "manifests")
        manifestDir.mkdirs()
        val manifestFile = File(manifestDir, "xcom2_mod_manager_enabled_mods.json")
        manifestFile.writeText(buildEnabledManifestJson().toString(2))
        return manifestFile
    }

    private fun writeEnabledManifestToDrive(treeUriString: String, showSuccess: Boolean = true) {
        try {
            val treeUri = Uri.parse(treeUriString)
            val rootDocumentUri = driveRootDocumentUri(treeUri)
            val documentUri = DocumentsContract.createDocument(
                contentResolver,
                rootDocumentUri,
                "application/json",
                "xcom2_mod_manager_enabled_mods.json"
            ) ?: throw IllegalStateException("Could not create a file in the selected Drive folder.")

            contentResolver.openOutputStream(documentUri, "wt").use { output ->
                requireNotNull(output) { "Could not write to the selected Drive folder." }
                output.write(buildEnabledManifestJson().toString(2).toByteArray())
            }
            if (showSuccess) {
                showMessage("Drive export complete", "Enabled mod manifest was written to the selected folder.")
            }
        } catch (error: Exception) {
            showMessage("Drive export failed", error.message ?: "Unknown error")
        }
    }

    private fun writeEnabledBundleToDrive(treeUriString: String, showSuccess: Boolean = true) {
        try {
            val treeUri = Uri.parse(treeUriString)
            val rootDocumentUri = driveRootDocumentUri(treeUri)
            val enabledMods = store.loadMods().filter { it.enabled }

            val manifestUri = DocumentsContract.createDocument(
                contentResolver,
                rootDocumentUri,
                "application/json",
                "xcom2_mod_manager_enabled_mods.json"
            ) ?: throw IllegalStateException("Could not create the manifest in the selected Drive folder.")

            contentResolver.openOutputStream(manifestUri, "wt").use { output ->
                requireNotNull(output) { "Could not write the manifest to the selected Drive folder." }
                output.write(buildEnabledManifestJson().toString(2).toByteArray())
            }

            var copiedMods = 0
            enabledMods.forEach { mod ->
                val modFile = File(mod.filePath)
                if (modFile.exists()) {
                    val modDocumentUri = DocumentsContract.createDocument(
                        contentResolver,
                        rootDocumentUri,
                        "application/zip",
                        sanitizeFileName(mod.name).removeSuffix(".zip") + ".zip"
                    ) ?: throw IllegalStateException("Could not create ${mod.name} in the selected Drive folder.")

                    modFile.inputStream().use { input ->
                        contentResolver.openOutputStream(modDocumentUri, "wt").use { output ->
                            requireNotNull(output) { "Could not write ${mod.name} to the selected Drive folder." }
                            input.copyTo(output)
                        }
                    }
                    copiedMods += 1
                }
            }

            if (showSuccess) {
                showMessage(
                    "Drive export complete",
                    "Wrote the enabled-mod manifest and $copiedMods enabled mod ZIP(s) to the selected Drive folder.\n\nThis still does not guarantee XCOM will load them; it gives us files to test with the game's sync behavior."
                )
            }
        } catch (error: Exception) {
            showMessage("Drive export failed", error.message ?: "Unknown error")
        }
    }

    private fun writeExtractedEnabledModsToDrive(
        treeUriString: String,
        exportLayout: ExportLayout,
        showSuccess: Boolean = true
    ) {
        try {
            val treeUri = Uri.parse(treeUriString)
            val rootDocumentUri = driveRootDocumentUri(treeUri)
            val enabledMods = store.loadMods().filter { it.enabled }
            val modsDirectory = ensureDirectoryPath(rootDocumentUri, exportLayout.pathSegments)

            writeJsonDocument(rootDocumentUri, "xcom2_mod_manager_enabled_mods.json", buildEnabledManifestJson())

            var extractedMods = 0
            enabledMods.forEach { mod ->
                val modFile = File(mod.filePath)
                if (modFile.exists()) {
                    val modDirectory = ensureDirectory(modsDirectory, sanitizePathSegment(mod.name))
                    extractZipIntoDocumentDirectory(modFile, modDirectory)
                    extractedMods += 1
                }
            }

            if (showSuccess) {
                showMessage(
                    "PC-style export complete",
                    "Extracted $extractedMods enabled mod(s) into:\n${exportLayout.displayPath}\n\nUse this folder with the Android install/sync method you confirmed works."
                )
            }
        } catch (error: Exception) {
            showMessage("PC-style export failed", error.message ?: "Unknown error")
        }
    }

    private fun extractZipIntoDocumentDirectory(zipFile: File, targetDirectory: Uri) {
        val topLevelFolder = detectSingleTopLevelFolder(zipFile)
        ZipInputStream(zipFile.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val pathSegments = normalizedZipSegments(entry.name, topLevelFolder)
                if (pathSegments.isNotEmpty()) {
                    if (entry.isDirectory) {
                        ensureDirectoryPath(targetDirectory, pathSegments)
                    } else {
                        val parentDirectory = ensureDirectoryPath(targetDirectory, pathSegments.dropLast(1))
                        writeBinaryDocument(parentDirectory, pathSegments.last(), guessMimeType(pathSegments.last())) { output ->
                            zip.copyTo(output)
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun detectSingleTopLevelFolder(zipFile: File): String? {
        val topLevelNames = linkedSetOf<String>()
        var hasRootFile = false
        ZipInputStream(zipFile.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val segments = normalizedZipSegments(entry.name, null)
                if (segments.isNotEmpty()) {
                    if (segments.size == 1 && !entry.isDirectory) {
                        hasRootFile = true
                    }
                    topLevelNames.add(segments.first())
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return if (!hasRootFile && topLevelNames.size == 1) topLevelNames.first() else null
    }

    private fun normalizedZipSegments(entryName: String, topLevelFolder: String?): List<String> {
        val segments = entryName
            .replace('\\', '/')
            .trim('/')
            .split('/')
            .filter { it.isNotBlank() && it != "." && it != "__MACOSX" }

        if (segments.any { it == ".." }) return emptyList()
        val stripped = if (topLevelFolder != null && segments.firstOrNull() == topLevelFolder) {
            segments.drop(1)
        } else {
            segments
        }
        return stripped.map { sanitizePathSegment(it) }.filter { it.isNotBlank() }
    }

    private fun ensureDirectoryPath(startDirectory: Uri, pathSegments: List<String>): Uri {
        var currentDirectory = startDirectory
        pathSegments.forEach { segment ->
            currentDirectory = ensureDirectory(currentDirectory, segment)
        }
        return currentDirectory
    }

    private fun ensureDirectory(parentDirectory: Uri, displayName: String): Uri {
        val safeName = sanitizePathSegment(displayName)
        findChildDocument(parentDirectory, safeName, DocumentsContract.Document.MIME_TYPE_DIR)?.let { return it }
        return DocumentsContract.createDocument(
            contentResolver,
            parentDirectory,
            DocumentsContract.Document.MIME_TYPE_DIR,
            safeName
        ) ?: throw IllegalStateException("Could not create folder $safeName.")
    }

    private fun writeJsonDocument(parentDirectory: Uri, displayName: String, json: JSONObject) {
        writeBinaryDocument(parentDirectory, displayName, "application/json") { output ->
            output.write(json.toString(2).toByteArray())
        }
    }

    private fun writeBinaryDocument(
        parentDirectory: Uri,
        displayName: String,
        mimeType: String,
        writer: (java.io.OutputStream) -> Unit
    ) {
        val safeName = sanitizePathSegment(displayName)
        val documentUri = findChildDocument(parentDirectory, safeName, null)
            ?: DocumentsContract.createDocument(contentResolver, parentDirectory, mimeType, safeName)
            ?: throw IllegalStateException("Could not create file $safeName.")

        contentResolver.openOutputStream(documentUri, "wt").use { output ->
            requireNotNull(output) { "Could not write $safeName." }
            writer(output)
        }
    }

    private fun findChildDocument(parentDirectory: Uri, displayName: String, mimeType: String?): Uri? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            parentDirectory,
            DocumentsContract.getDocumentId(parentDirectory)
        )
        val columns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        contentResolver.query(childrenUri, columns, null, null, null).use { cursor ->
            if (cursor == null) return null
            val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (cursor.moveToNext()) {
                val childName = cursor.getString(nameIndex)
                val childMime = cursor.getString(mimeIndex)
                if (childName == displayName && (mimeType == null || childMime == mimeType)) {
                    val childId = cursor.getString(idIndex)
                    return DocumentsContract.buildDocumentUriUsingTree(parentDirectory, childId)
                }
            }
        }
        return null
    }

    private fun driveRootDocumentUri(treeUri: Uri): Uri =
        DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )

    private fun buildEnabledManifestJson(): JSONObject {
        val enabledMods = JSONArray()
        store.loadMods().filter { it.enabled }.forEach { mod ->
            enabledMods.put(
                JSONObject()
                    .put("id", mod.id)
                    .put("name", mod.name)
                    .put("fileName", mod.fileName)
                    .put("installedAt", mod.installedAt)
            )
        }
        return JSONObject()
            .put("generatedBy", "XCOM 2 Mobile Mod Manager")
            .put("generatedAt", System.currentTimeMillis())
            .put("importantNote", "This file is a manager manifest. Verified XCOM 2 Android mod file paths still need device testing.")
            .put("enabledMods", enabledMods)
    }

    private fun openXcomOrExplain() {
        val packageName = XCOM_PACKAGES.firstOrNull { packageManager.getLaunchIntentForPackage(it) != null }
        if (packageName == null) {
            showMessage(
                "XCOM 2 not found",
                "I could not find a known XCOM 2 Collection package on this device. Open the game manually after exporting files."
            )
        } else {
            startActivity(packageManager.getLaunchIntentForPackage(packageName))
        }
    }

    private fun openPackageOrStore(packageName: String, friendlyName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            showMessage("$friendlyName not found", "Install or open $friendlyName manually, then return to this app.")
        }
    }

    private fun openUri(uri: Uri) {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun queryDisplayName(uri: Uri): String? {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return cursor.getString(index)
            }
        }
        return uri.lastPathSegment
    }

    private fun card(title: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(14), dp(14), dp(14))
        background = GradientDrawable().apply {
            setColor(XCOM_PANEL)
            setStroke(dp(1), XCOM_CYAN)
            cornerRadius = dp(10).toFloat()
        }
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, dp(14))
        }
        layoutParams = params

        addView(TextView(context).apply {
            text = title
            setTextColor(XCOM_ORANGE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.04f
            setPadding(0, 0, 0, dp(8))
        })
    }

    private fun primaryButton(text: String, onClick: () -> Unit): Button =
        styledButton(text, XCOM_CYAN, Color.BLACK, onClick)

    private fun secondaryButton(text: String, onClick: () -> Unit): Button =
        styledButton(text, XCOM_ORANGE, Color.BLACK, onClick)

    private fun smallButton(text: String, onClick: () -> Unit): Button =
        styledButton(text, XCOM_PANEL_ALT, Color.WHITE, onClick).apply {
            val params = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            params.setMargins(0, dp(8), dp(8), 0)
            layoutParams = params
        }

    private fun styledButton(text: String, backgroundColor: Int, textColor: Int, onClick: () -> Unit): Button =
        Button(this).apply {
            this.text = text
            setTextColor(textColor)
            setTypeface(Typeface.DEFAULT_BOLD)
            background = GradientDrawable().apply {
                setColor(backgroundColor)
                cornerRadius = dp(8).toFloat()
            }
            setOnClickListener { onClick() }
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, dp(6), 0, dp(6))
            layoutParams = params
        }

    private fun helpText(text: String): TextView = TextView(this).apply {
        this.text = text
        setTextColor(XCOM_MUTED)
        textSize = 14f
        setPadding(0, dp(6), 0, dp(6))
    }

    private fun showMessage(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun sanitizeFileName(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "mod.zip" }

    private fun sanitizePathSegment(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").trim().ifBlank { "mod" }

    private fun guessMimeType(fileName: String): String {
        val lowerName = fileName.lowercase(Locale.US)
        return when {
            lowerName.endsWith(".ini") -> "text/plain"
            lowerName.endsWith(".int") -> "text/plain"
            lowerName.endsWith(".xcommod") -> "text/plain"
            lowerName.endsWith(".json") -> "application/json"
            lowerName.endsWith(".txt") -> "text/plain"
            lowerName.endsWith(".png") -> "image/png"
            lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") -> "image/jpeg"
            lowerName.endsWith(".zip") -> "application/zip"
            else -> "application/octet-stream"
        }
    }

    private fun formatDate(timestamp: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault()).format(Date(timestamp))

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_IMPORT_MOD = 1001
        private const val REQUEST_PICK_DRIVE_FOLDER = 1002
        private const val NEXUS_MODS_URL = "https://www.nexusmods.com/games/xcom2/mods"

        private val WOTC_EXPORT_LAYOUT = ExportLayout(
            label = "War of the Chosen",
            pathSegments = listOf("XCom2-WarOfTheChosen", "XComGame", "Mods")
        )
        private val BASE_GAME_EXPORT_LAYOUT = ExportLayout(
            label = "Base game",
            pathSegments = listOf("XComGame", "Mods")
        )

        private val XCOM_DARK = Color.rgb(7, 17, 31)
        private val XCOM_PANEL = Color.rgb(16, 36, 58)
        private val XCOM_PANEL_ALT = Color.rgb(28, 57, 86)
        private val XCOM_CYAN = Color.rgb(55, 217, 255)
        private val XCOM_ORANGE = Color.rgb(255, 159, 28)
        private val XCOM_MUTED = Color.rgb(183, 206, 221)

        private val XCOM_PACKAGES = listOf(
            "com.feralinteractive.xcom2_android",
            "com.feralinteractive.xcom2collection"
        )
    }
}

data class ExportLayout(
    val label: String,
    val pathSegments: List<String>
) {
    val displayPath: String = pathSegments.joinToString("/")
}

data class ModRecord(
    val id: String,
    val name: String,
    val fileName: String,
    val filePath: String,
    val sourceUri: String?,
    val enabled: Boolean,
    val installedAt: Long
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("fileName", fileName)
        .put("filePath", filePath)
        .put("sourceUri", sourceUri)
        .put("enabled", enabled)
        .put("installedAt", installedAt)

    companion object {
        fun fromJson(json: JSONObject): ModRecord = ModRecord(
            id = json.getString("id"),
            name = json.getString("name"),
            fileName = json.getString("fileName"),
            filePath = json.getString("filePath"),
            sourceUri = json.optString("sourceUri").ifBlank { null },
            enabled = json.optBoolean("enabled", false),
            installedAt = json.optLong("installedAt", System.currentTimeMillis())
        )
    }
}

data class CheatCode(val command: String, val description: String)

class ModStore(context: Context) {
    private val prefs = context.getSharedPreferences("xcom2_mod_manager", Context.MODE_PRIVATE)

    fun loadMods(): List<ModRecord> {
        val raw = prefs.getString(KEY_MODS, "[]") ?: "[]"
        val array = JSONArray(raw)
        return (0 until array.length()).map { index -> ModRecord.fromJson(array.getJSONObject(index)) }
            .sortedBy { it.name.lowercase(Locale.US) }
    }

    fun saveMod(mod: ModRecord) {
        val mods = loadMods().filterNot { it.id == mod.id } + mod
        saveMods(mods)
    }

    fun deleteMod(id: String) {
        saveMods(loadMods().filterNot { it.id == id })
    }

    fun saveDriveTreeUri(uri: String) {
        prefs.edit().putString(KEY_DRIVE_TREE_URI, uri).apply()
    }

    fun loadDriveTreeUri(): String? = prefs.getString(KEY_DRIVE_TREE_URI, null)

    private fun saveMods(mods: List<ModRecord>) {
        val array = JSONArray()
        mods.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_MODS, array.toString()).apply()
    }

    companion object {
        private const val KEY_MODS = "mods"
        private const val KEY_DRIVE_TREE_URI = "driveTreeUri"
    }
}

private val CHEATS = listOf(
    CheatCode("PowerUp", "God mode plus no reload for the selected tactical mission."),
    CheatCode("TakeNoDamage", "Selected soldiers do not take damage."),
    CheatCode("GiveActionPoints 2", "Adds action points to the selected unit."),
    CheatCode("ToggleUnlimitedActions", "Toggles unlimited actions for the current tactical mission."),
    CheatCode("GiveResource Supplies 1000", "Adds supplies to the Avenger inventory."),
    CheatCode("GiveResource Intel 500", "Adds Intel."),
    CheatCode("GiveResource EleriumDust 500", "Adds Elerium Crystals."),
    CheatCode("GiveResource AlienAlloy 500", "Adds Alien Alloys."),
    CheatCode("GiveScientist 1", "Adds a scientist."),
    CheatCode("GiveEngineer 1", "Adds an engineer."),
    CheatCode("LevelUpBarracks 1", "Levels up every soldier in the barracks once."),
    CheatCode("HealAllSoldiers", "Heals wounded soldiers in the barracks."),
    CheatCode("ToggleSquadConcealment", "Toggles squad concealment during a mission."),
    CheatCode("RestartLevel", "Restarts the current tactical mission.")
)
