package de.velwebsolutions.mildcrowd.live.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Receives push hints over Firebase Cloud Messaging and hands them to [PushMessages]. Without google-services.json
 * Firebase never starts this service.
 */
@AndroidEntryPoint(FirebaseMessagingService::class)
class MildcrowdMessagingService : Hilt_MildcrowdMessagingService() {
    /** Shows pushes and registers new tokens. */
    @set:Inject lateinit var messages: PushMessages

    // Firebase still calls onNewToken for every new token; the deprecation is on the Hilt base, not on FCM.
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) = messages.tokenChanged()

    override fun onMessageReceived(message: RemoteMessage) = messages.received(message)
}
