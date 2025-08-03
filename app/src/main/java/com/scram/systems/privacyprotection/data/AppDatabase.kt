package com.scram.systems.privacyprotection.data
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [DnsServer::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dnsDao(): DnsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "privacy_protection_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Pre-populate the database on first creation
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = getDatabase(context).dnsDao()
                                if (dao.count() == 0) {
                                    dao.insert(DnsServer(name = "Google", primaryIp = "8.8.8.8"))
                                    dao.insert(DnsServer(name = "Cloudflare", primaryIp = "1.1.1.1"))
                                    dao.insert(DnsServer(name = "AdGuard", primaryIp = "94.140.14.14"))
                                    dao.insert(DnsServer(name = "OpenDNS", primaryIp = "208.67.222.222"))
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}