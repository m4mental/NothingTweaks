package com.souleven.nothingos.hooks

import android.content.Context
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.SeekBar
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import androidx.preference.SeekBarPreference
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import com.souleven.nothingos.MainHook.Companion.TAG

class KillAppTimeoutSeekBarPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SeekBarPreference(context, attrs) {

    private var currentSeekBar: SeekBar? = null

    init {
        min = 200
        max = 3000
        seekBarIncrement = 100
        showSeekBarValue = true
        updatesContinuously = true
    }

    override fun setValue(seekBarValue: Int) {
        val snapped = Math.round(seekBarValue.toFloat() / 100f) * 100
        val clamped = snapped.coerceIn(min, max)
        super.setValue(clamped)
        currentSeekBar?.let { sb ->
            val targetProgress = clamped - min
            if (sb.progress != targetProgress) {
                sb.progress = targetProgress
            }
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val valueText = holder.findViewById(androidx.preference.R.id.seekbar_value) as? TextView
        if (valueText != null) {
            val current = valueText.text?.toString()?.replace(" ms", "")
            if (current != null) {
                valueText.text = "$current ms"
            }

            (valueText.getTag(androidx.preference.R.id.seekbar_value) as? TextWatcher)?.let {
                valueText.removeTextChangedListener(it)
            }

            var updating = false
            val watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (updating) return
                    val str = s?.toString() ?: return
                    if (!str.endsWith(" ms")) {
                        updating = true
                        valueText.text = "$str ms"
                        updating = false
                    }
                }
            }
            valueText.setTag(androidx.preference.R.id.seekbar_value, watcher)
            valueText.addTextChangedListener(watcher)
        }

        val seekBar = holder.findViewById(androidx.preference.R.id.seekbar) as? SeekBar
        currentSeekBar = seekBar
        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    val rawValue = min + progress
                    val snapped = Math.round(rawValue.toFloat() / 100f) * 100
                    val clamped = snapped.coerceIn(min, max)
                    val targetProgress = clamped - min

                    if (fromUser && sb.progress != targetProgress) {
                        sb.progress = targetProgress
                    }

                    if (value != clamped) {
                        if (callChangeListener(clamped)) {
                            value = clamped
                        }
                    }
                }

                override fun onStartTrackingTouch(sb: SeekBar) {}

                override fun onStopTrackingTouch(sb: SeekBar) {
                    val rawValue = min + sb.progress
                    val snapped = Math.round(rawValue.toFloat() / 100f) * 100
                    val clamped = snapped.coerceIn(min, max)
                    val targetProgress = clamped - min

                    if (sb.progress != targetProgress) {
                        sb.progress = targetProgress
                    }

                    if (value != clamped) {
                        if (callChangeListener(clamped)) {
                            value = clamped
                        }
                    }
                }
            })
        }
    }
}

class BackGestureKillHooks : HookModule {

    companion object {
        const val PREF_KEY_TIMEOUT = "pref_back_gesture_kill_timeout"
        const val DEFAULT_TIMEOUT_MS = 500
        const val MIN_TIMEOUT_MS = 200
        const val MAX_TIMEOUT_MS = 3000
        private val PROTECTED_PACKAGES = setOf(
            "com.android.systemui",
            "com.nothing.launcher",
            "android"
        )
    }

    private var isCallbackHooked = false

    override fun handleLoadPackage(lpparam: LoadPackageParam, prefs: Prefs) {
        if (lpparam.packageName != "com.android.systemui") return
        if (!prefs.getBoolean("pref_back_gesture_kill", false)) return

        try {
            val handlerClass = XposedHelpers.findClassIfExists(
                "com.android.systemui.navigationbar.gestural.EdgeBackGestureHandler",
                lpparam.classLoader
            )

            if (handlerClass == null) {
                XposedBridge.log("$TAG BackGestureKillHooks: EdgeBackGestureHandler not found")
                return
            }

            XposedBridge.hookAllConstructors(
                handlerClass,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            XposedHelpers.setIntField(param.thisObject, "mLongPressTimeout", Int.MAX_VALUE)
                        } catch (e: Exception) {
                            XposedBridge.log("$TAG BackGestureKillHooks: Failed to set mLongPressTimeout: ${e.message}")
                        }
                    }
                }
            )

            val gestureState = java.util.WeakHashMap<Any, GestureState>()

            XposedHelpers.findAndHookMethod(
                handlerClass,
                "onMotionEvent",
                MotionEvent::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val handler = param.thisObject
                        val event = param.args[0] as MotionEvent
                        val action = event.actionMasked

                        val state = gestureState.getOrPut(handler) { GestureState() }

                        if (!isCallbackHooked) {
                            val callback = try {
                                XposedHelpers.getObjectField(handler, "mBackCallback")
                            } catch (e: Exception) { null }
                            
                            if (callback != null) {
                                isCallbackHooked = true
                                XposedBridge.hookAllMethods(
                                    callback.javaClass,
                                    "triggerBack",
                                    object : XC_MethodHook() {
                                        override fun beforeHookedMethod(callbackParam: MethodHookParam) {
                                            val h = try {
                                                XposedHelpers.getObjectField(callbackParam.thisObject, "this\$0")
                                            } catch (e: Exception) { null }
                                            
                                            if (h != null) {
                                                val s = gestureState[h]
                                                if (s != null && s.longPressTriggered) {
                                                    val packageName = try {
                                                        XposedHelpers.getObjectField(h, "mPackageName") as? String
                                                    } catch (e: Exception) { null }

                                                    if (packageName != null && packageName !in PROTECTED_PACKAGES) {
                                                        try {
                                                            val context = XposedHelpers.getObjectField(h, "mContext") as Context
                                                            val intent = Intent("com.souleven.nothingos.FORCE_STOP_APP")
                                                            intent.setPackage("android")
                                                            intent.putExtra("package_name", packageName)
                                                            context.sendBroadcast(intent)
                                                        } catch (e: Exception) {
                                                            XposedBridge.log("$TAG BackGestureKill: Failed to send kill broadcast: ${e.message}")
                                                        }
                                                    }

                                                    callbackParam.result = null
                                                    s.longPressTriggered = false
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        when (action) {
                            MotionEvent.ACTION_DOWN -> {
                                state.longPressTriggered = false
                                prefs.forceReload()
                            }

                            MotionEvent.ACTION_MOVE -> {
                                val allowGesture = try {
                                    XposedHelpers.getBooleanField(handler, "mAllowGesture")
                                } catch (e: Exception) { false }

                                if (allowGesture) {
                                    val elapsed = event.eventTime - event.downTime
                                    val timeoutMs = prefs.getInt(PREF_KEY_TIMEOUT, DEFAULT_TIMEOUT_MS)
                                        .coerceIn(MIN_TIMEOUT_MS, MAX_TIMEOUT_MS)

                                    if (elapsed > timeoutMs && !state.longPressTriggered) {
                                        state.longPressTriggered = true
                                    }
                                }
                            }
                        }
                    }
                }
            )

        } catch (t: Throwable) {
            XposedBridge.log("$TAG BackGestureKillHooks: Failed to hook: ${t.message}")
            XposedBridge.log(t)
        }
    }

    private class GestureState {
        var longPressTriggered = false
    }
}
