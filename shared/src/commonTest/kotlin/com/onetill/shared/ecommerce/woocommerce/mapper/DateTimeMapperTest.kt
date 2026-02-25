package com.onetill.shared.ecommerce.woocommerce.mapper

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeMapperTest {

    @Test
    fun emptyStringReturnsEpochZero() {
        assertEquals(Instant.fromEpochMilliseconds(0), parseWooDateTime(""))
    }

    @Test
    fun isoWithoutTimezoneAppendZ() {
        val result = parseWooDateTime("2024-01-15T10:30:00")
        assertEquals(Instant.parse("2024-01-15T10:30:00Z"), result)
    }

    @Test
    fun isoWithZPassesThrough() {
        val result = parseWooDateTime("2024-01-15T10:30:00Z")
        assertEquals(Instant.parse("2024-01-15T10:30:00Z"), result)
    }

    @Test
    fun isoWithPlusOffsetPassesThrough() {
        val result = parseWooDateTime("2024-01-15T10:30:00+05:00")
        assertEquals(Instant.parse("2024-01-15T10:30:00+05:00"), result)
    }

    @Test
    fun garbageStringReturnsEpochZero() {
        assertEquals(Instant.fromEpochMilliseconds(0), parseWooDateTime("not-a-date"))
    }
}
