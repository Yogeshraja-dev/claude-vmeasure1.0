package com.vmeasure.app.data.repository

import android.content.Context
import android.provider.Settings
import com.google.gson.Gson
import com.vmeasure.app.data.db.dao.AppConfigDao
import com.vmeasure.app.data.db.entity.AppConfigEntity
import com.vmeasure.app.data.db.entity.DeletedUserIdEntity
import com.vmeasure.app.data.model.MeasurementSection
import com.vmeasure.app.data.model.SyncGeneral
import com.vmeasure.app.data.model.SyncJson
import com.vmeasure.app.data.model.SyncSection
import com.vmeasure.app.data.model.SyncUser
import com.vmeasure.app.data.model.TagType
import com.vmeasure.app.data.model.User
import com.vmeasure.app.util.DateFormatter
import com.vmeasure.app.util.IdGenerator
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appConfigDao: AppConfigDao,
    private val userRepository: UserRepository,
    private val okHttpClient: OkHttpClient
) {
    private val gson = Gson()
    private val FOLDER_NAME_PREFIX = "Vmeasure_Backup_"
    private val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
    private val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"

    // ── Device ID ─────────────────────────────────────────────────────────────

    fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"
    }

    // ── Config helpers ────────────────────────────────────────────────────────

    suspend fun getStoredToken(): String? =
        appConfigDao.get(com.vmeasure.app.data.db.entity.ConfigKeys.GOOGLE_ACCOUNT_TOKEN)

    suspend fun getStoredEmail(): String? =
        appConfigDao.get(com.vmeasure.app.data.db.entity.ConfigKeys.GOOGLE_ACCOUNT_EMAIL)

    suspend fun storeAccountInfo(email: String, token: String) {
        appConfigDao.set(AppConfigEntity(
            com.vmeasure.app.data.db.entity.ConfigKeys.GOOGLE_ACCOUNT_EMAIL, email))
        appConfigDao.set(AppConfigEntity(
            com.vmeasure.app.data.db.entity.ConfigKeys.GOOGLE_ACCOUNT_TOKEN, token))
        appConfigDao.set(AppConfigEntity(
            com.vmeasure.app.data.db.entity.ConfigKeys.DEVICE_ID, getDeviceId()))
    }

    suspend fun getLastSyncTime(): String? =
        appConfigDao.get(com.vmeasure.app.data.db.entity.ConfigKeys.LAST_SYNC_TIME)

    suspend fun setLastSyncTime() {
        appConfigDao.set(AppConfigEntity(
            com.vmeasure.app.data.db.entity.ConfigKeys.LAST_SYNC_TIME,
            DateFormatter.nowTimestamp()
        ))
    }

    suspend fun getStoredFolderId(): String? =
        appConfigDao.get(com.vmeasure.app.data.db.entity.ConfigKeys.DRIVE_FOLDER_ID)

    suspend fun storeFolderId(id: String) {
        appConfigDao.set(AppConfigEntity(
            com.vmeasure.app.data.db.entity.ConfigKeys.DRIVE_FOLDER_ID, id))
    }

    // ── Is signed in ─────────────────────────────────────────────────────────

    suspend fun isSignedIn(): Boolean = !getStoredToken().isNullOrBlank()

    // ── Drive: Find or create folder ─────────────────────────────────────────

    suspend fun findOrCreateFolder(token: String): Result<String> {
        return try {
            // First check stored folder id
            val storedId = getStoredFolderId()
            if (!storedId.isNullOrBlank()) {
                // Verify it still exists
                val verifyUrl = "$DRIVE_FILES_URL/$storedId?fields=id"
                val verifyReq = Request.Builder()
                    .url(verifyUrl)
                    .addHeader("Authorization", "Bearer $token")
                    .get().build()
                val resp = okHttpClient.newCall(verifyReq).execute()
                if (resp.isSuccessful) return Result.success(storedId)
            }

            // Search for existing folder with prefix
            val searchUrl = "$DRIVE_FILES_URL?q=" +
                    "mimeType='application/vnd.google-apps.folder' and " +
                    "name contains '$FOLDER_NAME_PREFIX' and trashed=false" +
                    "&fields=files(id,name)&orderBy=createdTime desc"

            val searchReq = Request.Builder()
                .url(searchUrl)
                .addHeader("Authorization", "Bearer $token")
                .get().build()

            val searchResp = okHttpClient.newCall(searchReq).execute()
            val searchBody = searchResp.body?.string() ?: ""
            val files = JSONObject(searchBody).optJSONArray("files")

            if (files != null && files.length() > 0) {
                val folderId = files.getJSONObject(0).getString("id")
                storeFolderId(folderId)
                return Result.success(folderId)
            }

            // Create new folder
            val folderName = "$FOLDER_NAME_PREFIX${IdGenerator.generate()}"
            val meta = JSONObject().apply {
                put("name", folderName)
                put("mimeType", "application/vnd.google-apps.folder")
            }
            val body = meta.toString()
                .toRequestBody("application/json".toMediaType())
            val createReq = Request.Builder()
                .url(DRIVE_FILES_URL)
                .addHeader("Authorization", "Bearer $token")
                .post(body).build()

            val createResp = okHttpClient.newCall(createReq).execute()
            val createBody = createResp.body?.string() ?: ""
            val folderId = JSONObject(createBody).getString("id")
            storeFolderId(folderId)
            Result.success(folderId)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Drive: Get latest JSON file ───────────────────────────────────────────

    suspend fun getLatestDriveFile(token: String, folderId: String): Result<SyncJson?> {
        return try {
            val url = "$DRIVE_FILES_URL?q='$folderId' in parents and trashed=false" +
                    "&fields=files(id,name)&orderBy=name desc"

            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get().build()

            val resp = okHttpClient.newCall(req).execute()
            val body = resp.body?.string() ?: ""
            val files = JSONObject(body).optJSONArray("files")

            if (files == null || files.length() == 0) {
                return Result.success(null)
            }

            // Latest file is first (ordered by name desc — name is the 18-digit timestamp)
            val fileId = files.getJSONObject(0).getString("id")
            val downloadUrl = "$DRIVE_FILES_URL/$fileId?alt=media"

            val dlReq = Request.Builder()
                .url(downloadUrl)
                .addHeader("Authorization", "Bearer $token")
                .get().build()

            val dlResp = okHttpClient.newCall(dlReq).execute()
            val json = dlResp.body?.string() ?: return Result.success(null)
            val syncJson = gson.fromJson(json, SyncJson::class.java)
            Result.success(syncJson)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Drive: Upload JSON file ───────────────────────────────────────────────

    suspend fun uploadJsonFile(
        token: String,
        folderId: String,
        json: String
    ): Result<Unit> {
        return try {
            val fileName = IdGenerator.generate()
            val metaJson = JSONObject().apply {
                put("name", fileName)
                put("parents", JSONArray().put(folderId))
            }

            val boundary = "vmeasure_boundary_${System.currentTimeMillis()}"
            val body = buildString {
                append("--$boundary\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(metaJson.toString())
                append("\r\n--$boundary\r\n")
                append("Content-Type: application/json\r\n\r\n")
                append(json)
                append("\r\n--$boundary--")
            }.toRequestBody("multipart/related; boundary=$boundary".toMediaType())

            val req = Request.Builder()
                .url(DRIVE_UPLOAD_URL)
                .addHeader("Authorization", "Bearer $token")
                .post(body).build()

            val resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Upload failed: ${resp.code}"))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Build local SyncJson ──────────────────────────────────────────────────

    suspend fun buildLocalSyncJson(): SyncJson {
        val allIds = userRepository.getAllUserIds()
        val syncUsers = allIds.mapNotNull { id ->
            val user = userRepository.getUserWithSections(id) ?: return@mapNotNull null
            userToSyncUser(user)
        }
        val deletedIds = userRepository.getAllDeletedIds()
        return SyncJson(
            users = syncUsers,
            deletedUserIds = deletedIds,
            general = SyncGeneral(
                deviceId = getDeviceId(),
                appVersion = getAppVersion(),
                databaseVersion = "1"
            )
        )
    }

    private fun getAppVersion(): String {
        return try {
            context.packageManager
                .getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) { "1.0" }
    }

    // ── Perform full sync ─────────────────────────────────────────────────────

    suspend fun performSync(
        token: String,
        onConflict: suspend () -> Boolean  // returns true = proceed, false = cancel
    ): Result<Unit> {
        return try {
            // 1. Find or create folder
            val folderResult = findOrCreateFolder(token)
            if (folderResult.isFailure)
                return Result.failure(folderResult.exceptionOrNull()!!)
            val folderId = folderResult.getOrThrow()

            // 2. Get latest Drive file
            val driveResult = getLatestDriveFile(token, folderId)
            if (driveResult.isFailure)
                return Result.failure(driveResult.exceptionOrNull()!!)
            val driveJson = driveResult.getOrNull()

            if (driveJson == null) {
                // No file on Drive — just upload current local data
                val local = buildLocalSyncJson()
                val json = gson.toJson(local)
                return uploadJsonFile(token, folderId, json)
            }

            // 3. Check if same device
            val localDeviceId = getDeviceId()
            val driveDeviceId = driveJson.general.deviceId

            if (localDeviceId == driveDeviceId) {
                // Same device — just upload
                val local = buildLocalSyncJson()
                return uploadJsonFile(token, folderId, gson.toJson(local))
            }

            // 4. Different device — warn user
            val proceed = onConflict()
            if (!proceed) return Result.success(Unit)

            // 5. Merge deletedUserIds
            val localDeleted = userRepository.getAllDeletedIds().toMutableSet()
            localDeleted.addAll(driveJson.deletedUserIds)
            val mergedDeleted = localDeleted.toList()

            // Insert any new deleted IDs locally
            val newDeletedEntities = driveJson.deletedUserIds.map { DeletedUserIdEntity(it) }
            // (direct dao access via userRepository is not available — use upsert path)

            // 6. Process each Drive user
            for (syncUser in driveJson.users) {
                // Skip if in merged deleted list
                if (syncUser.id in mergedDeleted) {
                    userRepository.deleteUser(syncUser.id)
                    continue
                }
                // Convert SyncUser → User + sections
                val (user, sections) = syncUserToUserAndSections(syncUser)
                userRepository.upsertUserFromSync(user, sections)
            }

            // 7. Upload merged result
            val finalLocal = buildLocalSyncJson()
            val uploadResult = uploadJsonFile(token, folderId, gson.toJson(finalLocal))

            if (uploadResult.isSuccess) {
                setLastSyncTime()
            }

            uploadResult

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Conversion helpers ────────────────────────────────────────────────────

    private fun userToSyncUser(user: User): SyncUser {
        return SyncUser(
            id = user.userId,
            userName = user.name,
            dateOfBirth = user.dateOfBirth,
            specialDate = user.specialDate,
            isFavorite = user.isFavorite,
            isPinned = user.isPinned,
            contactNumber = user.contactNumber,
            instagramId = user.instagramId,
            otherMediaId = user.otherMediaId,
            location = user.location,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
            tags = user.selectedTags,
            measurementSections = user.sections.map { sectionToSyncSection(it) }
        )
    }

    private fun sectionToSyncSection(s: MeasurementSection): SyncSection {
        val data = buildMap<String, String> {
            if (s.uBust.isNotBlank()) put("uBust", s.uBust)
            if (s.bust.isNotBlank()) put("bust", s.bust)
            if (s.waist.isNotBlank()) put("waist", s.waist)
            if (s.hip.isNotBlank()) put("hip", s.hip)
            if (s.armhole.isNotBlank()) put("armhole", s.armhole)
            if (s.shoulder.isNotBlank()) put("shoulder", s.shoulder)
            if (s.length.isNotBlank()) put("length", s.length)
            if (s.fNeck.isNotBlank()) put("fNeck", s.fNeck)
            if (s.bNeck.isNotBlank()) put("bNeck", s.bNeck)
            if (s.sleeveLength.isNotBlank()) put("sleeveLength", s.sleeveLength)
            if (s.sleeveRound.isNotBlank()) put("sleeveRound", s.sleeveRound)
            if (s.blouseCut.isNotBlank()) put("blouseCut", s.blouseCut)
            if (s.blouseField.isNotBlank()) put("blouseField", s.blouseField)
            if (s.thighRound.isNotBlank()) put("thighRound", s.thighRound)
            if (s.kneeRound.isNotBlank()) put("kneeRound", s.kneeRound)
            if (s.bottom.isNotBlank()) put("bottom", s.bottom)
            if (s.inseam.isNotBlank()) put("inseam", s.inseam)
            if (s.frockLength.isNotBlank()) put("frockLength", s.frockLength)
            if (s.yokeLength.isNotBlank()) put("yokeLength", s.yokeLength)
            if (s.blouseWaist.isNotBlank()) put("blouseWaist", s.blouseWaist)
            if (s.blouseLength.isNotBlank()) put("blouseLength", s.blouseLength)
            if (s.skirtLength.isNotBlank()) put("skirtLength", s.skirtLength)
            if (s.waistLength.isNotBlank()) put("waistLength", s.waistLength)
            if (s.chest.isNotBlank()) put("chest", s.chest)
            if (s.pantLength.isNotBlank()) put("pantLength", s.pantLength)
            if (s.pantWaist.isNotBlank()) put("pantWaist", s.pantWaist)
            if (s.notes.isNotBlank()) put("notes", s.notes)
        }
        return SyncSection(id = s.sectionId, type = s.type,
            createdAt = s.createdAt, updatedAt = s.updatedAt, data = data)
    }

    private fun syncUserToUserAndSections(
        s: SyncUser
    ): Pair<User, List<MeasurementSection>> {
        val user = User(
            userId = s.id, name = s.userName,
            dateOfBirth = s.dateOfBirth, specialDate = s.specialDate,
            isFavorite = s.isFavorite, isPinned = s.isPinned,
            contactNumber = s.contactNumber, instagramId = s.instagramId,
            otherMediaId = s.otherMediaId, location = s.location,
            createdAt = s.createdAt, updatedAt = s.updatedAt,
            selectedTags = s.tags,
            createdAtMillis = 0L, updatedAtMillis = 0L,
            specialDateMillis = 0L, dobMillis = 0L
        )
        val sections = s.measurementSections.mapIndexed { index, ss ->
            MeasurementSection(
                sectionId = ss.id, userId = s.id, type = ss.type,
                createdAt = ss.createdAt, updatedAt = ss.updatedAt,
                createdAtMillis = 0L, updatedAtMillis = 0L, sortOrder = index,
                uBust = ss.data["uBust"] ?: "",
                bust = ss.data["bust"] ?: "",
                waist = ss.data["waist"] ?: "",
                hip = ss.data["hip"] ?: "",
                armhole = ss.data["armhole"] ?: "",
                shoulder = ss.data["shoulder"] ?: "",
                length = ss.data["length"] ?: "",
                fNeck = ss.data["fNeck"] ?: "",
                bNeck = ss.data["bNeck"] ?: "",
                sleeveLength = ss.data["sleeveLength"] ?: "",
                sleeveRound = ss.data["sleeveRound"] ?: "",
                blouseCut = ss.data["blouseCut"] ?: "",
                blouseField = ss.data["blouseField"] ?: "",
                thighRound = ss.data["thighRound"] ?: "",
                kneeRound = ss.data["kneeRound"] ?: "",
                bottom = ss.data["bottom"] ?: "",
                inseam = ss.data["inseam"] ?: "",
                frockLength = ss.data["frockLength"] ?: "",
                yokeLength = ss.data["yokeLength"] ?: "",
                blouseWaist = ss.data["blouseWaist"] ?: "",
                blouseLength = ss.data["blouseLength"] ?: "",
                skirtLength = ss.data["skirtLength"] ?: "",
                waistLength = ss.data["waistLength"] ?: "",
                chest = ss.data["chest"] ?: "",
                pantLength = ss.data["pantLength"] ?: "",
                pantWaist = ss.data["pantWaist"] ?: "",
                notes = ss.data["notes"] ?: ""
            )
        }
        return Pair(user, sections)
    }
}