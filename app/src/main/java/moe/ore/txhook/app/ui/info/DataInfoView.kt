package moe.ore.txhook.app.ui.info

import android.content.Context
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity.CENTER
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import android.widget.RelativeLayout
import android.widget.RelativeLayout.CENTER_VERTICAL
import android.widget.RelativeLayout.END_OF
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import moe.ore.android.util.AndroidUtil.dip2px
import moe.ore.txhook.R

class DataInfoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : CardView(context, attrs, defStyle) {
    private val root: LinearLayout = LinearLayout(context).also {
        it.layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        it.orientation = VERTICAL
    }
    private val itemList: ArrayList<RelativeLayout> = arrayListOf()
    private lateinit var title: TextView
    private lateinit var noDataText: TextView

    init {
        addView(root)
    }

    fun tittle(content: String) {
        if (!this::title.isInitialized) {
            title = TextView(context)
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)

            title.layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)

            val tp: TextPaint = title.paint
            tp.isFakeBoldText = true // 设置粗体

            root.addView(title)

            noDataText = TextView(context)
            noDataText.layoutParams = RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also {
                it.addRule(CENTER_VERTICAL)
                val top = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    10f,
                    this.resources.displayMetrics
                ).toInt()
                it.setMargins(0, top, 0, top)
            }
            noDataText.gravity = CENTER
            noDataText.setTextAppearance(androidx.constraintlayout.widget.R.style.TextAppearance_AppCompat_Large)
            noDataText.setTextColor(ActivityCompat.getColor(context, R.color.tx_base_info))
            noDataText.setText(R.string.noData)
            noDataText.visibility = GONE

            root.addView(noDataText)
        }
        title.text = content
    }

    fun item(
        leftIcon: Int,
        name: String,
        content: String,
        clickListener: OnItemClickListener? = null
    ) =
        item(ItemInfo(leftIcon, name, content), clickListener = clickListener)

    fun item(
        name: String,
        content: String,
        canCopy: Boolean = true,
        clickListener: OnItemClickListener? = null
    ) =
        item(ItemInfo(0, name, content), canCopy = canCopy, clickListener = clickListener)

    private fun item(
        itemInfo: ItemInfo,
        canCopy: Boolean = true,
        clickListener: OnItemClickListener? = null
    ) {
        val item = RelativeLayout(context)

        val params = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        params.topMargin = dip2px(context, if (itemList.isEmpty()) 10f else 18f)
        item.layoutParams = params

        var icon: ImageView? = null
        if (itemInfo.leftIcon != 0) {
            val licon = ImageView(context)
            item.addView(licon, RelativeLayout.LayoutParams(
                dip2px(context, 22f), dip2px(context, 22f)
            ).also {
                it.addRule(CENTER_VERTICAL)
            })
            licon.setImageResource(itemInfo.leftIcon)
            licon.id = findUnusedId()
            icon = licon
        }

        val name = TextView(context)
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        item.addView(name, RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).also {
            it.addRule(CENTER_VERTICAL)
            if (icon != null)
                it.addRule(END_OF, icon.id)
        })
        name.setTextColor(ResourcesCompat.getColor(resources, R.color.accentFallback, null))
        name.text = itemInfo.name
        name.id = findUnusedId()

        val content = TextView(context)
        content.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        item.addView(content, RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also {
            it.marginStart = dip2px(context, 18f)
            it.addRule(CENTER_VERTICAL)
            it.addRule(END_OF, name.id)
        })
        content.setTextColor(ResourcesCompat.getColor(resources, R.color.accentFallback, null))
        content.text = itemInfo.content
        if (clickListener == null)
            content.setTextIsSelectable(canCopy)

        item.setOnClickListener {
            clickListener?.onClickItem()
        }
        icon?.setOnClickListener {
            clickListener?.onClickLeftIcon(it as ImageView)
        }

        itemList.add(item)
        root.addView(item)
    }

    fun hideNoData() {
        noDataText.visibility = GONE
    }

    fun showNoData() {
        noDataText.visibility = VISIBLE
    }

    fun clear() {
        itemList.forEach {
            root.removeView(it)
        }
        itemList.clear()
    }

    private var fID = com.google.android.material.R.id.NO_DEBUG

    private fun findUnusedId(): Int {
        while (findViewById<View?>(++fID) != null) {
        }
        return fID
    }

    data class ItemInfo(
        val leftIcon: Int,
        val name: String,
        val content: String,
    )
}

fun interface OnItemClickListener {
    fun onClickItem()

    fun onClickLeftIcon(image: ImageView) {

    }
}