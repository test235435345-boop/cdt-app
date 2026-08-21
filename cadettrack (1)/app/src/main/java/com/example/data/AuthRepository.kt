package com.example.data

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.model.StaffMember
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
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

    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user ?: throw IllegalStateException("Authentication failed: User is null")
            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "signInWithEmail error", e)
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(
        email: String,
        pass: String,
        displayName: String,
        role: String
    ): Result<StaffMember> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user ?: throw IllegalStateException("Sign up failed: User is null")
            
            // Check bootstrap condition: does /appConfig/bootstrap exist?
            val bootstrapDoc = try {
                firestore.collection("appConfig").document("bootstrap").get().await()
            } catch (e: Exception) {
                null
            }
            val staffSnapshot = firestore.collection("staffMembers").get().await()
            val isFirstUser = (bootstrapDoc == null || !bootstrapDoc.exists()) && staffSnapshot.isEmpty

            val staffMember = StaffMember(
                uid = user.uid,
                email = user.email ?: email.trim(),
                displayName = displayName.ifBlank { user.email ?: "Staff" },
                role = role.ifBlank { if (isFirstUser) "Commanding Officer / Band Officer" else "Instructor" },
                authorizedSquadrons = emptyList(), // Admin can access all
                isAdmin = isFirstUser,
                isAuthorized = isFirstUser, // First user is auto-authorized; subsequent users are pending
                createdAt = System.currentTimeMillis()
            )

            firestore.collection("staffMembers").document(user.uid).set(staffMember).await()
            if (isFirstUser) {
                firestore.collection("appConfig").document("bootstrap")
                    .set(mapOf("initialized" to true, "adminUid" to user.uid, "createdAt" to System.currentTimeMillis()))
                    .await()
            }
            Result.success(staffMember)
        } catch (e: Exception) {
            Log.e("AuthRepository", "signUpWithEmail error", e)
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "sendPasswordReset error", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(context: Context, webClientId: String): Result<StaffMember> {
        return try {
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

                // Check existing staff profile or create new
                val staffDocRef = firestore.collection("staffMembers").document(user.uid)
                val staffDoc = staffDocRef.get().await()

                if (staffDoc.exists()) {
                    val staff = staffDoc.toObject(StaffMember::class.java) ?: StaffMember(uid = user.uid)
                    Result.success(staff)
                } else {
                    // Check if first user
                    val staffSnapshot = firestore.collection("staffMembers").get().await()
                    val isFirstUser = staffSnapshot.isEmpty

                    val newStaff = StaffMember(
                        uid = user.uid,
                        email = user.email ?: "",
                        displayName = user.displayName ?: user.email ?: "Staff",
                        role = if (isFirstUser) "Band Director" else "Instructor",
                        authorizedSquadrons = emptyList(),
                        isAdmin = isFirstUser,
                        isAuthorized = isFirstUser,
                        createdAt = System.currentTimeMillis()
                    )
                    staffDocRef.set(newStaff).await()
                    Result.success(newStaff)
                }
            } else {
                Result.failure(IllegalStateException("Unsupported credential type"))
            }
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
                Log.e("AuthRepository", "observeStaffMember error", error)
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
