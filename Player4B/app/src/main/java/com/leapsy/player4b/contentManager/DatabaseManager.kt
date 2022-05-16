package com.leapsy.player4b.contentManager

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseManager(aContext:Context?) : SQLiteOpenHelper(aContext,
    ContentMemberBook.DATABASE_NAME, null,
    ContentMemberBook.DATABASE_VERSION
) {

    override fun onCreate(aSQLiteDatabase: SQLiteDatabase?) {
        val createTable = ("CREATE TABLE " + ContentMemberBook.Member.TABLE_NAME
                + "(" + ContentMemberBook.Member.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ContentMemberBook.Member.MEDIA_ID + " TEXT,"
                + ContentMemberBook.Member.NAME + " TEXT,"
                + ContentMemberBook.Member.PATH + " TEXT,"
                + ContentMemberBook.Member.BUCKET + " TEXT,"
                + ContentMemberBook.Member.TYPE + " INTEGER,"
                + ContentMemberBook.Member.DURATION + " TEXT" + ")")

        aSQLiteDatabase?.execSQL(createTable)
    }

    override fun onUpgrade(aSQLiteDatabase: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        aSQLiteDatabase?.execSQL("DROP TABLE IF EXISTS " + ContentMemberBook.Member.TABLE_NAME)
        onCreate(aSQLiteDatabase)
    }

}