@file:Suppress("DEPRECATION")

package com.example.doctoronline

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.doctoronline.databinding.ActivityLoginScreenBinding
import com.example.doctoronline.model.UserModel
import com.example.doctoronline.utils.Utils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginScreen : AppCompatActivity() {
    private val binding by lazy { ActivityLoginScreenBinding.inflate(layoutInflater) }

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase

    private lateinit var googleSignInClient: GoogleSignInClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val update = intent.getBooleanExtra("updatePassword", false)
        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance()

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this@LoginScreen, RegistrationScreen::class.java))
            finish()

        }


        binding.btnLogin.setOnClickListener {
            val email = binding.editTextTextEmailAddress.text.toString().trim()
            val password = binding.editTextTextPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {


                signInWithEmailAndPassword(email, password)
            } else {
                Toast.makeText(this, "Email and password cannot be empty", Toast.LENGTH_SHORT)
                    .show()
            }

        }

        binding.tvForgetPassword.setOnClickListener {
            startActivity(Intent(this, forgot_password::class.java))

        }






        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this@LoginScreen, gso)

        binding.buttonLoginWithGoogle.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }


    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleResults(task)
        } else {
            Toast.makeText(this@LoginScreen, "Failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleResults(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful) {

            val account: GoogleSignInAccount? = task.result
            if (account != null) {
                updateUI(account)
            }
        } else {
            Toast.makeText(this@LoginScreen, "Sign In Failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(account: GoogleSignInAccount) {
        val credentials = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credentials).addOnCompleteListener {

            if (it.isSuccessful) {
                val currentUser: FirebaseUser? = auth.currentUser
                if (currentUser != null) {

                    Handler(Looper.getMainLooper()).postDelayed({
                        val intent = Intent(this@LoginScreen, RegistrationScreen::class.java)
                        intent.putExtra("google", true)
                        startActivity(intent)
                        finish()
                    }, 2000)


                }
                val progressDialog = ProgressDialog(this).apply {
                    setMessage("Verifying Your Email...")
                    show()
                }


            } else {
                Toast.makeText(this@LoginScreen, "Something went Wrong", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun signInWithEmailAndPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    val user = auth.currentUser
                    updateUI(user)

                    val progressDialog = ProgressDialog(this).apply {
                        setMessage("Verifying...")
                        show()
                    }


                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(
                        this, "Authentication Failed. ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateUI(null)

                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {

        if (user != null) {
            db.getReference("Users").child(user.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user1: UserModel = snapshot.getValue(UserModel::class.java)!!
                        Utils.saveUserData(this@LoginScreen,user1)

                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }

                })
            // User is signed in, navigate to the main activity or perform any other action

            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }, 2000)
        } else {
            // User is signed out or authentication failed, update UI accordingly
            // For example, display an error message on the screen
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Email sent successfully, redirect to a new screen
                    Toast.makeText(
                        this,
                        "Password reset email sent to $email",
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToNewScreen()
                } else {
                    // If the email is not found or other issues occur

                    Toast.makeText(
                        this,
                        "Failed to send password reset email. ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun checkIfEmailIsRegistered(email: String) {
        auth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val providers = task.result?.signInMethods
                    if (providers == null || providers.isEmpty()) {
                        // Email is not registered
                        Toast.makeText(
                            this,
                            "This email is not registered. Please sign up.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // Email is registered, proceed with password reset
                        sendPasswordResetEmail(email)
                    }
                } else {

                    Toast.makeText(
                        this,
                        "Error checking email registration status. ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }


    private fun navigateToNewScreen() {

        val intent = Intent(this, WelcomeScreen::class.java)
        startActivity(intent)
        finish() // Optionally finish the current activity
    }


    private fun update_resetPass(btnName: String, title: String) {
        binding.buttonLoginWithGoogle.visibility = View.INVISIBLE
        binding.editTextTextPassword.visibility = View.GONE
        binding.tvForgetPassword.visibility = View.GONE
        binding.tvRegister.visibility = View.GONE
        binding.backBtn.visibility = View.VISIBLE
        binding.titleText.text = title
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
        binding.btnLogin.setText(btnName)

        binding.btnLogin.setOnClickListener {
            val email = binding.editTextTextEmailAddress.text.toString().trim()

            if (email.isNotEmpty()) {
                val progressDialog = ProgressDialog(this).apply {
                    setMessage("Sending...")
                    show()
                }

                //  checkIfEmailIsRegistered(email)
                sendPasswordResetEmail(email)
            } else {
                Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }





}