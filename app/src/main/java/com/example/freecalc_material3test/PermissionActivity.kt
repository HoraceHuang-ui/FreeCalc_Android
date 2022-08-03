package com.example.freecalc_material3test

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.freecalc_material3test.databinding.ActivityPermissionBinding

class PermissionActivity : AppCompatActivity() {

    lateinit var binding: ActivityPermissionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)
        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.grantButton.setOnClickListener{
            Toast.makeText(this, "Please enable 'Allow management of all files' for FreeCalc.", Toast.LENGTH_SHORT).show()
            getPermission()
        }
        binding.denyButton.setOnClickListener {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            navigateToMainActivity(false)
        }
    }

    override fun onStart() {
        super.onStart()

        // TODO: Request file permissions and read / write settings
        if (checkPermissions()) {
            navigateToMainActivity(true)
        }
    }

    private fun checkPermissions(): Boolean {
        return Environment.isExternalStorageManager()
    }

    private fun getPermission() {
        val intent = Intent(ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        this.startActivity(intent)
    }

    private fun navigateToMainActivity(granted: Boolean) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("granted", granted)

        this.startActivity(intent)
    }
}
