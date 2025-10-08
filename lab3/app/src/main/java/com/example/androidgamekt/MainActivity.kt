package com.example.androidgamekt

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.androidgamekt.util.ViewPagerAdapter
import com.google.android.material.tabs.TabLayout
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

        val titles = listOf("Игра", "Регистрация", "Правила", "Авторы", "Настройки")
        titles.forEach { title ->
            tabLayout.addTab(tabLayout.newTab().setText(title))
        }

        var selectingFromPager = false

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (!selectingFromPager) {
                    selectingFromPager = true
                    tabLayout.getTabAt(position)?.select()
                    selectingFromPager = false
                }
            }
        })

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (selectingFromPager) return
                updateCurrentItem(viewPager, tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {
                if (selectingFromPager) return
                updateCurrentItem(viewPager, tab.position)
            }
        })
    }

    private fun updateCurrentItem(viewPager: ViewPager2, target: Int) {
        val current = viewPager.currentItem
        val smoothScroll = abs(target - current) <= 1
        viewPager.setCurrentItem(target, smoothScroll)
    }
}
