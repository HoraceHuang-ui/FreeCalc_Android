package com.example.freecalc_material3test

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import com.example.freecalc_material3test.databinding.ActivitySettingsBinding
import com.google.android.material.slider.Slider

class SettingsActivity : AppCompatActivity() {
    var decAccu = 5
    var deg = false

    lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        val actionBar = supportActionBar!!
        actionBar.setDisplayHomeAsUpEnabled(true)

        if (intent.hasExtra("settingsBundle")) {
            deg = intent?.extras?.getBundle("settingsBundle")?.getBoolean("degMode")!!
            decAccu = intent?.extras?.getBundle("settingsBundle")?.getInt("decAccu")!!
            binding.modeSelect.isChecked = deg
            binding.accuracySlider.value = decAccu.toFloat()
            binding.modeSelect.text = when (binding.modeSelect.isChecked) {
                true -> {
                    getText(R.string.menu_item_deg)
                }
                false -> {
                    getText(R.string.menu_item_rad)
                }
            }
            binding.sliderDesc.text = "%s%d".format(getText(R.string.accuracy), decAccu)
        }

        binding.accuracySlider.addOnChangeListener(accuracySliderListener())
        binding.modeSelect.setOnCheckedChangeListener(modeSelectSwitchListener())

        binding.accuracySlider.value = decAccu.toFloat()
        binding.modeSelect.isChecked = deg
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.

        menuInflater.inflate(R.menu.menu_settings, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            android.R.id.home -> {
                val intent = Intent(this, MainActivity::class.java)
                this.startActivity(intent)
                true
            }
            R.id.action_done -> {
                val intent = Intent(this, MainActivity::class.java)
                val bundle = Bundle()
                bundle.putInt("decAccu", decAccu)
                bundle.putBoolean("degMode", deg)
                intent.putExtra("settingsBundle", bundle)
                this.startActivity(intent)
                this.finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun accuracySliderListener(): Slider.OnChangeListener {
        return Slider.OnChangeListener { slider, _, _ ->
            decAccu = slider.value.toInt()
            binding.sliderDesc.text = "%s%d".format(getText(R.string.accuracy), decAccu)
        }
    }
    private fun modeSelectSwitchListener(): CompoundButton.OnCheckedChangeListener {
        return CompoundButton.OnCheckedChangeListener{ _, checkedId ->
            when (checkedId) {
                true -> {
                    deg = true
                    binding.modeSelect.text = getText(R.string.menu_item_deg)
                }
                false -> {
                    deg = false
                    binding.modeSelect.text = getText(R.string.menu_item_rad)
                }
            }
        }
    }
}