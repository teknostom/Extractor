package com.steelextractor

import net.minecraft.world.level.ChunkPos
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Stores MD5 hashes of chunk block data captured at each generation stage.
 */
object ChunkStageHashStorage {
    // Map of (chunkPos, stageName) -> hash
    private val hashes = ConcurrentHashMap<Pair<ChunkPos, String>, String>()

    // Track which chunks we're interested in (set by the extractor)
    private val trackedChunks = ConcurrentHashMap.newKeySet<ChunkPos>()

    // The seed we're tracking (set by extractor)
    @Volatile
    var trackedSeed: Long? = null

    fun startTracking(pos: ChunkPos) {
        trackedChunks.add(pos)
    }

    fun stopTracking(pos: ChunkPos) {
        trackedChunks.remove(pos)
    }

    fun isTracking(pos: ChunkPos): Boolean {
        return trackedChunks.contains(pos)
    }

    fun storeHash(pos: ChunkPos, stageName: String, hash: String) {
        hashes[Pair(pos, stageName)] = hash
    }

    fun getHash(pos: ChunkPos, stageName: String): String? {
        return hashes[Pair(pos, stageName)]
    }

    fun getAllHashes(): Map<Pair<ChunkPos, String>, String> {
        return hashes.toMap()
    }

    fun clear() {
        hashes.clear()
        trackedChunks.clear()
        trackedSeed = null
    }

    /**
     * Computes an MD5 hash of block data in a chunk's sections.
     */
    fun computeBlockHash(sections: Iterable<net.minecraft.world.level.chunk.LevelChunkSection>): String {
        val md = MessageDigest.getInstance("MD5")

        for (section in sections) {
            if (section.hasOnlyAir()) {
                // For empty sections, just hash a marker
                md.update(0.toByte())
            } else {
                // Hash the block states in the section
                val states = section.states
                for (y in 0 until 16) {
                    for (z in 0 until 16) {
                        for (x in 0 until 16) {
                            val state = states.get(x, y, z)
                            val stateId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getId(state.block)
                            // Write state ID as 4 bytes
                            md.update((stateId shr 24).toByte())
                            md.update((stateId shr 16).toByte())
                            md.update((stateId shr 8).toByte())
                            md.update(stateId.toByte())
                        }
                    }
                }
            }
        }

        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
