package com.example.android_manager.data.permissions

/**
 * Plain-language explanation for Android permissions, clarifying everyday functionality,
 * legitimate developer use-cases, and potential security risks when misused by malware.
 */
data class PermissionExplanation(
    val simpleName: String,
    val plainSummary: String,
    val legitimateUse: String,
    val securityRisk: String
)

object PermissionExplanations {

    private val explanations = mapOf(
        "CAMERA" to PermissionExplanation(
            simpleName = "Camera",
            plainSummary = "Allows the application to capture still photos and record video streams at any time.",
            legitimateUse = "Taking photos, video conferencing, scanning QR codes, or visual search.",
            securityRisk = "A malicious app could silently capture visual footage of surroundings or users without consent."
        ),
        "RECORD_AUDIO" to PermissionExplanation(
            simpleName = "Microphone",
            plainSummary = "Allows the application to record sound and ambient audio via the device microphone.",
            legitimateUse = "Voice messaging, video calling, voice notes, and speech recognition.",
            securityRisk = "Rogue applications can record private conversations, ambient discussions, and exfiltrate audio recordings."
        ),
        "ACCESS_FINE_LOCATION" to PermissionExplanation(
            simpleName = "Precise Location (GPS)",
            plainSummary = "Grants access to high-accuracy device location via GPS, Wi-Fi, and cellular towers.",
            legitimateUse = "Navigation, ride-sharing, geotagging, delivery tracking, and fitness run recording.",
            securityRisk = "Stalking, real-time physical movement tracking, and reconstructing daily lifestyle habits."
        ),
        "ACCESS_COARSE_LOCATION" to PermissionExplanation(
            simpleName = "Approximate Location",
            plainSummary = "Grants access to approximate location derived from cellular cell IDs and public Wi-Fi networks.",
            legitimateUse = "Local weather reports, city-level personalized news, and regional compliance.",
            securityRisk = "Tracking user presence within city neighborhoods and targeted ad profiling."
        ),
        "READ_CONTACTS" to PermissionExplanation(
            simpleName = "Read Contacts",
            plainSummary = "Permits reading the user's address book, contact names, email addresses, and phone numbers.",
            legitimateUse = "Inviting friends to a platform, syncing phonebook, and caller ID lookup.",
            securityRisk = "Harvesting contact lists for phishing attacks, spam distribution, or social engineering."
        ),
        "WRITE_CONTACTS" to PermissionExplanation(
            simpleName = "Modify Contacts",
            plainSummary = "Permits adding, editing, or deleting entries in the device contact storage.",
            legitimateUse = "Syncing corporate contacts, restoring address book backups, or phonebook organizers.",
            securityRisk = "Rogue apps could silently alter phone numbers to route calls to premium rate numbers or wipe contacts."
        ),
        "READ_CALL_LOG" to PermissionExplanation(
            simpleName = "Read Call Log",
            plainSummary = "Permits reading telephony history, including dialed numbers, incoming callers, timestamps, and call duration.",
            legitimateUse = "Dialer replacements, call management utilities, and spam call blocker engines.",
            securityRisk = "Surveillance of personal and business associations, revealing caller patterns and confidential contacts."
        ),
        "WRITE_CALL_LOG" to PermissionExplanation(
            simpleName = "Modify Call Log",
            plainSummary = "Permits editing or erasing entries from the device telephony call log.",
            legitimateUse = "Privacy cleaners, dialer replacements, and specialized call management tools.",
            securityRisk = "Covering tracks after unauthorized outgoing calls or masking calls to fraudulent services."
        ),
        "READ_PHONE_STATE" to PermissionExplanation(
            simpleName = "Read Phone Status & Identity",
            plainSummary = "Allows read access to cellular network state, ongoing calls, SIM state, and carrier info.",
            legitimateUse = "Pausing media during active phone calls, network diagnostics, and dual-SIM management.",
            securityRisk = "Device fingerprinting and associating physical phone usage with tracked online identities."
        ),
        "CALL_PHONE" to PermissionExplanation(
            simpleName = "Direct Phone Call",
            plainSummary = "Allows initiating phone calls directly without routing through the system dialer UI.",
            legitimateUse = "One-tap emergency dialing tools or dedicated telephony assistants.",
            securityRisk = "Malware can silently place toll calls or premium numbers, generating unexpected financial charges."
        ),
        "SEND_SMS" to PermissionExplanation(
            simpleName = "Send SMS Messages",
            plainSummary = "Permits sending SMS text messages directly from the device in the background.",
            legitimateUse = "SMS messaging clients, 2FA verification beacons, and emergency alert transmitters.",
            securityRisk = "Fraudulent subscription to premium rate SMS services and spreading malware links to contacts."
        ),
        "RECEIVE_SMS" to PermissionExplanation(
            simpleName = "Receive SMS Messages",
            plainSummary = "Allows intercepting and monitoring incoming SMS messages as they arrive.",
            legitimateUse = "Automatic verification code (OTP) autofill and SMS spam filtering.",
            securityRisk = "Interception of one-time passwords (OTPs) and multi-factor authorization codes from banks."
        ),
        "READ_SMS" to PermissionExplanation(
            simpleName = "Read Stored SMS",
            plainSummary = "Allows reading existing text messages and conversational SMS history stored on the device.",
            legitimateUse = "Default messaging applications, SMS backup managers, and expense trackers.",
            securityRisk = "Exposure of private financial transaction notifications, personal chats, and archived OTP codes."
        ),
        "READ_EXTERNAL_STORAGE" to PermissionExplanation(
            simpleName = "Read Shared Storage",
            plainSummary = "Grants access to read photos, documents, and downloads on device shared storage.",
            legitimateUse = "Document viewers, media players, file managers, and cloud backup tools.",
            securityRisk = "Unauthorized inspection of private photos, downloaded documents, and sensitive local files."
        ),
        "WRITE_EXTERNAL_STORAGE" to PermissionExplanation(
            simpleName = "Write Shared Storage",
            plainSummary = "Permits writing, modifying, and deleting files on device shared storage.",
            legitimateUse = "Saving downloaded media, saving photo edits, and file management.",
            securityRisk = "Ransomware encryption, altering downloaded installers, and unauthorized file destruction."
        ),
        "MANAGE_EXTERNAL_STORAGE" to PermissionExplanation(
            simpleName = "All Files Access",
            plainSummary = "Special high-privilege access granting unrestricted read and write permissions to all device files.",
            legitimateUse = "Full-featured file manager utilities, disk partition tools, and complete system backup apps.",
            securityRisk = "Complete bypass of Android scoped storage, giving full access to everything on the user device."
        ),
        "SYSTEM_ALERT_WINDOW" to PermissionExplanation(
            simpleName = "Display Over Other Apps",
            plainSummary = "Allows drawing custom overlay floating windows above all other running applications.",
            legitimateUse = "Chat heads, screen recorders, call overlays, and assistive accessibility tools.",
            securityRisk = "Tapjacking attacks (tricking user into clicking hidden permissions) and fake banking login phishing overlays."
        ),
        "WRITE_SETTINGS" to PermissionExplanation(
            simpleName = "Modify System Settings",
            plainSummary = "Allows modifying global Android operating system settings and hardware configurations.",
            legitimateUse = "Ringtone managers, automated brightness managers, and device customizers.",
            securityRisk = "Disabling security features, changing network routing, or altering lock screen behaviors."
        ),
        "REQUEST_INSTALL_PACKAGES" to PermissionExplanation(
            simpleName = "Install Unknown Apps",
            plainSummary = "Allows requesting the installation of new application packages (APKs) from within the app.",
            legitimateUse = "Alternative app stores (F-Droid), enterprise MDM agents, and browser APK downloads.",
            securityRisk = "Trojan dropper behavior: downloading and executing second-stage malicious payloads."
        ),
        "PACKAGE_USAGE_STATS" to PermissionExplanation(
            simpleName = "App Usage Access",
            plainSummary = "Allows monitoring which applications you use, how frequently you open them, and network usage per app.",
            legitimateUse = "Digital wellbeing monitors, app lockers, and process diagnostics (like APVM).",
            securityRisk = "Profiling exact daily routine, identifying installed financial/crypto apps for targeted exploitation."
        ),
        "ACCESS_NOTIFICATION_POLICY" to PermissionExplanation(
            simpleName = "Do Not Disturb Access",
            plainSummary = "Allows reading and altering system Do Not Disturb (DND) modes and notification priority.",
            legitimateUse = "Scheduled focus mode utilities, calendar auto-silence, and driving mode companions.",
            securityRisk = "Silencing security warnings or suppressing incoming transaction alerts during an attack."
        ),
        "POST_NOTIFICATIONS" to PermissionExplanation(
            simpleName = "Post Notifications",
            plainSummary = "Allows displaying banner notifications, badges, and alerts in the status bar.",
            legitimateUse = "Chat messages, news updates, reminders, and background process status indicators.",
            securityRisk = "Spamming aggressive deceptive advertisements or masquerading as system security alerts."
        ),
        "READ_MEDIA_IMAGES" to PermissionExplanation(
            simpleName = "Read Photos & Images",
            plainSummary = "Scoped permission on Android 13+ granting access to the user's photo and image library.",
            legitimateUse = "Photo editors, gallery apps, social media post creation, and avatar uploaders.",
            securityRisk = "Exfiltration of personal photo library and inspection of embedded EXIF GPS metadata."
        ),
        "READ_MEDIA_VIDEO" to PermissionExplanation(
            simpleName = "Read Videos",
            plainSummary = "Scoped permission on Android 13+ granting access to user recorded and stored video files.",
            legitimateUse = "Video players, video editor suites, and streaming media uploaders.",
            securityRisk = "Unauthorized access to recorded private home videos and confidential recordings."
        ),
        "READ_MEDIA_AUDIO" to PermissionExplanation(
            simpleName = "Read Music & Audio",
            plainSummary = "Scoped permission on Android 13+ granting access to stored music tracks and audio recordings.",
            legitimateUse = "Music player utilities, audio tag editors, and playlist creators.",
            securityRisk = "Accessing saved voice memos, audio recordings, and personal media files."
        ),
        "BODY_SENSORS" to PermissionExplanation(
            simpleName = "Body Sensors",
            plainSummary = "Permits reading biometric body sensor data such as heart rate monitors and pulse oximeters.",
            legitimateUse = "Health, fitness, and workout tracking companions.",
            securityRisk = "Harvesting private physiological and health information without explicit awareness."
        ),
        "ACTIVITY_RECOGNITION" to PermissionExplanation(
            simpleName = "Physical Activity Recognition",
            plainSummary = "Allows detecting when the user is walking, running, cycling, or travelling in a vehicle.",
            legitimateUse = "Pedometer step tracking, commute recorders, and driving detection mode.",
            securityRisk = "Building physical behavioral profiles and inferring daily user schedule patterns."
        )
    )

    fun getExplanation(rawName: String): PermissionExplanation {
        val simpleKey = rawName.substringAfterLast(".").uppercase()
        return explanations[simpleKey] ?: PermissionExplanation(
            simpleName = simpleKey.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
            plainSummary = "Accesses standard Android platform subsystem capability ($simpleKey).",
            legitimateUse = "Standard application operation and feature enablement.",
            securityRisk = "Potential elevation of capability or access if utilized by untrusted code."
        )
    }
}
