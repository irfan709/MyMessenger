package com.example.mymessenger

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.example.mymessenger.databinding.ActivityOtpVerificationBinding
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class OtpVerification : AppCompatActivity() {
    private lateinit var binding: ActivityOtpVerificationBinding
    private lateinit var auth: FirebaseAuth
    private var sentcode: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val phoneNumber = intent.getStringExtra("phoneNumber")

        binding.etnum.text = phoneNumber

        val options = phoneNumber?.let {
            PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(it)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                        TODO("Not yet implemented")
                    }

                    override fun onVerificationFailed(p0: FirebaseException) {
                        TODO("Not yet implemented")
                    }

                    override fun onCodeSent(p0: String, p1: PhoneAuthProvider.ForceResendingToken) {
                        super.onCodeSent(p0, p1)
                        sentcode = p0
                        binding.otpprogress.visibility = View.INVISIBLE
                        val inputMethodManager =
                            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        inputMethodManager.showSoftInput(
                            binding.etnum,
                            InputMethodManager.SHOW_IMPLICIT
                        )
                        binding.otpView.requestFocus()
                    }
                }).build()
        }
        options?.let { PhoneAuthProvider.verifyPhoneNumber(it) }

        binding.otpView.setOtpCompletionListener { otp ->
            val credential = otp?.let { PhoneAuthProvider.getCredential(sentcode ?: "", it) }
            credential?.let {
                auth.signInWithCredential(it).addOnCompleteListener { task: Task<AuthResult> ->
                    if (task.isSuccessful) {
                        val intent = Intent(this@OtpVerification, SetupProfile::class.java)
                        startActivity(intent)
                        finishAffinity()
                    }
                }
            }
        }
    }
}