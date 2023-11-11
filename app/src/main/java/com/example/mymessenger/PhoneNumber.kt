package com.example.mymessenger

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.mymessenger.databinding.ActivityPhoneNumberBinding
import com.google.firebase.auth.FirebaseAuth

class PhoneNumber : AppCompatActivity() {
    private lateinit var binding: ActivityPhoneNumberBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var countryCode: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneNumberBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        countryCode = binding.ccp.selectedCountryCodeWithPlus
        binding.number.requestFocus()

        binding.ccp.setOnCountryChangeListener {
            countryCode = binding.ccp.selectedCountryCodeWithPlus
        }

        binding.verifyptp.setOnClickListener {
            val number: String = binding.number.text.toString()
            val phoneNumber: String = countryCode + number

            if (number.isEmpty()) {
                binding.number.error = "Enter your number"
            } else {
                intent = Intent(this, OtpVerification::class.java)
                intent.putExtra("phoneNumber", phoneNumber)
                startActivity(intent)
            }
        }
    }
}