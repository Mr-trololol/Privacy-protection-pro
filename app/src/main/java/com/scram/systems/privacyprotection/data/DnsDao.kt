package com.scram.systems.privacyprotection.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DnsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(server: DnsServer)

    @Update
    suspend fun update(server: DnsServer)

    @Delete
    suspend fun delete(server: DnsServer)

    @Query("SELECT * FROM dns_servers ORDER BY name ASC")
    fun getAllServers(): Flow<List<DnsServer>>

    @Query("SELECT COUNT(*) FROM dns_servers")
    suspend fun count(): Int
}
