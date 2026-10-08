package me.shadow.eclipselauncher.anim.animations

import me.shadow.eclipselauncher.anim.animations.bounce.BounceEnlargeAnimator
import me.shadow.eclipselauncher.anim.animations.bounce.BounceInDownAnimator
import me.shadow.eclipselauncher.anim.animations.bounce.BounceInLeftAnimator
import me.shadow.eclipselauncher.anim.animations.bounce.BounceInRightAnimator
import me.shadow.eclipselauncher.anim.animations.bounce.BounceInUpAnimator
import me.shadow.eclipselauncher.anim.animations.bounce.BounceShrinkAnimator
import me.shadow.eclipselauncher.anim.animations.fade.FadeInAnimator
import me.shadow.eclipselauncher.anim.animations.fade.FadeInDownAnimator
import me.shadow.eclipselauncher.anim.animations.fade.FadeInLeftAnimator
import me.shadow.eclipselauncher.anim.animations.fade.FadeInRightAnimator
import me.shadow.eclipselauncher.anim.animations.fade.FadeInUpAnimator
import me.shadow.eclipselauncher.anim.animations.fade.FadeOutAnimator
import me.shadow.eclipselauncher.anim.animations.fade.FadeOutDownAnimator
import me.shadow.eclipselauncher.anim.animations.fade.FadeOutLeftAnimator
import me.shadow.eclipselauncher.anim.animations.fade.FadeOutRightAnimator
import me.shadow.eclipselauncher.anim.animations.fade.FadeOutUpAnimator
import me.shadow.eclipselauncher.anim.animations.other.PulseAnimator
import me.shadow.eclipselauncher.anim.animations.other.ShakeAnimator
import me.shadow.eclipselauncher.anim.animations.other.WobbleAnimator
import me.shadow.eclipselauncher.anim.animations.slide.SlideInDownAnimator
import me.shadow.eclipselauncher.anim.animations.slide.SlideInLeftAnimator
import me.shadow.eclipselauncher.anim.animations.slide.SlideInRightAnimator
import me.shadow.eclipselauncher.anim.animations.slide.SlideInUpAnimator
import me.shadow.eclipselauncher.anim.animations.slide.SlideOutDownAnimator
import me.shadow.eclipselauncher.anim.animations.slide.SlideOutLeftAnimator
import me.shadow.eclipselauncher.anim.animations.slide.SlideOutRightAnimator
import me.shadow.eclipselauncher.anim.animations.slide.SlideOutUpAnimator

enum class Animations(val animator: BaseAnimator) {
    //Bounce
    BounceInDown(BounceInDownAnimator()),
    BounceInLeft(BounceInLeftAnimator()),
    BounceInRight(BounceInRightAnimator()),
    BounceInUp(BounceInUpAnimator()),
    BounceEnlarge(BounceEnlargeAnimator()),
    BounceShrink(BounceShrinkAnimator()),

    //Fade in
    FadeIn(FadeInAnimator()),
    FadeInLeft(FadeInLeftAnimator()),
    FadeInRight(FadeInRightAnimator()),
    FadeInUp(FadeInUpAnimator()),
    FadeInDown(FadeInDownAnimator()),

    //Fade out
    FadeOut(FadeOutAnimator()),
    FadeOutLeft(FadeOutLeftAnimator()),
    FadeOutRight(FadeOutRightAnimator()),
    FadeOutUp(FadeOutUpAnimator()),
    FadeOutDown(FadeOutDownAnimator()),

    //Slide in
    SlideInLeft(SlideInLeftAnimator()),
    SlideInRight(SlideInRightAnimator()),
    SlideInUp(SlideInUpAnimator()),
    SlideInDown(SlideInDownAnimator()),

    //Slide out
    SlideOutLeft(SlideOutLeftAnimator()),
    SlideOutRight(SlideOutRightAnimator()),
    SlideOutUp(SlideOutUpAnimator()),
    SlideOutDown(SlideOutDownAnimator()),

    //Other
    Pulse(PulseAnimator()),
    Wobble(WobbleAnimator()),
    Shake(ShakeAnimator())
}