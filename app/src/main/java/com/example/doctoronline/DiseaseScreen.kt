package com.example.doctoronline

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.doctoronline.adapter.DiseaseAdopter
import com.example.doctoronline.databinding.ActivityDiseaseScreenBinding
import com.example.doctoronline.databinding.DialogProgressBinding
import com.example.doctoronline.model.DiseaseModel
import com.example.doctoronline.utils.Utils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import java.time.LocalTime

class DiseaseScreen : AppCompatActivity() {
    private val binding by lazy { ActivityDiseaseScreenBinding.inflate(layoutInflater) }
    private lateinit var diseaseList: ArrayList<DiseaseModel>
    private lateinit var db: FirebaseFirestore
    private lateinit var progressDialog: AlertDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
        val user = Utils.getUserData(this)
        if (user?.userImage!=""){
            binding.imageEmail.load(Utils.getUserData(this)?.userImage)
        }
        else{
            binding.imageEmail.setImageResource(R.drawable.placeholder)
        }

        binding.tvLogin.text = "Hi, " + Utils.getUserData(this)?.name

        wishUser()


        db = FirebaseFirestore.getInstance()

        progressDialog = AlertDialog.Builder(this, R.style.CustomAlertDialog).create()
        val progressDialogLay = DialogProgressBinding.inflate(LayoutInflater.from(this))
        progressDialog.setView(progressDialogLay.root)
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()


        getDisease()

    }

    private fun wishUser() {
        val currentTime = LocalTime.now()
        val greeting = when {
            currentTime.isAfter(LocalTime.parse("06:00:00")) && currentTime.isBefore(LocalTime.parse("12:00:00")) -> {
                "Good Morning.."
            }
            currentTime.isAfter(LocalTime.parse("12:00:00")) && currentTime.isBefore(LocalTime.parse("18:00:00")) -> {
                "Good Afternoon.."
            }
            else -> {
                "Good Evening.."
            }
        }

        binding.tvWish.text = greeting
    }

    private fun getDisease() {
        diseaseList = ArrayList()
        db.collection("Symptoms").get().addOnSuccessListener { symptoms ->
            diseaseList.clear()
            for (symptom in symptoms) {
                val sym = symptom.toObject<DiseaseModel>()
                diseaseList.add(sym)
            }
            binding.diseaseRv.adapter = DiseaseAdopter(this, diseaseList)
            progressDialog.dismiss()
        }.addOnFailureListener {
            Toast.makeText(this, "Something went Wrong", Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
        }
    }
}