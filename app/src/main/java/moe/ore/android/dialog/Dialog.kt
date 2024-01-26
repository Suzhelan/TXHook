package moe.ore.android.dialog

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import moe.ore.txhook.R
import moe.ore.txhook.databinding.DialogInputBinding
import moe.ore.txhook.databinding.DialogListBinding
import moe.ore.txhook.databinding.DialogTipsBinding

object Dialog {
    class EditTextAlertBuilder @JvmOverloads constructor(
        ctx: Context,
        @StyleRes themeId: Int = R.style.AppTheme_Dialog
    ) :
        AlertDialog.Builder(ctx, themeId) {

        private var mLayout: DialogInputBinding =
            DialogInputBinding.inflate(context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)

        private var negativeListener: DialogInterface.OnClickListener? = null

        private var dialog: AlertDialog? = null

        private var positiveListener: EditTextAlertListener? = null

        init {
            super.setView(mLayout.root)
            mLayout.negative.setOnClickListener {
                negativeListener?.onClick(dialog, DialogInterface.BUTTON_NEGATIVE)
                dialog?.dismiss()
            }
        }

        override fun setTitle(title: CharSequence?): EditTextAlertBuilder {
            mLayout.title.text = title
            mLayout.title.visibility = View.VISIBLE
            return this
        }

        override fun setTitle(titleId: Int): AlertDialog.Builder {
            mLayout.title.setText(titleId)
            mLayout.title.visibility = View.VISIBLE
            return this
        }

        override fun setView(layoutResId: Int): AlertDialog.Builder {
            return this
        }

        override fun setView(view: View?): AlertDialog.Builder {
            return this
        }

        fun setHint(text: CharSequence?): AlertDialog.Builder {
            mLayout.editText.hint = text
            return this
        }

        fun setFloatingText(text: CharSequence?): EditTextAlertBuilder {
            mLayout.editText.floatingLabelText = text
            return this
        }

        fun setTextListener(
            listener: EditTextAlertListener
        ): EditTextAlertBuilder {
            positiveListener = listener
            return this
        }

        override fun setPositiveButton(
            text: CharSequence?,
            listener: DialogInterface.OnClickListener?
        ): AlertDialog.Builder {
            mLayout.buttonPanel.visibility = View.VISIBLE
            mLayout.positive.visibility = View.VISIBLE
            mLayout.positive.text = text
            mLayout.positive.setOnClickListener {
                positiveListener?.onSubmit(mLayout.editText.text)
                dialog?.dismiss()
            }
            return this
        }

        override fun setPositiveButton(
            @StringRes textId: Int,
            listener: DialogInterface.OnClickListener
        ): AlertDialog.Builder {
            return setPositiveButton(context.getString(textId), listener)
        }

        override fun setNegativeButton(
            text: CharSequence?,
            listener: DialogInterface.OnClickListener
        ): AlertDialog.Builder {
            mLayout.buttonPanel.visibility = View.VISIBLE
            mLayout.negative.visibility = View.VISIBLE
            mLayout.negative.text = text
            negativeListener = listener
            return this
        }

        override fun setNegativeButton(
            @StringRes textId: Int,
            listener: DialogInterface.OnClickListener
        ): AlertDialog.Builder {
            return setNegativeButton(context.getString(textId), listener)
        }

        override fun create(): AlertDialog {
            dialog = super.create()
            return dialog as AlertDialog
        }

        override fun show(): AlertDialog {
            this.create()
            dialog?.show()
            return dialog!!
        }

        fun interface EditTextAlertListener {
            fun onSubmit(text: CharSequence?)
        }
    }

    class ListAlertBuilder @JvmOverloads constructor(
        ctx: Context,
        @StyleRes themeId: Int = R.style.AppTheme_Dialog
    ) :
        AlertDialog.Builder(ctx, themeId) {

        private var mLayout: DialogListBinding =
            DialogListBinding.inflate(context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)

        private var negativeListener: DialogInterface.OnClickListener? = null

        private var dialog: AlertDialog? = null

        private val items: LinkedHashMap<String, ((AlertDialog, View, Int) -> Unit)?> =
            linkedMapOf()

        init {
            super.setView(mLayout.root)
            mLayout.negative.setOnClickListener {
                negativeListener?.onClick(dialog, DialogInterface.BUTTON_NEGATIVE)
                dialog?.dismiss()
            }
        }

        override fun setTitle(title: CharSequence?): ListAlertBuilder {
            mLayout.title.text = title
            mLayout.title.visibility = View.VISIBLE
            return this
        }

        override fun setTitle(titleId: Int): AlertDialog.Builder {
            mLayout.title.setText(titleId)
            mLayout.title.visibility = View.VISIBLE
            return this
        }

        override fun setView(layoutResId: Int): AlertDialog.Builder {
            return this
        }

        override fun setView(view: View?): AlertDialog.Builder {
            return this
        }

        fun addItem(
            name: String,
            block: ((dialog: AlertDialog, v: View, pos: Int) -> Unit)?
        ): ListAlertBuilder {
            items[name] = block
            return this
        }

        override fun setNegativeButton(
            text: CharSequence?,
            listener: DialogInterface.OnClickListener
        ): AlertDialog.Builder {
            mLayout.buttonPanel.visibility = View.VISIBLE
            mLayout.negative.visibility = View.VISIBLE
            mLayout.negative.text = text
            negativeListener = listener
            return this
        }

        override fun setNegativeButton(
            @StringRes textId: Int,
            listener: DialogInterface.OnClickListener
        ): AlertDialog.Builder {
            return setNegativeButton(context.getString(textId), listener)
        }

        override fun create(): AlertDialog {
            dialog = super.create()

            val ks = arrayListOf<String>()
            val vs = arrayListOf<((AlertDialog, View, Int) -> Unit)?>()

            items.forEach { (t, u) ->
                ks.add(t)
                vs.add(u)
            }

            val list = mLayout.listView
            list.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, ks)
            list.setOnItemClickListener { _, view, position, _ ->
                val f = vs[position]
                if (f != null) f.invoke(dialog!!, view, position) else dialog?.dismiss()
            }

            return dialog as AlertDialog
        }

        override fun show(): AlertDialog {
            this.create()
            dialog?.show()
            return dialog!!
        }
    }

    class CommonAlertBuilder @JvmOverloads constructor(
        ctx: Context,
        @StyleRes themeId: Int = R.style.AppTheme_Dialog
    ) :
        AlertDialog.Builder(ctx, themeId) {

        private var mLayout: DialogTipsBinding =
            DialogTipsBinding.inflate(context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)

        private var positiveListener: DialogInterface.OnClickListener? = null
        private var negativeListener: DialogInterface.OnClickListener? = null
        private var neutralListener: DialogInterface.OnClickListener? = null

        private var dialog: AlertDialog? = null

        init {
            super.setView(mLayout.root)
            mLayout.title.visibility = View.GONE
            mLayout.negative.visibility = View.GONE
            mLayout.positive.visibility = View.GONE
            mLayout.neutral.visibility = View.GONE
            mLayout.buttonPanel.visibility = View.GONE
            mLayout.messagePanel.visibility = View.GONE
        }

        override fun setTitle(title: CharSequence?): AlertDialog.Builder {
            mLayout.title.text = title
            mLayout.title.visibility = View.VISIBLE
            return this
        }

        override fun setTitle(titleId: Int): AlertDialog.Builder {
            mLayout.title.setText(titleId)
            mLayout.title.visibility = View.VISIBLE
            return this
        }

        override fun setView(layoutResId: Int): AlertDialog.Builder {
            return this
        }

        override fun setView(view: View?): AlertDialog.Builder {
            return this
        }

        override fun setMessage(@Nullable message: CharSequence?): AlertDialog.Builder {
            mLayout.message.text = message
            mLayout.messagePanel.visibility = View.VISIBLE
            return this
        }

        override fun setMessage(@StringRes messageId: Int): AlertDialog.Builder {
            return setMessage(context.getString(messageId))
        }

        override fun setPositiveButton(
            text: CharSequence?,
            listener: DialogInterface.OnClickListener
        ): AlertDialog.Builder {
            mLayout.buttonPanel.visibility = View.VISIBLE
            mLayout.positive.visibility = View.VISIBLE
            mLayout.positive.text = text
            positiveListener = listener
            mLayout.positive.setOnClickListener {
                positiveListener?.onClick(dialog, DialogInterface.BUTTON_POSITIVE)
                dialog?.dismiss()
            }
            return this
        }

        override fun setPositiveButton(
            @StringRes textId: Int,
            listener: DialogInterface.OnClickListener
        ): AlertDialog.Builder {
            return setPositiveButton(context.getString(textId), listener)
        }

        override fun setNegativeButton(
            text: CharSequence?,
            listener: DialogInterface.OnClickListener
        ): AlertDialog.Builder {
            mLayout.buttonPanel.visibility = View.VISIBLE
            mLayout.negative.visibility = View.VISIBLE
            mLayout.negative.text = text
            negativeListener = listener
            mLayout.negative.setOnClickListener {
                negativeListener?.onClick(dialog, DialogInterface.BUTTON_NEGATIVE)
                dialog?.dismiss()
            }
            return this
        }

        override fun setNegativeButton(
            @StringRes textId: Int,
            listener: DialogInterface.OnClickListener
        ): AlertDialog.Builder {
            return setNegativeButton(context.getString(textId), listener)
        }

        override fun setNeutralButton(
            text: CharSequence?,
            listener: DialogInterface.OnClickListener
        ): AlertDialog.Builder {
            mLayout.buttonPanel.visibility = View.VISIBLE
            mLayout.neutral.visibility = View.VISIBLE
            mLayout.neutral.text = text
            neutralListener = listener
            mLayout.neutral.setOnClickListener {
                neutralListener?.onClick(dialog, DialogInterface.BUTTON_NEUTRAL)
                dialog?.dismiss()
            }
            return this
        }

        override fun setNeutralButton(
            @StringRes textId: Int,
            listener: DialogInterface.OnClickListener
        ): AlertDialog.Builder {
            return setNeutralButton(context.getString(textId), listener)
        }

        override fun create(): AlertDialog {
            dialog = super.create()
            return dialog as AlertDialog
        }

        override fun show(): AlertDialog {
            this.create()
            dialog?.show()
            return dialog!!
        }
    }
}