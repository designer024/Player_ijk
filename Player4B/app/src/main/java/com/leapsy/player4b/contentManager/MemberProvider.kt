package com.leapsy.player4b.contentManager

import android.content.*
import android.database.Cursor
import android.net.Uri
import android.text.TextUtils
import android.widget.Toast
import java.lang.IllegalArgumentException
import java.util.*

class MemberProvider : ContentProvider() {

    private var mDatabaseManager: DatabaseManager? = null

    private var mCursor: Cursor? = null

    companion object Matcher {
        val mUriMatcher = UriMatcher(UriMatcher.NO_MATCH)

        init {
            // 若URI資源路徑 = content://com.leapsy.vicihomeprovider/ViciInit ，則返回註冊碼 1
            mUriMatcher.addURI(ContentMemberBook.AUTHORITY, ContentMemberBook.PATH, 1)

            // 若URI資源路徑 = content://com.leapsy.vicihomeprovider/ViciInit/數字 ，則返回註冊碼 2
            mUriMatcher.addURI(ContentMemberBook.AUTHORITY, ContentMemberBook.PATH.toString() + "/#", 2)
        }
    }

    override fun onCreate(): Boolean {
        mDatabaseManager = DatabaseManager(context)
        return false
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val db = mDatabaseManager!!.writableDatabase

        when (mUriMatcher.match(uri)) {
            1 -> mCursor = db.query(
                ContentMemberBook.Member.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
            )
            2 -> {
                mCursor = db.query(
                    ContentMemberBook.Member.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    null
                )
            }
            else -> {
                Toast.makeText(context, "Invalid content uri", Toast.LENGTH_SHORT).show()
                throw IllegalArgumentException("Unknown Uri: $uri")
            }
        }
        mCursor?.setNotificationUri(Objects.requireNonNull(context)?.contentResolver, uri)
        return mCursor
    }

    override fun getType(uri: Uri): String? {
        return when (mUriMatcher.match(uri)) {
            1 -> ContentMemberBook.CONTENT_LIST
            2 -> ContentMemberBook.CONTENT_ITEM
            else -> throw IllegalArgumentException("Unknown Uri: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val db = mDatabaseManager!!.writableDatabase

        require(mUriMatcher.match(uri) == 1) { "Unknown URI: $uri" }
        val rowId = db.insert(ContentMemberBook.Member.TABLE_NAME, null, values)
        if (rowId > 0) {
            val uriMember = ContentUris.withAppendedId(ContentMemberBook.CONTENT_URI, rowId)
            Objects.requireNonNull(context)!!
                .contentResolver.notifyChange(uriMember, null)
            return uriMember
        }
        throw IllegalArgumentException("Unknown URI: $uri")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val db = mDatabaseManager!!.writableDatabase
        var count = 0

        count = when (mUriMatcher.match(uri)) {
            1 -> db.delete(ContentMemberBook.Member.TABLE_NAME, selection, selectionArgs)
            2 -> {
                val rowId = uri.pathSegments[1]
                db.delete(
                    ContentMemberBook.Member.TABLE_NAME,
                    ContentMemberBook.Member.ID + " = " + rowId
                            + if (!TextUtils.isEmpty(selection)) " AND ($selection)" else "",
                    selectionArgs
                )
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        Objects.requireNonNull(context)!!.contentResolver.notifyChange(uri, null)
        return count
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        val db = mDatabaseManager!!.writableDatabase
        var count = 0

        count = when (mUriMatcher.match(uri)) {
            1 -> db.update(
                ContentMemberBook.Member.TABLE_NAME,
                values,
                selection,
                selectionArgs
            )
            2 -> {
                val rowId = uri.pathSegments[1]
                db.update(
                    ContentMemberBook.Member.TABLE_NAME,
                    values,
                    ContentMemberBook.Member.ID + " = " + rowId +
                            if (!TextUtils.isEmpty(selection)) " AND (" + ")" else "",
                    selectionArgs
                )
            }
            else -> throw IllegalArgumentException("Unknown Uri: $uri")
        }

        Objects.requireNonNull(context)?.contentResolver?.notifyChange(uri, null)
        return count
    }
}