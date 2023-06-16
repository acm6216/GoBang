package cen.xiaoyuan.gobang

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kotlin.collections.set

object SpManager {
    abstract class SpListener<T>(val callback: (T) -> Unit) {
        fun onUpdate(newValue: T) = callback(newValue)
    }

    class SpBoolListener(
        val defaultValue: Boolean = false,
        callback: (Boolean) -> Unit
    ) : SpListener<Boolean>(callback)

    class SpFloatListener(
        val defaultValue: Float = 0f,
        callback: (Float) -> Unit
    ) : SpListener<Float>(callback)

    class SpIntListener(
        val defaultValue: Int = 0,
        callback: (Int) -> Unit
    ) : SpListener<Int>(callback)

    class SpStringListener(
        val defaultValue: String = "",
        callback: (String) -> Unit
    ) : SpListener<String>(callback)

    private lateinit var sp: SharedPreferences

    private val listeners:LinkedHashMap<String,LinkedHashMap<String,SpListener<out Any>>> = linkedMapOf()

    fun init(context: Context) {
        sp = PreferenceManager.getDefaultSharedPreferences(context).also {
            it.registerOnSharedPreferenceChangeListener { sp, key ->
                update(sp, key)
            }
        }
    }

    private fun SharedPreferences.use(block:((SharedPreferences.Editor)-> SharedPreferences.Editor)){ block(edit()).apply() }

    fun String.putString(value:String) = sp.use{ it.putString(this,value) }
    fun String.getString(def:String):String = sp.getString(this,def).toString()

    fun String.putInt(value:Int) = sp.use{ it.putInt(this,value) }
    fun String.getInt(def:Int):Int = sp.getInt(this,def)

    fun String.putBoolean(value:Boolean) = sp.use {  it.putBoolean(this,value) }
    fun String.getBoolean(def:Boolean):Boolean = sp.getBoolean(this,def)

    fun String.getIntCompat(def:Int):Int{
        return try { sp.getInt(this,def) } catch (e:Exception){ def }
    }

    fun String.getStringCompat(def:String):String{
        return try { sp.getString(this,def).toString() } catch (e:Exception){ def }
    }

    private fun update(sp: SharedPreferences?, key: String) {
        listeners[key]?.values?.forEach {
            when (it) {
                is SpBoolListener -> it.onUpdate(sp?.getBoolean(key, it.defaultValue) ?: it.defaultValue)
                is SpFloatListener -> it.onUpdate(sp?.getFloat(key, it.defaultValue) ?: it.defaultValue)
                is SpIntListener -> it.onUpdate(sp?.getInt(key, it.defaultValue) ?: it.defaultValue)
                is SpStringListener -> it.onUpdate(sp?.getString(key, it.defaultValue) ?: it.defaultValue)
            }
        }
    }

    fun <K : Any, T : SpListener<K>> listen(key: String,tag:String, listener: T) {
        if(!listeners.containsKey(key)) listeners[key] = LinkedHashMap()
        listeners[key]?.put(tag,listener)
        update(sp, key)
    }

}