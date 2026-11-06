package com.smartphonetop.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.smartphonetop.app.R
import com.smartphonetop.app.core.DeviceStatsReader
import com.smartphonetop.app.databinding.ActivityMainBinding
import com.smartphonetop.app.ui.fragments.AppsFragment
import com.smartphonetop.app.ui.fragments.CleanFragment
import com.smartphonetop.app.ui.fragments.FilesFragment
import com.smartphonetop.app.ui.fragments.HomeFragment
import com.smartphonetop.app.ui.fragments.MoreFragment
import com.smartphonetop.app.util.ThemeUtils

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selected = R.id.navHome

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeUtils.darkSystemBars(this)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
        val stats = DeviceStatsReader.read(this)
        binding.deviceName.text = "${stats.manufacturer} ${stats.model}  •  ${stats.androidVersion.split(" ").firstOrNull()}"
        binding.navHome.setOnClickListener { select(R.id.navHome) }
        binding.navClean.setOnClickListener { select(R.id.navClean) }
        binding.navApps.setOnClickListener { select(R.id.navApps) }
        binding.navFiles.setOnClickListener { select(R.id.navFiles) }
        binding.navMore.setOnClickListener { select(R.id.navMore) }
        binding.moreButton.setOnClickListener { select(R.id.navMore) }
        select(R.id.navHome)
    }

    fun openTab(id: Int) {
        select(id)
    }

    fun openActivity(activity: Class<*>) {
        startActivity(Intent(this, activity))
    }

    private fun select(id: Int) {
        selected = id
        val fragment: Fragment = when (id) {
            R.id.navClean -> CleanFragment()
            R.id.navApps -> AppsFragment()
            R.id.navFiles -> FilesFragment()
            R.id.navMore -> MoreFragment()
            else -> HomeFragment()
        }
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.content, fragment)
            .commit()
        updateNav()
    }

    private fun updateNav() {
        val icons = listOf(binding.navHomeIcon, binding.navCleanIcon, binding.navAppsIcon, binding.navFilesIcon, binding.navMoreIcon)
        val labels = listOf(binding.navHomeLabel, binding.navCleanLabel, binding.navAppsLabel, binding.navFilesLabel, binding.navMoreLabel)
        val ids = listOf(R.id.navHome, R.id.navClean, R.id.navApps, R.id.navFiles, R.id.navMore)
        icons.forEachIndexed { index, icon ->
            val on = ids[index] == selected
            icon.setColorFilter(ContextCompat.getColor(this, if (on) R.color.brand else R.color.text_tertiary))
            labels[index].setTextColor(ContextCompat.getColor(this, if (on) R.color.brand else R.color.text_tertiary))
        }
    }
}

interface Refreshable {
    fun refresh()
}
