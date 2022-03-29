package com.example.itin

object UsernameUtil {

    // the input is not valid if the username contains whitespace characters or has less than 6 characters.
    fun validateUsernameInput(
        username: String
    ): Boolean {

        if (username.length < 6) {
            return false
        }

        val noWhiteSpace = Regex("^(.*\\s+.*)+\$")
        if (username.matches(noWhiteSpace)) {
            return false
        }

        return true
    }
}