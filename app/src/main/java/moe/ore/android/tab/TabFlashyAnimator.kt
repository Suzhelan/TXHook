package moe.ore.android.tab

import android.annotation.SuppressLint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import androidx.viewpager.widget.ViewPager.*
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import moe.ore.txhook.R

class TabFlashyAnimator(private var tabLayout: TabLayout?, private val selectedColor: Int) :
    ViewPager2.OnPageChangeCallback() {
    private val mFragmentTitleList: MutableList<String> = ArrayList()
    private val mFragmentIconList: MutableList<Int> = ArrayList()
    private val mFragmentColorList: MutableList<Int?> = ArrayList()
    private val mFragmentSizeList: MutableList<Float?> = ArrayList()
    private var previousPosition = -1

    fun addTabItem(title: String, tabIcon: Int) {
        addTabItem(title, tabIcon, null, null)
    }

    fun addTabItem(title: String, tabIcon: Int, color: Int) {
        addTabItem(title, tabIcon, color, null)
    }

    fun addTabItem(title: String, tabIcon: Int, size: Float) {
        addTabItem(title, tabIcon, null, size)
    }

    fun addTabItem(title: String, tabIcon: Int, color: Int?, size: Float?) {
        mFragmentTitleList.add(title)
        mFragmentIconList.add(tabIcon)
        mFragmentColorList.add(color)
        mFragmentSizeList.add(size)
    }

    @SuppressLint("InflateParams")
    private fun getTabView(position: Int, tab: TabLayout.Tab?, isSelected: Boolean) {
        val view = if (tab!!.customView == null) LayoutInflater.from(
            tabLayout?.context
        ).inflate(R.layout.custom_tab, null) else tab.customView!!
        if (tab.customView == null) {
            tab.customView = view
        }

        tabLayout?.setSelectedTabIndicatorColor(selectedColor)

        val tabImageView = view.findViewById<ImageView>(R.id.tab_image)
        tabImageView.setImageResource(mFragmentIconList[position])
        val layout: ConstraintLayout = view.findViewById(R.id.root)
        val set = ConstraintSet()
        val foreground = view.findViewById<ImageView>(R.id.image_foreground)

        foreground.visibility = VISIBLE

        val dot = view.findViewById<ImageView>(R.id.dot)
        val title = view.findViewById<TextView>(R.id.tab_title)
        title.text = mFragmentTitleList[position]

        title.setTextColor(
            if (mFragmentColorList[position] == null)
                selectedColor
            else getColor(mFragmentColorList[position]!!)
        )

        if (mFragmentSizeList[position] != null) {
            title.setTextSize(TypedValue.COMPLEX_UNIT_PX, mFragmentSizeList[position]!!)
        }
        set.clone(layout)
        // set.connect(textForeground.id, ConstraintSet.TOP, if (isSelected) title.id else tabImageView.id, ConstraintSet.BOTTOM)
        if (isSelected) {
            set.clear(tabImageView.id, ConstraintSet.BOTTOM)
            set.connect(title.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            set.connect(
                title.id,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM
            )
            dot.startAnimation(AnimationUtils.loadAnimation(tabLayout?.context, R.anim.tab_show))
        } else {
            set.connect(
                tabImageView.id,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM
            )
            set.clear(title.id, ConstraintSet.BOTTOM)
            set.connect(title.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            if (position == previousPosition || previousPosition == -1) {
                dot.startAnimation(
                    AnimationUtils.loadAnimation(
                        tabLayout?.context,
                        R.anim.tab_hide
                    )
                )
            }
        }
        set.clear(foreground.id, if (isSelected) ConstraintSet.TOP else ConstraintSet.BOTTOM)
        set.connect(
            foreground.id,
            if (isSelected) ConstraintSet.BOTTOM else ConstraintSet.TOP,
            tabImageView.id,
            ConstraintSet.BOTTOM
        )

        set.applyTo(layout)

        if (isSelected)
            tabImageView.visibility = GONE
        else {
            tabImageView.visibility = VISIBLE
            // tabImageView.setColorFilter(Color.rgb(50,205,50))
        }

        foreground.visibility = INVISIBLE
    }

    fun highLightTab(position: Int) {
        if (tabLayout != null) {
            TransitionManager.beginDelayedTransition(tabLayout!!, transitionSet)
            for (i in 0 until tabLayout!!.tabCount) {
                val tab = tabLayout!!.getTabAt(i)!!
                getTabView(i, tab, i == position)
                val layout =
                    (tabLayout!!.getChildAt(0) as LinearLayout).getChildAt(i) as LinearLayout
                layout.background = null
                layout.setPaddingRelative(0, 0, 0, 0)
            }
            previousPosition = position
        }
    }

    private val transitionSet: TransitionSet
        get() {
            val set = TransitionSet()
            set.addTransition(ChangeBounds().setDuration(250))
            set.ordering = TransitionSet.ORDERING_TOGETHER
            return set
        }

    fun onStart(tabLayout: TabLayout?) {
        this.tabLayout = tabLayout
    }

    fun onStop() {
        tabLayout = null
    }

    override fun onPageSelected(position: Int) {
        highLightTab(position)
    }

    private fun getColor(@ColorRes colorRes: Int): Int {
        return ContextCompat.getColor(tabLayout!!.context, colorRes)
    }

    fun setBadge(count: Int, position: Int) {
        val tab = tabLayout!!.getTabAt(position)!!
        val badge = tab.customView!!.findViewById<TextView>(R.id.badge)
        badge.visibility = if (count == 0) View.GONE else View.VISIBLE
        badge.text = count.toString()
    }
}