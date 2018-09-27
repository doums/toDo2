package com.todo.app

import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.widget.CheckBox
import android.widget.TextView
import java.util.*

fun View.pickUp() {
    (this as CardView).setCardBackgroundColor(MaterialColor.White.aRGB.toInt())
    this.cardElevation = Converter.convertDpToPx(8F).toFloat()
}

fun View.release(color: MaterialColor) {
    (this as CardView).setCardBackgroundColor(color.aRGB.toInt())
    this.cardElevation = Converter.convertDpToPx(2F).toFloat()
}

fun <T> MutableList<T>.swap(index1: Int, index2: Int) {
    val tmp = this[index1]
    this[index1] = this[index2]
    this[index2] = tmp
}

/**
 * Created by pierre on 21/01/18.
 */

class TaskAdapter(
        private val touchListener: TouchListener,
        tasks: MutableList<Task> = ArrayList()
) :
        RecyclerView.Adapter<TaskAdapter.TaskViewHolder>(),
        ItemTouchHelperAdapter {

    private enum class State {
        OnSelect,
        OnMove,
        Idle
    }

    var tasks: MutableList<Task> = tasks
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    private val selectedTasksPosition: MutableList<Int> = mutableListOf()
    private var state = State.Idle

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.list_item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun getItemCount(): Int {
        return tasks.size
    }

    override fun onBindViewHolder(holder: TaskViewHolder?, position: Int) {
        holder?.bindTask(tasks[position])
    }

    fun addTask(task: Task, position: Int) {
        Log.d("test", "addTask")
        tasks.add(position, task)
        notifyItemInserted(position)
    }

    fun clearTasks() {
        Log.d("test", "clearTasks")
        tasks.clear()
        notifyDataSetChanged()
    }

    private fun updateSelectedPositions(position: Int) {
        selectedTasksPosition.forEach {
            if (it > position) {
                val index = selectedTasksPosition.indexOf(it)
                selectedTasksPosition[index] = it - 1
            }
        }
    }

    fun removeSelectedTasks() {
        Log.d("test", "removeSelectedTasks")
        while (!selectedTasksPosition.isEmpty()) {
            val position = selectedTasksPosition.first()
            tasks.removeAt(position)
            selectedTasksPosition.remove(position)
            notifyItemRemoved(position)
            updateSelectedPositions(position)
        }
    }

    fun recolorSelectedTasks(color: MaterialColor) {
        Log.d("test", "recolor tasks")
        selectedTasksPosition.forEach { tasks[it].color = color }
    }

    fun deselectAll() {
        Log.d("test", "deselectAll")
        if (state != TaskAdapter.State.OnMove) {
            selectedTasksPosition.forEach { notifyItemChanged(it) }
            selectedTasksPosition.clear()
            if (state == TaskAdapter.State.OnSelect)
                state = TaskAdapter.State.Idle
        }
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        Log.d("test", "onItemMove ")
        if (state == State.OnSelect) {
            state = State.OnMove
            touchListener.onStopSelect()
            selectedTasksPosition.clear()
        }
        state = State.OnMove
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                tasks.swap(i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                tasks.swap(i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    override fun onItemDismiss(position: Int) {
        tasks.removeAt(position)
        notifyItemRemoved(position)
    }

    fun selectTask(v: View, position: Int) {
        if (!selectedTasksPosition.any { it == position }) {
            selectedTasksPosition.add(position)
            v.pickUp()
        }
    }

    fun deselectTask(v: View, position: Int) {
        v.release(tasks[position].color)
        selectedTasksPosition.remove(position)
    }

    inner class TaskViewHolder(private var view: View) :
            RecyclerView.ViewHolder(view),
            View.OnClickListener,
            View.OnLongClickListener,
            ItemTouchHelperViewHolder {

        private val descriptionTextView = view.findViewById(R.id.task_description) as TextView
        private val completedCheckBox = view.findViewById(R.id.task_completed) as CheckBox

        init {
            view.setOnClickListener(this)
            view.setOnLongClickListener(this)
        }

        fun bindTask(task: Task) {
            Log.d("viewHolder", "bindTask " + task.id)
            descriptionTextView.text = task.description
            completedCheckBox.isChecked = task.completed
            if (state == State.OnSelect && selectedTasksPosition.any { it == adapterPosition })
                view.pickUp()
            else
                view.release(task.color)
            completedCheckBox.setOnCheckedChangeListener { _, isChecked ->
                tasks[adapterPosition].completed = isChecked
            }
        }

        override fun onClick(v: View) {
            if (adapterPosition != RecyclerView.NO_POSITION) {
                when (state) {
                    TaskAdapter.State.Idle -> {
                        Log.d("test", "onClick Idle")
                        completedCheckBox.isChecked = !completedCheckBox.isChecked
                    }
                    TaskAdapter.State.OnSelect -> {
                        Log.d("test", "onClick onSelect")
                        if (selectedTasksPosition.any { it == adapterPosition })
                            deselectTask(v, adapterPosition)
                        else
                            selectTask(v, adapterPosition)
                        if (selectedTasksPosition.isEmpty()) {
                            touchListener.onStopSelect()
                            state = State.Idle
                        }
                    }
                    else -> return
                }
            }
        }

        override fun onLongClick(v: View): Boolean {
            Log.d("test", "onItemSelected")
            if (adapterPosition != RecyclerView.NO_POSITION && state == TaskAdapter.State.Idle) {
                Log.d("test", "onItemSelected Idle")
                touchListener.onStartSelect()
                state = State.OnSelect
                touchListener.onStartDrag(this)
                selectTask(v, adapterPosition)
            }
            return true
        }

        override fun onItemClear() {
            Log.d("test", "onItemClear")
            if (state == State.OnSelect)
                view.pickUp()
            if (state == State.OnMove) {
                state = State.Idle
                view.release(tasks[adapterPosition].color)
            }
        }
    }
}