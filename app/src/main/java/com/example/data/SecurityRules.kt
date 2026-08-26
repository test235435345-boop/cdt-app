package com.example.data

object SecurityRules {
    const val RULES_TEXT = """rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper function to check if caller is an authenticated user
    function isAuthenticated() {
      return request.auth != null;
    }

    // Helper function to check if caller is an authorized staff member
    function isAuthorizedStaff() {
      return isAuthenticated() &&
        exists(/databases/$(database)/documents/staffMembers/$(request.auth.uid)) &&
        get(/databases/$(database)/documents/staffMembers/$(request.auth.uid)).data.isAuthorized == true;
    }
    
    // Helper function to check if caller is an administrator
    function isAdmin() {
      return isAuthenticated() &&
        exists(/databases/$(database)/documents/staffMembers/$(request.auth.uid)) &&
        get(/databases/$(database)/documents/staffMembers/$(request.auth.uid)).data.isAdmin == true;
    }

    // Bootstrap configuration document (/appConfig/bootstrap)
    // Tracks initialization state to safely authorize first administrator
    match /appConfig/bootstrap {
      allow read: if isAuthenticated();
      allow create, update: if isAuthenticated() && (!exists(/databases/$(database)/documents/appConfig/bootstrap) || isAdmin());
    }
    
    // Staff Members Collection
    // 1. Authenticated users can read staff profiles
    // 2. Initial user can bootstrap admin only if /appConfig/bootstrap does NOT exist.
    // 3. Any subsequent self-registration MUST have isAdmin == false and isAuthorized == false.
    // 4. Admins can create and authorize other staff profiles.
    // 5. Non-admins can update their own 'displayName', 'rank', and 'role'. 'isAdmin', 'isAuthorized', and 'authorizedSquadrons' can only be modified by Admins.
    // 6. Deletions strictly restricted to Admins.
    match /staffMembers/{uid} {
      allow read: if isAuthenticated();
      allow create: if isAuthenticated() && (
        isAdmin() || 
        (!exists(/databases/$(database)/documents/appConfig/bootstrap) && request.auth.uid == uid && request.resource.data.isAdmin == true && request.resource.data.isAuthorized == true) ||
        (exists(/databases/$(database)/documents/appConfig/bootstrap) && request.auth.uid == uid && request.resource.data.isAdmin == false && request.resource.data.isAuthorized == false)
      );
      allow update: if isAuthenticated() && (
        isAdmin() || 
        (request.auth.uid == uid && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['displayName', 'rank', 'role']))
      );
      allow delete: if isAdmin();
    }
    
    // Organization Settings
    // Readable by authorized staff, writeable by Admins or during initial setup
    match /orgSettings/{docId} {
      allow read: if isAuthenticated();
      allow write: if isAdmin() || (isAuthenticated() && !exists(/databases/$(database)/documents/orgSettings/settings));
    }
    
    // Cadets: strictly require authorized staff member
    match /cadets/{cadetId} {
      allow read, write: if isAuthorizedStaff();
    }
    
    // Events: strictly require authorized staff member
    match /events/{eventId} {
      allow read, write: if isAuthorizedStaff();
    }
    
    // Attendance Records: strictly require authorized staff member
    match /attendanceRecords/{recordId} {
      allow read, write: if isAuthorizedStaff();
    }
  }
}"""
}

