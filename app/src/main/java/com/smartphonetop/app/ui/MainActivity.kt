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
import com.smartphonetop.app.ui.fragments.BatteryFragment
import com.smartphonetop.app.ui.fragments.CleanFragment
import com.smartphonetop.app.ui.fragments.FilesFragment
import com.smartphonetop.app.ui.fragments.HomeFragment
import com.smartphonetop.app.util.ThemeUtils

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selected = R.id.navHome
    private var moreSheet: MoreSheet? = null

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
        binding.navFiles.setOnClickListener { select(R.id.navFiles) }
        binding.navBattery.setOnClickListener { select(R.id.navBattery) }
        binding.navApps.setOnClickListener { select(R.id.navApps) }
        binding.navMore.setOnClickListener { showMoreSheet() }
        binding.moreButton.setOnClickListener { showMoreSheet() }
        select(R.id.navHome)
    }

    private fun showMoreSheet() {
        if (moreSheet?.isAdded == true) return
        selected = R.id.navMore
        updateNav()
        moreSheet = MoreSheet().also { it.show(supportFragmentManager, "more") }
    }

    fun openTab(id: Int) {
        select(id)
    }

    fun openCacheScan() {
        selected = R.id.navClean
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.content, CleanFragment.newInstance(autoScan = true))
            .commit()
        updateNav()
    }

    fun openActivity(activity: Class<*>) {
        startActivity(Intent(this, activity))
    }

    private fun select(id: Int) {
        selected = id
        val fragment: Fragment = when (id) {
            R.id.navClean -> CleanFragment()
            R.id.navFiles -> FilesFragment()
            R.id.navBattery -> BatteryFragment()
            R.id.navApps -> AppsFragment()
            else -> HomeFragment()
        }
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.content, fragment)
            .commit()
        updateNav()
    }

    private fun updateNav() {
        val icons = listOf(
            binding.navHomeIcon, binding.navCleanIcon, binding.navFilesIcon,
            binding.navBatteryIcon, binding.navAppsIcon, binding.navMoreIcon
        )
        val labels = listOf(
            binding.navHomeLabel, binding.navCleanLabel, binding.navFilesLabel,
            binding.navBatteryLabel, binding.navAppsLabel, binding.navMoreLabel
        )
        val ids = listOf(R.id.navHome, R.id.navClean, R.id.navFiles, R.id.navBattery, R.id.navApps, R.id.navMore)
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
