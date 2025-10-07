package com.example.androidgamekt

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.androidgamekt.util.ViewPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = adapter.itemCount

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Игра"
                1 -> "Регистрация"
                2 -> "Правила"
                3 -> "Авторы"
                4 -> "Настройки"
                else -> null
            }
        }.attach()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                updateCurrentItem(viewPager, tab)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {
                updateCurrentItem(viewPager, tab)
            }
        })
    }

    private fun updateCurrentItem(viewPager: ViewPager2, tab: TabLayout.Tab) {
        val current = viewPager.currentItem
        val target = tab.position
        val smoothScroll = abs(target - current) == 1
        viewPager.setCurrentItem(target, smoothScroll)
    }
}