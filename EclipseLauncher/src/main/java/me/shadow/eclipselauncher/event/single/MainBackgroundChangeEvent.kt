package me.shadow.eclipselauncher.event.single

/**
 * 当主Activity的背景图片变更时，会通过这个事件进行通知
 * 仅仅只有主Activity需要进行事件通知，因为当背景图片变更时，用户当前的界面一定为主Activity
 * @see me.shadow.eclipselauncher.pojav.LauncherActivity
 * @see me.shadow.eclipselauncher.ui.fragment.CustomBackgroundFragment
 */
class MainBackgroundChangeEvent