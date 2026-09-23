package com.example.data.remote

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Authenticating : AuthState()
    data class Authenticated(val user: FirebaseUser, val provider: String = "firebase") : AuthState()
    data class Error(val message: String) : AuthState()
}

class FirebaseAuthManager(private val context: Context) {
    private val TAG = "FirebaseAuthManager"

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var auth: FirebaseAuth? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    init {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val authInstance = FirebaseAuth.getInstance()
                auth = authInstance
                val current = authInstance.currentUser
                if (current != null) {
                    _authState.value = AuthState.Authenticated(current)
                }

                // Attach reactive listener for token expiration, revocation, or sign-out
                val listener = FirebaseAuth.AuthStateListener { fbAuth ->
                    val user = fbAuth.currentUser
                    if (user != null) {
                        _authState.value = AuthState.Authenticated(user)
                    } else if (_authState.value !is AuthState.Authenticating) {
                        _authState.value = AuthState.Unauthenticated
                    }
                }
                authStateListener = listener
                authInstance.addAuthStateListener(listener)
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth not initialized: ${e.message}")
        }
    }

    /**
     * Authenticates with Google via Android Jetpack CredentialManager.
     * Accepts optional [activity] context to safely bind bottom sheets on Android 14+.
     */
    suspend fun signInWithGoogle(
        activity: Activity? = null,
        webClientId: String? = null
    ): Result<FirebaseUser> {
        _authState.value = AuthState.Authenticating
        return try {
            val targetContext = activity ?: context
            val credentialManager = CredentialManager.create(targetContext)

            // Resolve server web client ID or default placeholder
            val serverClientId = webClientId?.ifEmpty { null }
                ?: try {
                    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                    if (resId != 0) context.getString(resId) else "clubledger-google-auth.apps.googleusercontent.com"
                } catch (e: Exception) {
                    "clubledger-google-auth.apps.googleusercontent.com"
                }

            val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context = targetContext, request = request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)

                val authInstance = auth ?: FirebaseAuth.getInstance()
                val authResult = authInstance.signInWithCredential(authCredential).await()
                val user = authResult.user
                if (user != null) {
                    _authState.value = AuthState.Authenticated(user, "Google")
                    Result.success(user)
                } else {
                    _authState.value = AuthState.Error("Sign in returned empty user")
                    Result.failure(Exception("Empty user returned"))
                }
            } else {
                _authState.value = AuthState.Error("Unrecognized credential type")
                Result.failure(Exception("Unrecognized credential type"))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Google Sign-In cancelled by user")
            _authState.value = AuthState.Unauthenticated
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            _authState.value = AuthState.Error(e.localizedMessage ?: "Google Authentication failed")
            Result.failure(e)
        }
    }

    /**
     * Authenticates with Apple ID using Firebase OAuthProvider
     */
    suspend fun signInWithApple(activity: Activity): Result<FirebaseUser> {
        _authState.value = AuthState.Authenticating
        return try {
            val authInstance = auth ?: FirebaseAuth.getInstance()
            val provider = OAuthProvider.newBuilder("apple.com")
            provider.scopes = listOf("email", "name")

            val pendingResult = authInstance.pendingAuthResult
            val authResult = if (pendingResult != null) {
                pendingResult.await()
            } else {
                authInstance.startActivityForSignInWithProvider(activity, provider.build()).await()
            }

            val user = authResult.user
            if (user != null) {
                _authState.value = AuthState.Authenticated(user, "Apple")
                Result.success(user)
            } else {
                _authState.value = AuthState.Error("Apple Sign-In returned empty user")
                Result.failure(Exception("Apple Sign-In returned empty user"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Apple Sign-In failed", e)
            _authState.value = AuthState.Error(e.localizedMessage ?: "Apple Authentication failed")
            Result.failure(e)
        }
    }

    /**
     * Authenticates with Facebook using Firebase OAuthProvider
     */
    suspend fun signInWithFacebook(activity: Activity): Result<FirebaseUser> {
        _authState.value = AuthState.Authenticating
        return try {
            val authInstance = auth ?: FirebaseAuth.getInstance()
            val provider = OAuthProvider.newBuilder("facebook.com")
            provider.scopes = listOf("email", "public_profile")

            val pendingResult = authInstance.pendingAuthResult
            val authResult = if (pendingResult != null) {
                pendingResult.await()
            } else {
                authInstance.startActivityForSignInWithProvider(activity, provider.build()).await()
            }

            val user = authResult.user
            if (user != null) {
                _authState.value = AuthState.Authenticated(user, "Facebook")
                Result.success(user)
            } else {
                _authState.value = AuthState.Error("Facebook Sign-In returned empty user")
                Result.failure(Exception("Facebook Sign-In returned empty user"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Facebook Sign-In failed", e)
            _authState.value = AuthState.Error(e.localizedMessage ?: "Facebook Authentication failed")
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Sign out error", e)
        }
        _authState.value = AuthState.Unauthenticated
    }
}
