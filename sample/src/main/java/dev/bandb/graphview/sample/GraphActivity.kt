package dev.bandb.graphview.sample

import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.otaliastudios.zoom.OverPanRangeProvider
import dev.bandb.graphview.AbstractGraphAdapter
import dev.bandb.graphview.graph.Graph
import dev.bandb.graphview.graph.Node
import java.util.*

import com.otaliastudios.zoom.ZoomLayout

abstract class GraphActivity : AppCompatActivity() {
    protected lateinit var recyclerView: RecyclerView
    protected lateinit var adapter: AbstractGraphAdapter<NodeViewHolder>
    private lateinit var fab: FloatingActionButton
    private var currentNode: Node? = null
    private var nodeCount = 1

    protected abstract fun createGraph(): Graph
    protected abstract fun setLayoutManager()
    protected abstract fun setEdgeDecoration()
//---------
    lateinit var zoomLayout: ZoomLayout
//---------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)

        val graph = createGraph()
        recyclerView = findViewById(R.id.recycler)
        setLayoutManager()
        setEdgeDecoration()
        setupGraphView(graph)

        setupFab(graph)
        setupToolbar()
    //---------
    zoomLayout = findViewById(R.id.zoomLay)
    //---------
    }

    private fun setupGraphView(graph: Graph) {
        adapter = object : AbstractGraphAdapter<NodeViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NodeViewHolder {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.node, parent, false)
                return NodeViewHolder(view)
            }

            override fun onBindViewHolder(holder: NodeViewHolder, position: Int) {
                holder.textView.text = Objects.requireNonNull(getNodeData(position)).toString()
            }
        }.apply {
            this.submitGraph(graph)
            recyclerView.adapter = this
            //------------
            recyclerView.setPadding(500,500,500,500)
            //------------
        }
    }

    private fun setupFab(graph: Graph) {
        fab = findViewById(R.id.addNode)
        fab.setOnClickListener {
            val newNode = Node(nodeText)
            if (currentNode != null) {
                graph.addEdge(currentNode!!, newNode)
            } else {
                graph.addNode(newNode)
            }
            adapter.notifyDataSetChanged()
        }
        fab.setOnLongClickListener {
            if (currentNode != null) {
                graph.removeNode(currentNode!!)
                currentNode = null
                adapter.notifyDataSetChanged()
                fab.hide()
            }
            true
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val ab = supportActionBar
        if (ab != null) {
            ab.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
            ab.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    protected inner class NodeViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textView: TextView = itemView.findViewById(R.id.textView)

        init {
            itemView.setOnClickListener {
                if (!fab.isShown) {
                    fab.show()
                }
                currentNode = adapter.getNode(bindingAdapterPosition)
                //Snackbar.make(itemView, "Clicked on " + adapter.getNodeData(bindingAdapterPosition)?.toString(),
                //        Snackbar.LENGTH_SHORT).show()

                //---------
                //RectF(0F,0F,10000F,10000F)
                //zoomLayout.engine.setContentSize(10000F, 10000F)
                //zoomLayout.setOverScrollHorizontal(true)
                //zoomLayout.setOverScrollVertical(true)
                //Snackbar.make(itemView, "Clicked on " + adapter.getNodeData(bindingAdapterPosition)?.toString() +
                //        "Coord x " + currentNode?.x + "Coord y " + currentNode?.y +
                //        "Pan X is " + zoomLayout.pan.x.toString() + "Pan Y is "+ zoomLayout.pan.y.toString() +
                //        "Zoom is " + zoomLayout.zoom.toString(),
                //           Snackbar.LENGTH_SHORT).show()


                //zoomLayout.zoomTo(2.5F,true)
                Log.v("OverPanRangeProvider", OverPanRangeProvider.DEFAULT.getOverPan(zoomLayout.engine, true).toString())// = 10F
               // zoomLayout.setOverPanRange(provider = OverPanRangeProvider.DEFAULT.DEFAULT_OVERPAN_FACTOR = 10F )
                        Snackbar.make(itemView, OverPanRangeProvider.DEFAULT.getOverPan(zoomLayout.engine, true).toString(),
                           Snackbar.LENGTH_SHORT).show()


              //  zoomLayout.setOverScrollHorizontal(overScroll = true)
              //  zoomLayout.setOverScrollVertical(overScroll = true)
                //zoomLayout.setAlignment(0);
                //zoomLayout.zoomTo(3f,true)
                val nodeY = -currentNode?.y!!
                val nodeX = -currentNode?.x!!
                val toCentrX = zoomLayout.pivotX/zoomLayout.realZoom
                val toCentrY = zoomLayout.pivotY/zoomLayout.realZoom
                val midOfNodeX = currentNode?.width!!/2
                val midOfNodeY = currentNode?.height!!/2

                zoomLayout.panTo((  nodeX - midOfNodeX + toCentrX ),nodeY - midOfNodeY + toCentrY,true)

                Log.v("NodeX","" + currentNode?.x + " Y " + currentNode?.y )
                Log.v("PanX",(zoomLayout.pan.x).toString() + " Y "+ (zoomLayout.pan.y).toString())
                Log.v("Zoom", zoomLayout.zoom.toString())
                Log.v("_________", "_____________")

            }
        }
    }

    protected val nodeText: String
        get() = "Node " + nodeCount++
}