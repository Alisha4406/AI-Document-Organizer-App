package com.example.aidocumentorganizer.data.authentication

import android.util.Patterns
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest

class AuthManager {

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    sealed class AuthResult {
        data class Success(val name: String) : AuthResult()
        data class Error(val message: String) : AuthResult()
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun currentUserName(): String {
        return auth.currentUser?.displayName
            ?.takeIf { it.isNotBlank() }
            ?: auth.currentUser?.email?.substringBefore("@")
            ?: ""
    }

    fun currentUserEmail(): String {
        return auth.currentUser?.email ?: ""
    }

    fun signUp(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        onResult: (AuthResult) -> Unit
    ) {
        val trimmedName = name.trim()
        val normalizedEmail = email.trim()

        if (trimmedName.isBlank()) {
            onResult(AuthResult.Error("Please enter your name"))
            return
        }

        if (!isValidEmail(normalizedEmail)) {
            onResult(AuthResult.Error("Enter a valid email address"))
            return
        }

        if (password.length < 6) {
            onResult(AuthResult.Error("Password must be at least 6 characters"))
            return
        }

        if (password != confirmPassword) {
            onResult(AuthResult.Error("Passwords do not match"))
            return
        }

        auth.createUserWithEmailAndPassword(
            normalizedEmail,
            password
        ).addOnCompleteListener { task ->

            if (task.isSuccessful) {

                val user = task.result?.user

                if (user == null) {
                    onResult(
                        AuthResult.Error("Account was created, but user information is unavailable.")
                    )
                    return@addOnCompleteListener
                }

                val profileUpdate = UserProfileChangeRequest.Builder()
                    .setDisplayName(trimmedName)
                    .build()

                user.updateProfile(profileUpdate)
                    .addOnCompleteListener { profileTask ->

                        if (profileTask.isSuccessful) {
                            onResult(
                                AuthResult.Success(trimmedName)
                            )
                        } else {
                            // Account was successfully created even if
                            // profile name update failed.
                            onResult(
                                AuthResult.Success(trimmedName)
                            )
                        }
                    }

            } else {

                onResult(
                    AuthResult.Error(
                        friendlyError(
                            task.exception,
                            fallback = "Sign up failed. Please try again.",
                            isLogin = false
                        )
                    )
                )
            }
        }
    }

    fun login(
        email: String,
        password: String,
        onResult: (AuthResult) -> Unit
    ) {
        val normalizedEmail = email.trim()

        if (!isValidEmail(normalizedEmail)) {
            onResult(AuthResult.Error("Enter a valid email address"))
            return
        }

        if (password.isBlank()) {
            onResult(AuthResult.Error("Please enter your password"))
            return
        }

        auth.signInWithEmailAndPassword(
            normalizedEmail,
            password
        ).addOnCompleteListener { task ->

            if (task.isSuccessful) {

                onResult(
                    AuthResult.Success(currentUserName())
                )

            } else {

                onResult(
                    AuthResult.Error(
                        friendlyError(
                            task.exception,
                            fallback = "Login failed. Please try again.",
                            isLogin = true
                        )
                    )
                )
            }
        }
    }

    // Emails the signed-in user a password-reset link.
    fun sendPasswordReset(
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        val email = auth.currentUser?.email

        if (email.isNullOrBlank()) {
            onResult(false, "No email address found for this account.")
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, "Password reset link sent to $email")
                } else {
                    onResult(
                        false,
                        friendlyError(
                            task.exception,
                            fallback = "Could not send reset email.",
                            isLogin = true
                        )
                    )
                }
            }
    }

    fun logout() {
        auth.signOut()
    }

    // Turns Firebase's technical exceptions into short, readable messages.
    private fun friendlyError(
        exception: Exception?,
        fallback: String,
        isLogin: Boolean
    ): String = when (exception) {
        is FirebaseNetworkException ->
            "No internet connection. Check your network and try again."
        is FirebaseTooManyRequestsException ->
            "Too many attempts. Please wait a moment and try again."
        is FirebaseAuthUserCollisionException ->
            "An account with this email already exists. Try logging in."
        is FirebaseAuthWeakPasswordException ->
            "Password is too weak. Use at least 6 characters."
        is FirebaseAuthInvalidUserException ->
            if (isLogin) "Incorrect email or password."
            else "This account is unavailable."
        is FirebaseAuthInvalidCredentialsException ->
            if (isLogin) "Incorrect email or password."
            else "Enter a valid email address."
        else -> exception?.localizedMessage ?: fallback
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() &&
                Patterns.EMAIL_ADDRESS
                    .matcher(email)
                    .matches()
    }
}