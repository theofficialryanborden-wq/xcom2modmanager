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
            text = "Safe starter app for installing, tracking, enabling, exporting, and learning mods for XCOM 2 Collection on Android."
            setTextColor(XCOM_MUTED)
            textSize = 14f
            setPadding(0, dp(8), 0, 0)
        })
    }

    private fun modActionsPanel(): View = card("MOD OPERATIONS").apply {
        addView(primaryButton("Install mod ZIP from phone") {
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

        addView(helpText("Nexus Mods downloads usually require you to sign in and download through their site. Download a mod ZIP first, then return here and tap Install."))
    }

    private fun installedModsPanel(): View = card("INSTALLED MODS").apply {
        val mods = store.loadMods()
        if (mods.isEmpty()) {
            addView(helpText("No mods installed yet. Tap Install mod ZIP from phone to import one."))
        } else {
            mods.forEach { mod ->
                addView(modRow(mod))
            }
            addView(secondaryButton("Export enabled-mod manifest") {
                val file = writeEnabledManifest()
                showMessage("Manifest exported", "Saved enabled mod list to:\n${file.absolutePath}")
            })
        }
    }

    private fun drivePanel(): View = card("GOOGLE DRIVE SYNC WORKAROUND").apply {
        val savedFolder = store.loadDriveTreeUri()
        addView(helpText("Android apps cannot force XCOM 2 Collection to show its Google Drive sync prompt. This app can prepare files in a Drive folder and then open XCOM so you can use the game sync prompt if it appears."))

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
            addView(primaryButton("Write manifest to Drive folder") {
                writeEnabledManifestToDrive(savedFolder)
            })
        }

        addView(secondaryButton("Open Google Drive") {
            openPackageOrStore("com.google.android.apps.docs", "Google Drive")
        })
        addView(primaryButton("Prepare sync, then open XCOM 2") {
            savedFolder?.let { writeEnabledManifestToDrive(it, showSuccess = false) }
            openXcomOrExplain()
        })
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
            text = "${if (mod.enabled) "[ENABLED]" else "[DISABLED]"} ${mod.name}\nInstalled ${formatDate(mod.installedAt)}"
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
            showMessage("Mod installed", "$displayName was copied into this app's mod storage. It is disabled until you enable it.")
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
        showMessage("Drive folder saved", "Enabled mod manifests can now be exported to this folder.")
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
            val rootDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri)
            )
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

    private fun formatDate(timestamp: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault()).format(Date(timestamp))

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_IMPORT_MOD = 1001
        private const val REQUEST_PICK_DRIVE_FOLDER = 1002
        private const val NEXUS_MODS_URL = "https://www.nexusmods.com/games/xcom2/mods"

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
