package de.blox.graphview

import android.content.Context
import android.database.DataSetObserver
import android.graphics.*
import android.support.annotation.ColorInt
import android.support.annotation.Px
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView

internal class GraphNodeContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AdapterView<GraphAdapter<*>>(context, attrs, defStyleAttr) {
    private var linePaint: Paint

    init {
        linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = lineThickness
            color = lineColor
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND    // set the join to round you want
            pathEffect = CornerPathEffect(10f)   // set the path effect when they join.
        }

        attrs?.let { initAttrs(context, it) }
    }

    var lineThickness: Float = DEFAULT_LINE_THICKNESS
        set(@Px value) {
            linePaint.strokeWidth = value
            field = value
            invalidate()
        }

    var lineColor: Int = DEFAULT_LINE_COLOR
        @ColorInt get
        set(@ColorInt value) {
            linePaint.color = value
            field = value
            invalidate()
        }

    var isUsingMaxSize: Boolean = DEFAULT_USE_MAX_SIZE
        set(value) {
            field = value
            invalidate()
            requestLayout()
        }

    private var adapter: GraphAdapter<*>? = null

    private var maxChildWidth: Int = 0
    private var maxChildHeight: Int = 0
    private val rect: Rect by lazy {
        Rect()
    }

    private var dataSetObserver: DataSetObserver = GraphDataSetObserver()
    private val gestureDetector: GestureDetector = GestureDetector(getContext(), GestureListener())

    private fun initAttrs(context: Context, attrs: AttributeSet) {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.GraphView, 0, 0)

        lineThickness = a.getDimension(R.styleable.GraphView_lineThickness, DEFAULT_LINE_THICKNESS)
        lineColor = a.getColor(R.styleable.GraphView_lineColor, DEFAULT_LINE_COLOR)
        isUsingMaxSize = a.getBoolean(R.styleable.GraphView_useMaxSize, DEFAULT_USE_MAX_SIZE)

        a.recycle()
    }

    private fun positionItems() {
        var maxLeft = Integer.MAX_VALUE
        var maxRight = Integer.MIN_VALUE
        var maxTop = Integer.MAX_VALUE
        var maxBottom = Integer.MIN_VALUE

        for (index in 0 until adapter!!.count) {
            val child = adapter!!.getView(index, null, this)
            addAndMeasureChild(child)

            val width = child.measuredWidth
            val height = child.measuredHeight

            val (x, y) = adapter!!.getScreenPosition(index)

            // calculate the size and position of this child
            val left = x.toInt()
            val top = y.toInt()
            val right = left + width
            val bottom = top + height

            child.layout(left, top, right, bottom)

            maxRight = Math.max(maxRight, right)
            maxLeft = Math.min(maxLeft, left)
            maxBottom = Math.max(maxBottom, bottom)
            maxTop = Math.min(maxTop, top)
        }
    }

    private fun addAndMeasureChild(child: View) {
        var params: ViewGroup.LayoutParams? = child.layoutParams
        if (params == null) {
            params = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        addViewInLayout(child, -1, params, false)
        var widthSpec = makeMeasureSpec(params.width)
        var heightSpec = makeMeasureSpec(params.height)

        if (isUsingMaxSize) {
            widthSpec = View.MeasureSpec.makeMeasureSpec(
                maxChildWidth, View.MeasureSpec.EXACTLY
            )
            heightSpec = View.MeasureSpec.makeMeasureSpec(
                maxChildHeight, View.MeasureSpec.EXACTLY
            )
        }

        child.measure(widthSpec, heightSpec)
    }


    private fun getContainingChildIndex(x: Int, y: Int): Int {
        for (index in 0 until childCount) {
            getChildAt(index).getHitRect(rect)
            if (rect.contains(x, y)) {
                return index
            }
        }
        return INVALID_INDEX
    }

    private fun clickChildAt(x: Int, y: Int) {
        val index = getContainingChildIndex(x, y)
        // no child found at this position
        if (index == INVALID_INDEX) {
            return
        }

        val itemView = getChildAt(index)
        val id = adapter!!.getItemId(index)
        performItemClick(itemView, index, id)
    }

    private fun longClickChildAt(x: Int, y: Int) {
        val index = getContainingChildIndex(x, y)
        // no child found at this position
        if (index == INVALID_INDEX) {
            return
        }

        val itemView = getChildAt(index)
        val id = adapter!!.getItemId(index)
        val listener = onItemLongClickListener
        listener?.onItemLongClick(this, itemView, index, id)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun onLayout(
        changed: Boolean, left: Int, top: Int, right: Int,
        bottom: Int
    ) {
        super.onLayout(changed, left, top, right, bottom)

        if (adapter == null) {
            return
        }

        removeAllViewsInLayout()
        positionItems()

        invalidate()
    }

    override fun getSelectedView(): View? {
        return null
    }

    override fun setSelection(position: Int) {}

    override fun dispatchDraw(canvas: Canvas) {
        val adapter = getAdapter()
        val graph = adapter?.graph
        if (graph != null && graph.hasNodes()) {
            adapter.algorithm.drawEdges(canvas, graph, linePaint)
        }

        super.dispatchDraw(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val adapter = this.adapter ?: return

        var maxWidth = 0
        var maxHeight = 0
        var minHeight = Integer.MAX_VALUE

        for (i in 0 until adapter.count) {
            val child = adapter.getView(i, null, this)

            var params: ViewGroup.LayoutParams? = child.layoutParams
            if (params == null) {
                params = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            addViewInLayout(child, -1, params, true)

            val childWidthSpec = makeMeasureSpec(params.width)
            val childHeightSpec = makeMeasureSpec(params.height)

            child.measure(childWidthSpec, childHeightSpec)
            val node = adapter.getNode(i)
            val measuredWidth = child.measuredWidth
            val measuredHeight = child.measuredHeight
            node.size.apply {
                width = measuredWidth
                height = measuredHeight
            }

            maxWidth = Math.max(maxWidth, measuredWidth)
            maxHeight = Math.max(maxHeight, measuredHeight)
            minHeight = Math.min(minHeight, measuredHeight)
        }

        maxChildWidth = maxWidth
        maxChildHeight = maxHeight

        if (isUsingMaxSize) {
            removeAllViewsInLayout()
            for (i in 0 until adapter.count) {
                val child = adapter.getView(i, null, this)

                var params: ViewGroup.LayoutParams? = child.layoutParams
                if (params == null) {
                    params = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                addViewInLayout(child, -1, params, true)

                val widthSpec =
                    View.MeasureSpec.makeMeasureSpec(maxChildWidth, View.MeasureSpec.EXACTLY)
                val heightSpec =
                    View.MeasureSpec.makeMeasureSpec(maxChildHeight, View.MeasureSpec.EXACTLY)
                child.measure(widthSpec, heightSpec)

                val node = adapter.getNode(i)
                node.size.apply {
                    width = child.measuredWidth
                    height = child.measuredHeight
                }
            }
        }
        adapter.notifySizeChanged()

        val size = adapter.algorithm.graphSize
        setMeasuredDimension(size.width, size.height)
    }

    override fun setAdapter(adapter: GraphAdapter<*>?) {
        val oldAdapter = this.adapter
        oldAdapter?.unregisterDataSetObserver(dataSetObserver)

        this.adapter = adapter
        this.adapter?.let {
            it.registerDataSetObserver(dataSetObserver)
            requestLayout()
        }
    }

    override fun getAdapter(): GraphAdapter<*>? = adapter

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            clickChildAt(e.x.toInt(), e.y.toInt())
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            longClickChildAt(e.x.toInt(), e.y.toInt())
        }
    }

    private inner class GraphDataSetObserver : DataSetObserver() {
        override fun onChanged() {
            super.onChanged()

            refresh()
        }

        override fun onInvalidated() {
            super.onInvalidated()

            refresh()
        }

        private fun refresh() {
            requestLayout()
            invalidate()
        }
    }

    companion object {

        const val DEFAULT_USE_MAX_SIZE = false
        const val DEFAULT_LINE_THICKNESS = 5f
        const val DEFAULT_LINE_COLOR = Color.BLACK
        const val INVALID_INDEX = -1

        private fun makeMeasureSpec(dimension: Int): Int {
            return if (dimension > 0) {
                MeasureSpec.makeMeasureSpec(dimension, MeasureSpec.EXACTLY)
            } else {
                MeasureSpec.UNSPECIFIED
            }
        }
    }
}
