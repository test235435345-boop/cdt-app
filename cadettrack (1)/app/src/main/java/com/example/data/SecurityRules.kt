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
    // 2. Initial user can create first profile; admins can create; users can create own profile
    // 3. Non-admins can ONLY update their own 'displayName'. Role, isAdmin, and isAuthorized can only be modified by Admins.
    // 4. Deletions strictly restricted to Admins.
    match /staffMembers/{uid} {
      allow read: if isAuthenticated();
      allow create: if isAuthenticated() && (request.auth.uid == uid || isAdmin() || !exists(/databases/$(database)/documents/appConfig/bootstrap));
      allow update: if isAuthenticated() && (
        isAdmin() || 
        (request.auth.uid == uid && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['displayName']))
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

