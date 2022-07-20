package com.nikolay.meteringdevice

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.nio.ByteBuffer

class SettingsActivity : AppCompatActivity() {
    lateinit var btn_close:Button
    lateinit var btn_set_param:Button
    lateinit var btn_check_param:Button
    lateinit var inp_parameter:EditText
    lateinit var sp_parameter:Spinner

    val allCommands = hashMapOf<String, Char>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        btn_close = findViewById(R.id.btn_close)
        btn_set_param = findViewById(R.id.btn_set_parameter)
        btn_check_param = findViewById(R.id.btn_check_param)
        inp_parameter = findViewById(R.id.inp_parameter)
        sp_parameter = findViewById(R.id.sp_parameter)

        MeteringConnect.var_context = this

        val parametersAdepter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            resources.getStringArray(R.array.device_params)
        )
        parametersAdepter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sp_parameter.adapter = parametersAdepter

        for ((index, value) in cmdParams.withIndex()) {
            allCommands.put(parametersAdepter.getItem(index).toString(), value)
        }


        btn_set_param.setOnClickListener {
            sendCommand()
        }

        btn_check_param.setOnClickListener {
            checkCommand()
        }

        btn_close.setOnClickListener {
            finish()
        }
    }

    fun sendCommand()
    {
        if(inp_parameter.text.isBlank())
        {
            Toast.makeText(
                this, "Please enter the valid number",
                Toast.LENGTH_SHORT).show()
            return
        }
        val char_command = allCommands[sp_parameter.selectedItem.toString()]
        val byte_command = char_command!!.code.toByte()
        val command: ByteArray?

        when(char_command)
        {
            'S' -> {
                val int_value = inp_parameter.text.toString().toInt()
                val state = if (int_value > 0) 'S' else 'A'
                command = byteArrayOf(state.code.toByte(), 0, 0, 0, byte_command)
            }
            else -> {
                val float_value = inp_parameter.text.toString().toFloat()
                command = toByteArray(float_value).reversedArray() + byte_command
            }
        }
        MeteringConnect.instance.writeCMD(command)
    }


    fun checkCommand()
    {
        val char_command = allCommands[sp_parameter.selectedItem.toString()]!!.code.toByte()
        val command = byteArrayOf(char_command, 0, 0, 0, 'U'.code.toByte())
        MeteringConnect.instance.writeCMD(command)
    }

    fun toByteArray(value: Float): ByteArray {
        val bytes = ByteArray(4)
        ByteBuffer.wrap(bytes).putFloat(value)
        return bytes
    }

    companion object {
        val cmdParams  = listOf<Char>('S', 'P', 'L', '1', '2', '3', '4')
    }

}