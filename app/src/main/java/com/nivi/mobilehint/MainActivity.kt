package com.nivi.mobilehint

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.SubscriptionManager
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.material.snackbar.Snackbar
import com.nivi.mobilehint.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initResource()
    }

    private fun initResource() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.googlePhonePickerHeading.setOnClickListener { onGooglePhonePicker() }
        binding.subscriptionManagerHeading.setOnClickListener { onSubscriptionManager() }
    }

    private fun onGooglePhonePicker() {
        val request: GetPhoneNumberHintIntentRequest = GetPhoneNumberHintIntentRequest.builder().build()
        Identity.getSignInClient(this)
            .getPhoneNumberHintIntent(request)
            .addOnSuccessListener { result: PendingIntent ->
                try {
                    phoneNumberLauncher.launch(IntentSenderRequest.Builder(result).build())
                } catch (e: Exception) {
                    showMessage("Launching the PendingIntent failed")
                }
            }
            .addOnFailureListener {
//                showMessage("Phone Number Hint failed")
                onSubscriptionManager()
            }
    }

    private val phoneNumberLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        try {
            val phoneNumber = Identity.getSignInClient(this).getPhoneNumberFromIntent(it.data)
            binding.displayNumber.text = phoneNumber
        } catch (e: Exception) {
            showMessage("Phone Number Hint failed")
        }
    }

    private val subscriptionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (it.all { true }) {
            processSubscriptionManager()
        }
    }

    private fun onSubscriptionManager() {
        val permission = arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS)
        if (checkSelfPermission(permission.component1()) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(permission.component2()) == PackageManager.PERMISSION_GRANTED) {
            processSubscriptionManager()
        } else {
            subscriptionLauncher.launch(permission)
        }
    }

    @SuppressLint("MissingPermission")
    private fun processSubscriptionManager() {
        val subscriptionManager = getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val subsInfoList = subscriptionManager.activeSubscriptionInfoList
        val number = StringBuilder()

        for (subscriptionInfo in subsInfoList) {
            val isPrimary = subscriptionInfo.subscriptionId == SubscriptionManager.getDefaultSubscriptionId()
            val numberType = if (isPrimary) "Primary" else "Secondary"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                number.append("$numberType: ${subscriptionManager.getPhoneNumber(subscriptionInfo.subscriptionId)}\n")
            } else {
                number.append("$numberType: ${subscriptionInfo.number}\n")
            }
        }

        binding.displayNumber.text = number.toString()
    }


    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}