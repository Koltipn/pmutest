package com.example.androidgamekt.util

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.androidgamekt.model.AuthorsFragment
import com.example.androidgamekt.model.GameFragment
import com.example.androidgamekt.model.RulesFragment
import com.example.androidgamekt.model.SettingsFragment
import com.example.androidgamekt.model.SignUpFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> GameFragment()
            1 -> SignUpFragment()
            2 -> RulesFragment()
            3 -> AuthorsFragment()
            4 -> SettingsFragment()
            else -> GameFragment()
        }
    }
}