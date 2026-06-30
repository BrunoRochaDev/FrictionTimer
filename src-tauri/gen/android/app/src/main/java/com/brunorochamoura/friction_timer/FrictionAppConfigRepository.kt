package com.brunorochamoura.friction_timer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.util.Log
import org.json.JSONArray
import java.io.File

class FrictionAppConfigRepository(private val context: Context) {
  fun findByAppId(appId: String): FrictionAppConfig? {
    val dbFile = resolveDatabaseFile()
    if (!dbFile.exists()) {
      return null
    }

    var db: SQLiteDatabase? = null
    return try {
      db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
      db.rawQuery(
        """
        SELECT app_id, name, wait_seconds, duration_seconds, messages_json
        FROM friction_apps
        WHERE app_id = ?
        LIMIT 1
        """.trimIndent(),
        arrayOf(appId),
      ).use { cursor ->
        if (!cursor.moveToFirst()) {
          return null
        }

        val waitSeconds = cursor.getLong(2)
        val durationSeconds = cursor.getLong(3)
        if (waitSeconds < 1L || durationSeconds < 1L) {
          return null
        }

        FrictionAppConfig(
          appId = cursor.getString(0),
          name = cursor.getString(1),
          waitSeconds = waitSeconds,
          durationSeconds = durationSeconds,
          messages = parseMessages(cursor.getString(4)),
        )
      }
    } catch (ex: Exception) {
      Log.w(TAG, "Failed to read tracked app config for $appId", ex)
      null
    } finally {
      db?.close()
    }
  }

  private fun resolveDatabaseFile(): File {
    val dataDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      context.dataDir
    } else {
      File(context.applicationInfo.dataDir)
    }

    return File(dataDir, DB_NAME)
  }

  private fun parseMessages(messagesJson: String?): List<String> {
    if (messagesJson.isNullOrBlank()) {
      return emptyList()
    }

    return try {
      val array = JSONArray(messagesJson)
      val messages = buildList {
        for (index in 0 until array.length()) {
          add(array.optString(index))
        }
      }

      FrictionOverlayLogic.sanitizeMessages(messages)
    } catch (ex: Exception) {
      Log.w(TAG, "Failed to parse friction messages JSON", ex)
      emptyList()
    }
  }

  companion object {
    private const val TAG = "FrictionAppConfigRepo"
    private const val DB_NAME = "friction-timer.db"
  }
}
