package com.simplemobiletools.calendar.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import com.simplemobiletools.calendar.MonthlyCalendarImpl
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.helpers.Config
import com.simplemobiletools.calendar.helpers.HIGH_ALPHA
import com.simplemobiletools.calendar.helpers.LOW_ALPHA
import com.simplemobiletools.calendar.helpers.MyWidgetMonthlyProvider
import com.simplemobiletools.calendar.interfaces.MonthlyCalendar
import com.simplemobiletools.calendar.models.Day
import com.simplemobiletools.commons.extensions.adjustAlpha
import kotlinx.android.synthetic.main.first_row.*
import kotlinx.android.synthetic.main.top_navigation.*
import kotlinx.android.synthetic.main.widget_config_monthly.*
import org.joda.time.DateTime
import yuku.ambilwarna.AmbilWarnaDialog

class WidgetMonthlyConfigureActivity : AppCompatActivity(), MonthlyCalendar {
    lateinit var mRes: Resources
    private var mDays: List<Day>? = null
    private var mPackageName = ""

    private var mBgAlpha = 0f
    private var mWidgetId = 0
    private var mBgColorWithoutTransparency = 0
    private var mBgColor = 0
    private var mTextColorWithoutTransparency = 0
    private var mTextColor = 0
    private var mWeakTextColor = 0
    private var mDayTextSize = 0f
    private var mTodayTextSize = 0f

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        setContentView(R.layout.widget_config_monthly)
        mPackageName = packageName
        initVariables()

        val extras = intent.extras
        if (extras != null)
            mWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        if (mWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
            finish()

        config_save.setOnClickListener { saveConfig() }
        config_bg_color.setOnClickListener { pickBackgroundColor() }
        config_text_color.setOnClickListener { pickTextColor() }
    }

    private fun initVariables() {
        mRes = resources
        mDayTextSize = mRes.getDimension(R.dimen.day_text_size)
        mDayTextSize /= mRes.displayMetrics.density

        mTodayTextSize = mRes.getDimension(R.dimen.today_text_size)
        mTodayTextSize /= mRes.displayMetrics.density

        val config = Config.newInstance(this)
        mTextColorWithoutTransparency = config.widgetTextColor
        updateTextColors()

        mBgColor = config.widgetBgColor
        if (mBgColor == 1) {
            mBgColor = Color.BLACK
            mBgAlpha = .2f
        } else {
            mBgAlpha = Color.alpha(mBgColor) / 255.toFloat()
        }

        mBgColorWithoutTransparency = Color.rgb(Color.red(mBgColor), Color.green(mBgColor), Color.blue(mBgColor))
        config_bg_seekbar.setOnSeekBarChangeListener(bgSeekbarChangeListener)
        config_bg_seekbar.progress = (mBgAlpha * 100).toInt()
        updateBgColor()

        MonthlyCalendarImpl(this, applicationContext).updateMonthlyCalendar(DateTime())
    }

    fun saveConfig() {
        storeWidgetColors()
        requestWidgetUpdate()

        Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId)
            setResult(Activity.RESULT_OK, this)
        }
        finish()
    }

    private fun storeWidgetColors() {
        Config.newInstance(this).apply {
            widgetBgColor = mBgColor
            widgetTextColor = mTextColorWithoutTransparency
        }
    }

    fun pickBackgroundColor() {
        val dialog = AmbilWarnaDialog(this, mBgColorWithoutTransparency, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog) {
            }

            override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
                mBgColorWithoutTransparency = color
                updateBgColor()
            }
        })

        dialog.show()
    }

    fun pickTextColor() {
        val dialog = AmbilWarnaDialog(this, mTextColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog) {
            }

            override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
                mTextColorWithoutTransparency = color
                updateTextColors()
                updateDays()
            }
        })

        dialog.show()
    }

    private fun requestWidgetUpdate() {
        Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, MyWidgetMonthlyProvider::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(mWidgetId))
            sendBroadcast(this)
        }
    }

    private fun updateTextColors() {
        mTextColor = mTextColorWithoutTransparency.adjustAlpha(HIGH_ALPHA)
        mWeakTextColor = mTextColorWithoutTransparency.adjustAlpha(LOW_ALPHA)

        top_left_arrow.drawable.mutate().setColorFilter(mTextColor, PorterDuff.Mode.SRC_ATOP)
        top_right_arrow.drawable.mutate().setColorFilter(mTextColor, PorterDuff.Mode.SRC_ATOP)
        top_value.setTextColor(mTextColor)
        config_text_color.setBackgroundColor(mTextColor)
        config_save.setTextColor(mTextColor)
        updateLabels()
    }

    private fun updateBgColor() {
        mBgColor = mBgColorWithoutTransparency.adjustAlpha(mBgAlpha)
        config_calendar.setBackgroundColor(mBgColor)
        config_bg_color.setBackgroundColor(mBgColor)
        config_save.setBackgroundColor(mBgColor)
    }

    private fun updateDays() {
        val len = mDays!!.size

        if (Config.newInstance(applicationContext).displayWeekNumbers) {
            week_num.setTextColor(mWeakTextColor)
            week_num.visibility = View.VISIBLE

            for (i in 0..5) {
                val weekIdTV = findViewById(mRes.getIdentifier("week_num_" + i, "id", mPackageName)) as TextView?
                weekIdTV!!.text = mDays!![i * 7].weekOfYear.toString() + ":"
                weekIdTV.setTextColor(mWeakTextColor)
                weekIdTV.visibility = View.VISIBLE
            }
        }

        for (i in 0..len - 1) {
            val day = mDays!![i]
            val dayTV = findViewById(mRes.getIdentifier("day_" + i, "id", mPackageName)) as TextView?
            var curTextColor = mWeakTextColor
            var curTextSize = mDayTextSize

            if (day.isThisMonth) {
                curTextColor = mTextColor
            }

            if (day.isToday) {
                curTextSize = mTodayTextSize
            }

            dayTV!!.text = day.value.toString()
            dayTV.setTextColor(curTextColor)
            dayTV.textSize = curTextSize
        }
    }

    private val bgSeekbarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            mBgAlpha = progress.toFloat() / 100.toFloat()
            updateBgColor()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {

        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {

        }
    }

    override fun updateMonthlyCalendar(month: String, days: List<Day>) {
        runOnUiThread {
            mDays = days
            updateMonth(month)
            updateDays()
        }
    }

    private fun updateMonth(month: String) {
        top_value.text = month
    }

    private fun updateLabels() {
        for (i in 0..6) {
            val dayTV = findViewById(mRes.getIdentifier("label_" + i, "id", mPackageName)) as TextView?
            dayTV!!.textSize = mDayTextSize
            dayTV.setTextColor(mTextColor)
        }
    }
}
