package com.openpos.store.entity

import com.openpos.store.entity.annotation.PersonalData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * @PersonalData アノテーションが PII フィールドに正しく付与されていることを検証する。
 * GDPR コンプライアンスのため、PII フィールドの漏れを検出する。
 */
class PersonalDataAnnotationTest {
    private fun getPersonalDataFields(clazz: Class<*>): List<Pair<String, PersonalData>> =
        clazz.declaredFields
            .filter { it.isAnnotationPresent(PersonalData::class.java) }
            .map { it.name to it.getAnnotation(PersonalData::class.java) }

    @Test
    fun `CustomerEntity の PII フィールドが @PersonalData で マークされている`() {
        // Act
        val piiFields = getPersonalDataFields(CustomerEntity::class.java)

        // Assert
        val fieldNames = piiFields.map { it.first }
        assertTrue(fieldNames.contains("name"), "name フィールドに @PersonalData が必要")
        assertTrue(fieldNames.contains("email"), "email フィールドに @PersonalData が必要")
        assertTrue(fieldNames.contains("phone"), "phone フィールドに @PersonalData が必要")
        assertEquals(3, piiFields.size, "CustomerEntity には 3 つの PII フィールドがあるべき")
    }

    @Test
    fun `StaffEntity の PII フィールドが @PersonalData でマークされている`() {
        // Act
        val piiFields = getPersonalDataFields(StaffEntity::class.java)

        // Assert
        val fieldNames = piiFields.map { it.first }
        assertTrue(fieldNames.contains("name"), "name フィールドに @PersonalData が必要")
        assertTrue(fieldNames.contains("email"), "email フィールドに @PersonalData が必要")
        assertTrue(fieldNames.contains("pinHash"), "pinHash フィールドに @PersonalData が必要")
        assertTrue(fieldNames.contains("hydraSubject"), "hydraSubject フィールドに @PersonalData が必要")
        assertEquals(4, piiFields.size, "StaffEntity には 4 つの PII フィールドがあるべき")
    }

    @Test
    fun `StoreEntity の PII フィールドが @PersonalData でマークされている`() {
        // Act
        val piiFields = getPersonalDataFields(StoreEntity::class.java)

        // Assert
        val fieldNames = piiFields.map { it.first }
        assertTrue(fieldNames.contains("address"), "address フィールドに @PersonalData が必要")
        assertTrue(fieldNames.contains("phone"), "phone フィールドに @PersonalData が必要")
        assertEquals(2, piiFields.size, "StoreEntity には 2 つの PII フィールドがあるべき")
    }

    @Test
    fun `AuditLogEntity の PII フィールドが @PersonalData でマークされている`() {
        // Act
        val piiFields = getPersonalDataFields(AuditLogEntity::class.java)

        // Assert
        val fieldNames = piiFields.map { it.first }
        assertTrue(fieldNames.contains("ipAddress"), "ipAddress フィールドに @PersonalData が必要")
        assertEquals(1, piiFields.size, "AuditLogEntity には 1 つの PII フィールドがあるべき")
    }

    @Test
    fun `PersonalData アノテーションに category が設定されている`() {
        // Act
        val piiFields = getPersonalDataFields(CustomerEntity::class.java)

        // Assert
        val categories = piiFields.associate { it.first to it.second.category }
        assertEquals("NAME", categories["name"])
        assertEquals("EMAIL", categories["email"])
        assertEquals("PHONE", categories["phone"])
    }
}
