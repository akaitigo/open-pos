package com.openpos.pos.grpc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PaginationHelperTest {
    private fun sanitizePageSize(requested: Int): Int = if (requested > 0) requested.coerceIn(1, 100) else 20

    @Test
    fun `pageSizeが100を超える場合は100に制限される`() {
        assertEquals(100, sanitizePageSize(101))
        assertEquals(100, sanitizePageSize(10000))
    }

    @Test
    fun `pageSizeが0以下の場合はデフォルトの20が使用される`() {
        assertEquals(20, sanitizePageSize(0))
        assertEquals(20, sanitizePageSize(-1))
    }

    @Test
    fun `正常なpageSizeはそのまま返される`() {
        assertEquals(1, sanitizePageSize(1))
        assertEquals(50, sanitizePageSize(50))
        assertEquals(100, sanitizePageSize(100))
    }
}
