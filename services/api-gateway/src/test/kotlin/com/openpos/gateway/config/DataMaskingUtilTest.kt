package com.openpos.gateway.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DataMaskingUtilTest {
    @Nested
    inner class MaskEmail {
        @Test
        fun `通常のメールアドレスをマスクする`() {
            // Arrange & Act
            val result = DataMaskingUtil.maskEmail("tanaka@example.com")

            // Assert
            assertEquals("ta***@example.com", result)
        }

        @Test
        fun `短いローカルパートをマスクする`() {
            // Arrange & Act
            val result = DataMaskingUtil.maskEmail("a@example.com")

            // Assert
            assertEquals("a***@example.com", result)
        }

        @Test
        fun `nullの場合は固定マスクを返す`() {
            // Arrange & Act
            val result = DataMaskingUtil.maskEmail(null)

            // Assert
            assertEquals("***", result)
        }

        @Test
        fun `空文字の場合は固定マスクを返す`() {
            // Arrange & Act
            val result = DataMaskingUtil.maskEmail("")

            // Assert
            assertEquals("***", result)
        }
    }

    @Nested
    inner class MaskPhone {
        @Test
        fun `日本の電話番号をマスクする`() {
            // Arrange & Act
            val result = DataMaskingUtil.maskPhone("09012345678")

            // Assert
            assertEquals("090-****-5678", result)
        }

        @Test
        fun `nullの場合は固定マスクを返す`() {
            // Arrange & Act
            val result = DataMaskingUtil.maskPhone(null)

            // Assert
            assertEquals("***", result)
        }

        @Test
        fun `短い番号は固定マスクを返す`() {
            // Arrange & Act
            val result = DataMaskingUtil.maskPhone("123")

            // Assert
            assertEquals("***", result)
        }
    }

    @Nested
    inner class MaskPin {
        @Test
        fun `PINは常に固定マスクを返す`() {
            // Arrange & Act & Assert
            assertEquals("****", DataMaskingUtil.maskPin("1234"))
            assertEquals("****", DataMaskingUtil.maskPin(null))
        }
    }

    @Nested
    inner class MaskName {
        @Test
        fun `名前の先頭2文字だけ表示する`() {
            // Arrange & Act
            val result = DataMaskingUtil.maskName("田中太郎")

            // Assert
            assertEquals("田中**", result)
        }

        @Test
        fun `短い名前はそのまま返す`() {
            // Arrange & Act
            val result = DataMaskingUtil.maskName("AB")

            // Assert
            assertEquals("AB", result)
        }

        @Test
        fun `nullの場合は固定マスクを返す`() {
            // Arrange & Act
            val result = DataMaskingUtil.maskName(null)

            // Assert
            assertEquals("***", result)
        }
    }

    @Nested
    inner class MaskMapForLogging {
        @Test
        fun `PIIフィールドを自動マスクする`() {
            // Arrange
            val data =
                mapOf<String, Any?>(
                    "id" to "123",
                    "email" to "tanaka@example.com",
                    "phone" to "09012345678",
                    "pin" to "1234",
                    "pinHash" to "\$2b\$12\$hash...",
                    "name" to "田中太郎",
                )

            // Act
            val result = DataMaskingUtil.maskMapForLogging(data)

            // Assert
            assertEquals("123", result["id"])
            assertEquals("ta***@example.com", result["email"])
            assertEquals("090-****-5678", result["phone"])
            assertEquals("****", result["pin"])
            assertEquals("***", result["pinHash"])
            assertEquals("田中太郎", result["name"]) // name はデフォルトでマスクしない
        }
    }
}
