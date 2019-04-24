package com.anwesh.uiprojects.centerdiagrectview

/**
 * Created by anweshmishra on 24/04/19.
 */

import android.animation.Animator
import android.view.View
import android.view.MotionEvent
import android.app.Activity
import android.content.Context
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log

val nodes : Int = 5
val lines : Int = 4
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val strokeFactor : Int = 90
val sizeFactor : Float = 2.9f
val foreColor : Int = Color.parseColor("#4CAF50")
val backColor : Int = Color.parseColor("#BDBDBD")
val rectHFactor : Float = 4f
val delay : Long = 20

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.mirrorValue(a : Int, b : Int) : Float {
    val k : Float = scaleFactor()
    return (1 - k) * a.inverse() + k * b.inverse()
}
fun Float.updateValue(dir : Float, a : Int, b : Int) : Float = mirrorValue(a, b) * dir * scGap

fun Canvas.drawCenterDiag(x : Float, y : Float, sc : Float, paint : Paint) {
    drawLine(0f, 0f, x * sc, y * sc, paint)
}

fun Canvas.drawCDRNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    paint.strokeCap = Paint.Cap.ROUND
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.color = foreColor
    paint.style = Paint.Style.STROKE
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    val angle : Float = Math.atan(1.0 / rectHFactor).toFloat() * 180f / Math.PI.toFloat()
    Log.d("angle", "$angle")
    save()
    translate(w / 2, gap * (i + 1))
    rotate(90f * sc2)
    drawRect(RectF(-size, -size / rectHFactor, size, size / rectHFactor), paint)
    for (j in 0..(lines - 1)) {
        save()
        drawCenterDiag(size * (1f - 2 * (j / 2)), -size / rectHFactor * (1f - 2 * (j % 2)), sc1.divideScale(j, lines), paint)
        restore()
    }
    restore()
}

class CenterDiagRectView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, lines, 1)
            Log.d("scale", "$scale")
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
         }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class CDRNode(var i : Int, val state : State = State()) {

        private var next : CDRNode? = null
        private var prev : CDRNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = CDRNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawCDRNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : CDRNode {
            var curr : CDRNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class CenterDiagRect(var i : Int) {

        private val root : CDRNode = CDRNode(0)
        private var curr : CDRNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : CenterDiagRectView) {

        private val animator : Animator = Animator(view)
        private val cdr : CenterDiagRect = CenterDiagRect(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            cdr.draw(canvas, paint)
            animator.animate {
                cdr.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            cdr.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : CenterDiagRectView {
            val view : CenterDiagRectView = CenterDiagRectView(activity)
            activity.setContentView(view)
            return view
        }
    }
}