package com.prafullk.upitracker.data.detection.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.prafullk.upitracker.domain.usecase.transaction.LogTransactionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SmsReceiver : BroadcastReceiver(), KoinComponent {

    private val logTransaction by inject<LogTransactionUseCase>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages.forEach { sms ->
            if (isBankSms(sms.originatingAddress)) {
                SmsParser.parse(sms)?.let { raw -> scope.launch { logTransaction(raw) } }
            }
        }
    }

    private fun isBankSms(address: String?): Boolean {
        if (address == null) return false
        val bankSenders = setOf("HDFCBK", "ICICIB", "SBIINB", "PAYTMB", "GPAY", "AXISBK")
        val rootAddress = address.uppercase().replace(Regex("[^A-Z]"), "")
        return bankSenders.any { rootAddress.contains(it) }
    }
}
