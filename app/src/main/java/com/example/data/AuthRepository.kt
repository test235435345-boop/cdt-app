package com.example.data

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.model.StaffMember
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.security.MessageDigest
import java.util.UUID

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    val currentUser: FirebaseUser?
        get() = try {
            auth.currentUser
        } catch (e: Exception) {
            null
        }

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun getOrCreateStaffProfile(
        uid: String,
        email: String,
        displayName: String,
        preferredRole: String = "",
        preferredRank: String = ""
    ): StaffMember {
        val trimmedEmail = email.trim()
        val isDesignatedAdmin = trimmedEmail.equals("mop10015@gmail.com", ignoreCase = true) ||
                trimmedEmail.equals("snehal.guin@outlook.com", ignoreCase = true) ||
                uid == "b9hZz17DIZPXKbcoJPXwYyyENuD2"

        val staffDocRef = firestore.collection("staffMembers").document(uid)

        // 1. Try reading existing staff profile
        try {
            val snap = staffDocRef.get().await()
            if (snap != null && snap.exists()) {
                val existing = snap.toObject(StaffMember::class.java)
                if (existing != null) {
                    if (isDesignatedAdmin && (!existing.isAdmin || !existing.isAuthorized)) {
                        val updated = existing.copy(isAdmin = true, isAuthorized = true)
                        try {
                            staffDocRef.set(updated).await()
                        } catch (e: Exception) {
                            Log.w("AuthRepository", "Failed to update admin flag: ${e.message}")
                        }
                        return updated
                    }
                    return existing
                }
            }
        } catch (e: Exception) {
            Log.w("AuthRepository", "Error fetching staff profile document: ${e.message}")
        }

        // 2. Determine admin/authorization for new profiles:
        // Designated administrators (snehal.guin@outlook.com or mop10015@gmail.com) are automatically authorized.
        // ALL OTHER new staff registrations are created as PENDING (isAuthorized = false) and require administrator approval.
        val effectiveRole = if (preferredRole.isNotBlank()) {
            preferredRole
        } else if (isDesignatedAdmin) {
            "Commanding Officer / Band Officer"
        } else {
            "Instructor"
        }

        val fallbackName = if (displayName.isNotBlank()) displayName else if (trimmedEmail.isNotBlank()) trimmedEmail.substringBefore("@") else "Staff"

        val newStaff = StaffMember(
            uid = uid,
            email = trimmedEmail,
            displayName = fallbackName,
            rank = preferredRank.ifBlank { "Cdt" },
            role = effectiveRole,
            authorizedSquadrons = if (isDesignatedAdmin) listOf("1", "2") else emptyList(),
            isAdmin = isDesignatedAdmin,
            isAuthorized = isDesignatedAdmin,
            createdAt = System.currentTimeMillis()
        )

        // 3. Persist new profile
        try {
            staffDocRef.set(newStaff).await()
        } catch (e: Exception) {
            Log.w("AuthRepository", "Failed to write staff profile to Firestore: ${e.message}")
        }

        return newStaff
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<StaffMember> {
        val trimmedEmail = email.trim()
        val trimmedPass = pass.trim()
        return try {
            withTimeout(15000L) {
                val authResult = try {
                    auth.signInWithEmailAndPassword(trimmedEmail, trimmedPass).await()
                } catch (signInErr: Exception) {
                    val errMsg = signInErr.message ?: ""
                    // If user is not yet created in Auth, automatically try signup so test credentials work seamlessly
                    if (errMsg.contains("user-not-found", ignoreCase = true) ||
                        errMsg.contains("invalid-credential", ignoreCase = true) ||
                        errMsg.contains("There is no user record", ignoreCase = true)
                    ) {
                        try {
                            auth.createUserWithEmailAndPassword(trimmedEmail, trimmedPass).await()
                        } catch (createErr: Exception) {
                            throw signInErr
                        }
                    } else {
                        throw signInErr
                    }
                }

                val user = authResult.user ?: throw IllegalStateException("Authentication failed: User is null")
                val staff = getOrCreateStaffProfile(
                    uid = user.uid,
                    email = user.email ?: trimmedEmail,
                    displayName = user.displayName ?: trimmedEmail.substringBefore("@")
                )
                Result.success(staff)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e("AuthRepository", "signInWithEmail timeout", e)
            val user = auth.currentUser
            if (user != null) {
                val isAdm = trimmedEmail.equals("mop10015@gmail.com", ignoreCase = true) ||
                        trimmedEmail.equals("snehal.guin@outlook.com", ignoreCase = true)
                Result.success(
                    StaffMember(
                        uid = user.uid,
                        email = user.email ?: trimmedEmail,
                        displayName = user.displayName ?: trimmedEmail.substringBefore("@"),
                        isAdmin = isAdm,
                        isAuthorized = isAdm
                    )
                )
            } else {
                Result.failure(Exception("Request timed out — check your internet connection and try again."))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "signInWithEmail error", e)
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(
        email: String,
        pass: String,
        displayName: String,
        rank: String,
        role: String
    ): Result<StaffMember> {
        val trimmedEmail = email.trim()
        val trimmedPass = pass.trim()
        return try {
            withTimeout(15000L) {
                val authResult = try {
                    auth.createUserWithEmailAndPassword(trimmedEmail, trimmedPass).await()
                } catch (createErr: Exception) {
                    val errMsg = createErr.message ?: ""
                    if (errMsg.contains("email-already-in-use", ignoreCase = true) ||
                        errMsg.contains("already in use", ignoreCase = true)
                    ) {
                        // Email exists, try sign in with provided password
                        auth.signInWithEmailAndPassword(trimmedEmail, trimmedPass).await()
                    } else {
                        throw createErr
                    }
                }

                val user = authResult.user ?: throw IllegalStateException("Sign up failed: User is null")
                val staff = getOrCreateStaffProfile(
                    uid = user.uid,
                    email = user.email ?: trimmedEmail,
                    displayName = displayName.ifBlank { user.email?.substringBefore("@") ?: "Staff" },
                    preferredRole = role,
                    preferredRank = rank
                )
                Result.success(staff)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e("AuthRepository", "signUpWithEmail timeout", e)
            Result.failure(Exception("Request timed out — check your internet connection and try again."))
        } catch (e: Exception) {
            Log.e("AuthRepository", "signUpWithEmail error", e)
            Result.failure(e)
        }
    }

    suspend fun updateAccountPassword(newPass: String): Result<Unit> {
        return try {
            withTimeout(10000L) {
                val user = auth.currentUser ?: throw IllegalStateException("No active user session")
                user.updatePassword(newPass.trim()).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "updateAccountPassword error", e)
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            withTimeout(15000L) {
                auth.sendPasswordResetEmail(email.trim()).await()
                Result.success(Unit)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e("AuthRepository", "sendPasswordReset timeout", e)
            Result.failure(Exception("Request timed out — check your internet connection and try again."))
        } catch (e: Exception) {
            Log.e("AuthRepository", "sendPasswordReset error", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(context: Context, webClientId: String): Result<StaffMember> {
        return try {
            withTimeout(15000L) {
                val credentialManager = CredentialManager.create(context)
                
                // Generate nonce for security
                val rawNonce = UUID.randomUUID().toString()
                val bytes = rawNonce.toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(bytes)
                val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .setNonce(hashedNonce)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result: GetCredentialResponse = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = auth.signInWithCredential(firebaseCredential).await()
                    val user = authResult.user ?: throw IllegalStateException("Google sign-in user is null")

                    val staff = getOrCreateStaffProfile(
                        uid = user.uid,
                        email = user.email ?: "",
                        displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Staff",
                        preferredRole = ""
                    )
                    Result.success(staff)
                } else {
                    Result.failure(IllegalStateException("Unsupported credential type"))
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e("AuthRepository", "signInWithGoogle timeout", e)
            Result.failure(Exception("Request timed out — check your internet connection and try again."))
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Sign in was cancelled"))
        } catch (e: Exception) {
            Log.e("AuthRepository", "signInWithGoogle error", e)
            Result.failure(e)
        }
    }

    fun observeStaffMember(uid: String): Flow<StaffMember?> = callbackFlow {
        val docRef = firestore.collection("staffMembers").document(uid)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("AuthRepository", "observeStaffMember notice: ${error.message}")
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val staff = snapshot.toObject(StaffMember::class.java)
                trySend(staff)
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun getStaffMember(uid: String): StaffMember? {
        return try {
            val doc = firestore.collection("staffMembers").document(uid).get().await()
            if (doc.exists()) doc.toObject(StaffMember::class.java) else null
        } catch (e: Exception) {
            null
        }
    }

    fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.e("AuthRepository", "signOut error", e)
        }
    }
}
