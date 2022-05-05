package com.nikolay.meteringdevice

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import java.util.*


class RPMEntry(context: Context): LinearLayout(context) {
    private val timer: Timer = Timer()
    private val DELAY: Long = 1000 // in ms


    private val btn_rpm_send: ImageButton
    val rpm_fild: EditText
    private val send_rpm: Button
    init {
        send_rpm = Button(context)
        send_rpm.setText("  SET RPM  ")
        send_rpm.textSize = 26F
        val param0 = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        param0.height = ViewGroup.LayoutParams.MATCH_PARENT
        send_rpm.setLayoutParams(param0)
        send_rpm.setOnClickListener {
            sendRPMValue()
        }
        this.addView(send_rpm)


        rpm_fild = EditText(context)
        rpm_fild.inputType = InputType.TYPE_CLASS_NUMBER
        rpm_fild.textSize = 36.0F
        rpm_fild.setText("0")
        rpm_fild.setImeOptions(EditorInfo.IME_ACTION_DONE)
        rpm_fild.textAlignment = TEXT_ALIGNMENT_CENTER
        val param = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            1.0f
        )
        rpm_fild.setLayoutParams(param)
        rpm_fild.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if(rpm_fild.text.toString() == "")
                {
                    rpm_fild.setText("0")
                }
                if(rpm_fild.text.toString().toInt() > 255)
                {
                    rpm_fild.setText("255")
                }
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as
                        InputMethodManager
                imm.hideSoftInputFromWindow(this.windowToken, 0)
                ControlPreferences.saveRPMsettings(context as MainActivity)
                return@setOnEditorActionListener true
            }
            false
        }
        this.addView(rpm_fild)

        btn_rpm_send = ImageButton(context)
        btn_rpm_send.setImageResource(com.google.android.material.R.drawable.material_ic_clear_black_24dp)
        val param2 = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        param2.height = ViewGroup.LayoutParams.MATCH_PARENT
        param2.width = 100
        btn_rpm_send.setLayoutParams(param2)
        btn_rpm_send.setOnClickListener({
            deleteItself()
        })
        this.addView(btn_rpm_send)
    }

    private fun deleteItself()
    {
        (this.getParent() as ViewGroup).removeView(this)
        ControlPreferences.saveRPMsettings(context as MainActivity)
    }

    private fun sendRPMValue()
    {
        val ma = context as MainActivity
        val valInt = rpm_fild.text.toString().toInt()
        val value = if (valInt <= 255) rpm_fild.text.toString().toUByte() else 255U
        if(!ma.deviceConnected)
        {
            Toast.makeText(context, "The device is not connected!",
                Toast.LENGTH_SHORT).show()
        }
        else
        {
            ma.meteringConnection?.writeRPM(value)
        }
    }

}