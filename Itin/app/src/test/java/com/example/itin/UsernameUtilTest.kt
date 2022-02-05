package com.example.itin


import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UsernameUtilTest {

    @Test
    fun `less than 6 characters return false` () {
        val result = UsernameUtil.validateUsernameInput("trinh")
        assertThat(result).isFalse()
    }

    @Test
    fun `contain whitespaces return false` () {
        val result = UsernameUtil.validateUsernameInput("trinh vo")
        assertThat(result).isFalse()
    }

    @Test
    fun `valid username return true` () {
        val result = UsernameUtil.validateUsernameInput("trinhvo1020")
        assertThat(result).isTrue()
    }
}