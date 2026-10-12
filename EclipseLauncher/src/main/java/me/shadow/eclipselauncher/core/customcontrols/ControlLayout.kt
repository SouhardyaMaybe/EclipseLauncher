package me.shadow.eclipselauncher.core.customcontrols

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.Toast

import me.shadow.eclipselauncher.json.JsonSyntaxException
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.setting.AllSettings
import me.shadow.eclipselauncher.task.Task
import me.shadow.eclipselauncher.task.TaskExecutors
import me.shadow.eclipselauncher.ui.dialog.EditControlInfoDialog
import me.shadow.eclipselauncher.ui.dialog.SelectControlsDialog
import me.shadow.eclipselauncher.ui.dialog.TipDialog
import me.shadow.eclipselauncher.ui.subassembly.customcontrols.ControlInfoData
import me.shadow.eclipselauncher.utils.path.PathManager
import me.shadow.eclipselauncher.utils.stringutils.StringUtilsKt

import me.shadow.eclipselauncher.core.MinecraftGLSurface
import me.shadow.eclipselauncher.core.Tools
import me.shadow.eclipselauncher.core.customcontrols.buttons.ControlButton
import me.shadow.eclipselauncher.core.customcontrols.buttons.ControlDrawer
import me.shadow.eclipselauncher.core.customcontrols.buttons.ControlInterface
import me.shadow.eclipselauncher.core.customcontrols.buttons.ControlJoystick
import me.shadow.eclipselauncher.core.customcontrols.buttons.ControlSubButton
import me.shadow.eclipselauncher.core.customcontrols.handleview.ActionRow
import me.shadow.eclipselauncher.core.customcontrols.handleview.ControlHandleView
import me.shadow.eclipselauncher.core.customcontrols.handleview.EditControlPopup

import org.lwjgl.glfw.CallbackBridge

import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap

open class ControlLayout : FrameLayout {

    @JvmField
    protected var mLayout: CustomControls? = null
    private var mInfoData: ControlInfoData? = null
    /* Accessible when inside the game by ControlInterface implementations, cached for perf. */
    private var mGameSurface: MinecraftGLSurface? = null

    /* Cache to buttons for performance purposes */
    private var mButtons: MutableList<ControlInterface>? = null
    private var mModifiable = false
    private var mIsModified: Boolean = false
    private var mControlVisible = false

    private var mControlPopup: EditControlPopup? = null
    private var mHandleView: ControlHandleView? = null
    private var mMenuListener: ControlButtonMenuListener? = null
    @JvmField
    var mActionRow: ActionRow? = null
    @JvmField
    var mLayoutFileName: String? = null

    constructor(ctx: Context) : super(ctx)

    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)

    @Throws(IOException::class, JsonSyntaxException::class)
    fun loadLayout(jsonPath: String?) {
        val jsonFile = if (jsonPath != null) File(jsonPath) else File(AllSettings.defaultCtrl.getValue())

        val layout: CustomControls? = if (jsonFile.exists()) {
            LayoutConverter.loadAndConvertIfNecessary(context, jsonFile.absolutePath)
        } else {
            LayoutConverter.loadFromAssets(context, "default.json")
        }
        if (layout != null) {
            loadLayout(layout)
            mLayoutFileName = if (jsonFile.exists()) {
                StringUtilsKt.removeSuffix(jsonFile.name, ".json")
            } else {
                "default"
            }
        }
    }

    fun loadLayout(controlLayout: CustomControls?) {
        var sanitizedModified = false
        if (controlLayout != null) {
            sanitizedModified = LayoutSanitizer.sanitizeLayout(controlLayout)
        }
        mInfoData = controlLayout?.mControlInfoDataList
        if (mInfoData == null) {
            mInfoData = ControlInfoData()
        }

        if (mActionRow == null) {
            mActionRow = ActionRow(context)
            addView(mActionRow)
        }

        removeAllButtons()
        // The previous layout becomes unreachable right here; its lists are freed with it
        mLayout = null

        System.gc()
        mapTable.clear()

        // Cleanup buttons only when input layout is null
        if (controlLayout == null) return

        mLayout = controlLayout

        // Joystick(s) first, to workaround the touch dispatch
        for (joystick in mLayout!!.mJoystickDataList) {
            addJoystickView(joystick)
        }

        //CONTROL BUTTON
        for (button in controlLayout.mControlDataList) {
            addControlView(button)
        }

        //CONTROL DRAWER
        for (drawerData in controlLayout.mDrawerDataList) {
            val drawer = addDrawerView(drawerData)
            if (mModifiable) drawer.areButtonsVisible = true
        }

        mLayout!!.scaledAt = AllSettings.buttonScale.getValue().toFloat()

        setModified(sanitizedModified)
        mButtons = null
        getButtonChildren() // Force refresh
    } // loadLayout

    //CONTROL BUTTON
    fun addControlButton(controlButton: ControlData) {
        mLayout!!.mControlDataList.add(controlButton)
        addControlView(controlButton)
    }

    private fun addControlView(controlButton: ControlData) {
        val view = ControlButton(this, controlButton)

        if (!mModifiable) {
            view.alpha = view.getProperties().opacity
            view.isFocusable = false
            view.isFocusableInTouchMode = false
        }
        addView(view)

        setModified(true)
    }

    // CONTROL DRAWER
    fun addDrawer(drawerData: ControlDrawerData) {
        mLayout!!.mDrawerDataList.add(drawerData)
        addDrawerView()
    }

    private fun addDrawerView() {
        addDrawerView(null)
    }

    private fun addDrawerView(drawerData: ControlDrawerData?): ControlDrawer {
        val view = ControlDrawer(
            this,
            drawerData ?: mLayout!!.mDrawerDataList[mLayout!!.mDrawerDataList.size - 1]
        )

        if (!mModifiable) {
            view.alpha = view.getProperties().opacity
            view.isFocusable = false
            view.isFocusableInTouchMode = false
        }
        addView(view)
        //CONTROL SUB BUTTON
        for (subButton in view.getDrawerData().buttonProperties) {
            addSubView(view, subButton)
        }

        setModified(true)
        return view
    }

    //CONTROL SUB-BUTTON
    fun addSubButton(drawer: ControlDrawer, controlButton: ControlData) {
        //Yep there isn't much here
        drawer.getDrawerData().buttonProperties.add(controlButton)
        addSubView(drawer, drawer.getDrawerData().buttonProperties[drawer.getDrawerData().buttonProperties.size - 1])
    }

    private fun addSubView(drawer: ControlDrawer, controlButton: ControlData) {
        val view = ControlSubButton(this, controlButton, drawer)

        if (!mModifiable) {
            view.alpha = view.getProperties().opacity
            view.isFocusable = false
            view.isFocusableInTouchMode = false
        } else {
            view.setVisible(true)
        }

        addView(view)
        drawer.addButton(view)

        setModified(true)
    }

    // JOYSTICK BUTTON
    fun addJoystickButton(data: ControlJoystickData) {
        mLayout!!.mJoystickDataList.add(data)
        addJoystickView(data)
    }

    private fun addJoystickView(data: ControlJoystickData) {
        val view = ControlJoystick(this, data)

        if (!mModifiable) {
            view.alpha = view.getProperties().opacity
            view.isFocusable = false
            view.isFocusableInTouchMode = false
        }
        addView(view)
    }

    private fun removeAllButtons() {
        for (button in getButtonChildren()) {
            removeView(button.getControlView())
        }

        System.gc()
        //i wanna be sure that all the removed Views will be removed after a reload
        //because if frames will slowly go down after many control changes it will be warm and bad
    }

    @Throws(Exception::class)
    fun saveLayout(path: String?) {
        mLayout!!.save(path)
        setModified(false)
    }

    fun toggleControlVisible() {
        mControlVisible = !mControlVisible
        setControlVisible(mControlVisible)
    }

    fun getLayoutScale(): Float {
        return mLayout!!.scaledAt
    }

    fun getLayout(): CustomControls? {
        return mLayout
    }

    fun setControlVisible(isVisible: Boolean) {
        if (mModifiable) return // Not using on custom controls activity

        mControlVisible = isVisible
        for (button in getButtonChildren()) {
            button.setVisible(((button.getProperties().displayInGame && CallbackBridge.isGrabbing()) || (button.getProperties().displayInMenu && !CallbackBridge.isGrabbing())) && isVisible)
        }
    }

    fun setModifiable(isModifiable: Boolean) {
        if (!isModifiable && mModifiable) {
            removeEditWindow()
        }
        mModifiable = isModifiable
        if (isModifiable) {
            // In edit mode, all controls have to be shown
            for (button in getButtonChildren()) {
                button.setVisible(true)
            }
        }
    }

    fun getModifiable(): Boolean {
        return mModifiable
    }

    fun setModified(isModified: Boolean) {
        mIsModified = isModified
    }

    fun getButtonChildren(): MutableList<ControlInterface> {
        if (mModifiable || mButtons == null) {
            mButtons = ArrayList()
            for (i in 0 until childCount) {
                val v = getChildAt(i)
                if (v is ControlInterface)
                    mButtons!!.add(v)
            }
        }

        return mButtons!!
    }

    fun refreshControlButtonPositions() {
        for (button in getButtonChildren()) {
            button.setDynamicX(button.getProperties().dynamicX)
            button.setDynamicY(button.getProperties().dynamicY)
        }
    }

    override fun onViewRemoved(child: View?) {
        super.onViewRemoved(child)
        val popup = mControlPopup
        if (child is ControlInterface && popup != null) {
            popup.disappearColor()
            popup.disappear()
        }
    }

    /**
     * Load the layout if needed, and pass down the burden of filling values
     * to the button at hand.
     */
    fun editControlButton(button: ControlInterface) {
        val popup = mControlPopup
        if (popup == null) {
            // When the panel is null, it needs to inflate first.
            // So inflate it, then process it on the next frame
            mControlPopup = EditControlPopup(context, this)
            post { editControlButton(button) }
            return
        }

        popup.internalChanges = true
        popup.setCurrentlyEditedButton(button)

        popup.appear(button.getControlView().x + button.getControlView().width / 2f < Tools.currentDisplayMetrics.widthPixels / 2f)
        button.loadEditValues(popup)

        popup.internalChanges = false
        popup.disappearColor()

        if (mHandleView == null) {
            mHandleView = ControlHandleView(context)
            addView(mHandleView!!)
        }
        mHandleView!!.setControlButton(button)

        //mHandleView.show();
    }

    /** Swap the panel if the button position requires it */
    fun adaptPanelPosition() {
        mControlPopup?.adaptPanelPosition()
    }

    private val mapTable = HashMap<View, ControlInterface?>()

    //While this is called onTouch, this should only be called from a ControlButton.
    fun onTouch(v: View, ev: MotionEvent) {
        val lastControlButton = mapTable[v]

        // Map location to screen coordinates
        ev.offsetLocation(v.x, v.y)

        //Check if the action is cancelling, reset the lastControl button associated to the view
        if (ev.actionMasked == MotionEvent.ACTION_UP
            || ev.actionMasked == MotionEvent.ACTION_CANCEL
            || ev.actionMasked == MotionEvent.ACTION_POINTER_UP
        ) {
            lastControlButton?.sendKeyPresses(false)
            mapTable[v] = null
            return
        }

        if (ev.actionMasked != MotionEvent.ACTION_MOVE) return

        //Optimization pass to avoid looking at all children again
        if (lastControlButton != null) {
            println("last control button check" + ev.x + "-" + ev.y + "-" + lastControlButton.getControlView().x + "-" + lastControlButton.getControlView().y)
            if (ev.x > lastControlButton.getControlView().x
                && ev.x < lastControlButton.getControlView().x + lastControlButton.getControlView().width
                && ev.y > lastControlButton.getControlView().y
                && ev.y < lastControlButton.getControlView().y + lastControlButton.getControlView().height
            ) {
                return
            }
        }

        //Release last keys
        if (lastControlButton != null) lastControlButton.sendKeyPresses(false)
        mapTable.remove(v)

        // Update the state of all swipeable buttons
        for (button in getButtonChildren()) {
            if (!button.getProperties().isSwipeable) continue

            if (ev.x > button.getControlView().x
                && ev.x < button.getControlView().x + button.getControlView().width
                && ev.y > button.getControlView().y
                && ev.y < button.getControlView().y + button.getControlView().height
            ) {

                //Press the new key
                if (button != lastControlButton) {
                    button.sendKeyPresses(true)
                    mapTable[v] = button
                    return
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mModifiable && event.actionMasked != MotionEvent.ACTION_UP || mControlPopup == null)
            return true

        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // When the input window cannot be hidden, it returns false
        if (!imm.hideSoftInputFromWindow(windowToken, 0)) {
            if (mControlPopup!!.disappearLayer()) {
                mActionRow!!.setFollowedButton(null)
                mHandleView!!.hide()
            }
        }
        return true
    }

    fun removeEditWindow() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // When the input window cannot be hidden, it returns false
        imm.hideSoftInputFromWindow(windowToken, 0)
        val popup = mControlPopup
        if (popup != null) {
            popup.disappearColor()
            popup.disappear()
        }

        mActionRow?.setFollowedButton(null)
        mHandleView?.hide()
    }

    fun save(path: String?) {
        try {
            mLayout!!.save(path)
        } catch (e: IOException) {
            Logging.e("ControlLayout", "Failed to save the layout at:" + path)
        }
    }

    fun hasMenuButton(): Boolean {
        for (controlInterface in getButtonChildren()) {
            for (keycode in controlInterface.getProperties().keycodes) {
                if (keycode == ControlData.SPECIALBTN_MENU) return true
            }
        }
        return false
    }

    fun setMenuListener(menuListener: ControlButtonMenuListener?) {
        this.mMenuListener = menuListener
    }

    fun notifyAppMenu() {
        mMenuListener?.onClickedMenu()
    }

    /** Cached getter for perf purposes */
    fun getGameSurface(): MinecraftGLSurface? {
        if (mGameSurface == null) {
            mGameSurface = findViewById(R.id.main_game_render_view)
        }
        return mGameSurface
    }

    fun askToExit(editorExitable: EditorExitable) {
        if (mIsModified) {
            openSaveAndExitDialog(editorExitable)
        } else {
            openExitDialog(editorExitable)
        }
    }

    @Throws(Exception::class)
    fun saveToDirectory(name: String?): String {
        val jsonPath = PathManager.DIR_CTRLMAP_PATH + "/" + name + ".json"
        saveLayout(jsonPath)
        return jsonPath
    }

    private fun saveDialog(title: String?, confirmTask: Task<*>?) {
        val infoDialog = EditControlInfoDialog(context, true, mLayoutFileName, mInfoData!!)

        if (title != null && !title.isEmpty()) infoDialog.setTitle(title)

        infoDialog.setOnConfirmClickListener { fileName, _ ->
            try {
                val jsonPath = saveToDirectory(fileName)
                Toast.makeText(context, context.getString(R.string.generic_save) + ": " + jsonPath, Toast.LENGTH_SHORT).show()
                confirmTask?.execute()
            } catch (th: Throwable) {
                Tools.showError(context, th, true)
            }

            infoDialog.dismiss()
        }
        infoDialog.show()
    }

    fun openSaveDialog() {
        saveDialog(context.getString(R.string.generic_save), null)
    }

    fun openSaveAndExitDialog(editorExitable: EditorExitable) {
        saveDialog(
            context.getString(R.string.global_save_and_exit),
            Task.runTask(TaskExecutors.getAndroidUI()) {
                editorExitable.exitEditor()
                null
            })
    }

    fun openLoadDialog() {
        val dialog = SelectControlsDialog(context, object : SelectControlsDialog.SelectedListener {
            override fun onSelected(file: File) {
                try {
                    loadLayout(file.absolutePath)
                } catch (e: IOException) {
                    Tools.showError(context, e)
                }
            }
        })
        dialog.show()
    }

    fun openSetDefaultDialog() {
        val dialog = SelectControlsDialog(context, object : SelectControlsDialog.SelectedListener {
            override fun onSelected(file: File) {
                val absolutePath = file.absolutePath
                try {
                    AllSettings.defaultCtrl.put(absolutePath).save()
                    loadLayout(absolutePath)
                } catch (e: IOException) {
                    Tools.showError(context, e)
                } catch (e: JsonSyntaxException) {
                    Tools.showError(context, e)
                }
            }
        })
        dialog.setTitleText(R.string.customctrl_selectdefault)
        dialog.show()
    }

    fun openExitDialog(exitListener: EditorExitable) {
        TipDialog.Builder(context)
            .setTitle(R.string.customctrl_editor_exit_title)
            .setMessage(R.string.customctrl_editor_exit_msg)
            .setConfirmClickListener { exitListener.exitEditor() }
            .showDialog()
    }

    fun areControlVisible(): Boolean {
        return mControlVisible
    }
}
