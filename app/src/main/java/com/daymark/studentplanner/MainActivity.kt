package com.daymark.studentplanner

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("daymark_data", MODE_PRIVATE) }
    private val tasks = mutableListOf<Task>()
    private val subjects = mutableListOf<Subject>()
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var summary: TextView
    private var currentTab = "calendar"
    private val timetable = mutableListOf<CollegeClass>()
    private var selectedDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
    private val studentBatch = "IT1"
    data class CollegeClass(val id: Long, val day: Int, val subject: String, val start: String, val end: String, val teacher: String, val room: String, val batch: String, var attendance: String = "")

    data class Task(val id: Long, val title: String, val subject: String, val due: String, var done: Boolean)
    data class Subject(val name: String, var attended: Int, var total: Int, val history: MutableList<Boolean> = mutableListOf())

    private val ink = Color.rgb(28, 37, 54)
    private val muted = Color.rgb(105, 116, 135)
    private val accent = Color.rgb(73, 91, 214)
    private val bg = Color.rgb(246, 248, 252)
    private val card = Color.WHITE
    private val green = Color.rgb(28, 145, 102)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadData()
        buildShell()
        render()
    }

    private fun buildShell() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(18))
        }
        val brandRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = TextView(this).apply {
            text = "daymark"
            textSize = 28f
            setTextColor(ink)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        val badge = TextView(this).apply {
            text = "OFFLINE"
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(accent)
            background = rounded(Color.rgb(232, 235, 255), 30)
            setPadding(dp(10), dp(6), dp(10), dp(6))
        }
        brandRow.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        brandRow.addView(badge)
        page.addView(brandRow)
        val date = TextView(this).apply {
            text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
            textSize = 14f
            setTextColor(muted)
            setPadding(0, dp(4), 0, dp(18))
        }
        page.addView(date)
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(accent, 22)
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        hero.addView(TextView(this).apply {
            text = "A little progress, every day."
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        })
        summary = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.rgb(235, 237, 255))
            setPadding(0, dp(7), 0, 0)
        }
        hero.addView(summary)
        page.addView(hero, matchWrap())
        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(18), 0, dp(12))
        }
        tabs.addView(tabButton("Calendar", "calendar"), LinearLayout.LayoutParams(0, dp(46), 1f))
        tabs.addView(View(this), LinearLayout.LayoutParams(dp(6), 1))
        tabs.addView(tabButton("Tasks", "tasks"), LinearLayout.LayoutParams(0, dp(46), 1f))
        val gap = View(this)
        tabs.addView(gap, LinearLayout.LayoutParams(dp(10), 1))
        tabs.addView(tabButton("Attendance", "attendance"), LinearLayout.LayoutParams(0, dp(46), 1f))
        page.addView(tabs)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(content)
        scroll.addView(page)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val footer = TextView(this).apply {
            text = "Your data stays on this device"
            textSize = 12f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(14))
            setBackgroundColor(Color.WHITE)
        }
        root.addView(footer)
        setContentView(root)
    }

    private fun tabButton(label: String, tab: String): TextView {
        return TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (currentTab == tab) Color.WHITE else ink)
            background = rounded(if (currentTab == tab) accent else Color.WHITE, 14)
            setOnClickListener {
                currentTab = tab
                render()
                // Rebuild tab colors without recreating the whole screen.
                (parent as? LinearLayout)?.let { row ->
                    for (i in 0 until row.childCount) {
                        val child = row.getChildAt(i)
                        if (child is TextView) {
                            val selected = child.text.toString() == when (currentTab) { "calendar" -> "Calendar"; "tasks" -> "Tasks"; else -> "Attendance" }
                            child.setTextColor(if (selected) Color.WHITE else ink)
                            child.background = rounded(if (selected) accent else Color.WHITE, 14)
                        }
                    }
                }
            }
        }
    }

    private fun render() {
        val open = tasks.count { !it.done }
        summary.text = when (currentTab) {
            "tasks" -> "$open task${if (open == 1) "" else "s"} left to focus on"
            "calendar" -> "Roll 266020 • Batch IT1 • Weekly timetable"
            else -> {
                val pct = if (subjects.sumOf { it.total } == 0) 0 else subjects.sumOf { it.attended } * 100 / subjects.sumOf { it.total }
                "$pct% overall attendance • keep your progress visible"
            }
        }
        content.removeAllViews()
        when (currentTab) { "calendar" -> renderCalendar(); "tasks" -> renderTasks(); else -> renderAttendance() }
    }


    private fun renderCalendar() {
        sectionHeading("College calendar", "Weekly timetable • Roll 266020 • Batch IT1")
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dayRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        days.forEachIndexed { index, label ->
            val calendarDay = index + 2
            val actualDay = if (calendarDay == 8) 1 else calendarDay
            val b = TextView(this).apply {
                text = label; gravity = Gravity.CENTER; textSize = 12f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (selectedDay == actualDay) Color.WHITE else ink)
                background = rounded(if (selectedDay == actualDay) accent else Color.WHITE, 10)
                setPadding(0, dp(12), 0, dp(12))
                setOnClickListener { selectedDay = actualDay; render() }
            }
            val lp = LinearLayout.LayoutParams(0, -2, 1f); lp.setMargins(dp(2), 0, dp(2), dp(8))
            dayRow.addView(b, lp)
        }
        content.addView(dayRow)
        val dayName = when (selectedDay) { 1 -> "Sunday"; 2 -> "Monday"; 3 -> "Tuesday"; 4 -> "Wednesday"; 5 -> "Thursday"; 6 -> "Friday"; else -> "Saturday" }
        content.addView(TextView(this).apply { text = dayName; textSize = 18f; typeface = Typeface.DEFAULT_BOLD; setTextColor(ink); setPadding(0, dp(4), 0, dp(10)) })
        content.addView(actionButton("+ Add lecture / practical") { showAddClassDialog() }, matchWrap())
        val classes = timetable.filter { it.day == selectedDay && (it.batch.isBlank() || it.batch.equals(studentBatch, true) || it.batch.equals("ALL", true)) }.sortedBy { it.start }
        if (classes.isEmpty()) emptyCard("No timetable entries yet", "Add your lecture or practical slots. Only IT1 practicals will show for your batch.")
        classes.forEach { cl ->
            val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = rounded(card, 15); setPadding(dp(14), dp(13), dp(14), dp(13)) }
            box.addView(TextView(this).apply { text = "${cl.start} – ${cl.end}"; textSize = 13f; setTextColor(accent); typeface = Typeface.DEFAULT_BOLD })
            box.addView(TextView(this).apply { text = cl.subject; textSize = 17f; setTextColor(ink); typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(4), 0, dp(2)) })
            box.addView(TextView(this).apply { text = listOf(cl.teacher.ifBlank { "Teacher not set" }, cl.room.ifBlank { "Room not set" }, cl.batch.ifBlank { "Lecture" }).joinToString(" • "); textSize = 12f; setTextColor(muted) })
            val mark = TextView(this).apply {
                text = if (cl.attendance.isBlank()) "Attendance: Not marked  ·  Tap to mark" else "Attendance: ${cl.attendance}  ·  Tap to edit"
                textSize = 13f; typeface = Typeface.DEFAULT_BOLD; setTextColor(if (cl.attendance == "Present") green else if (cl.attendance == "Absent") Color.rgb(190,76,76) else accent)
                setPadding(0, dp(12), 0, 0)
                setOnClickListener {
                    val options = arrayOf("Present", "Absent", "Not marked")
                    AlertDialog.Builder(this@MainActivity).setTitle("Attendance • ${cl.subject}").setItems(options) { _, which ->
                        cl.attendance = if (which == 2) "" else options[which]
                        saveData(); render()
                    }.show()
                }
            }
            box.addView(mark)
            val lp = matchWrap(); lp.topMargin = dp(8); content.addView(box, lp)
        }
        content.addView(TextView(this).apply { text = "Tip: add each theory slot and practical slot once. Batch is set to IT1 for roll 266020."; textSize = 12f; setTextColor(muted); setPadding(dp(2), dp(12), dp(2), dp(8)) })
    }

    private fun showAddClassDialog() {
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(4), dp(18), 0) }
        val day = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday")); setSelection((selectedDay + 5) % 7) }
        val subject = EditText(this).apply { hint = "Subject code/name (e.g. HBMT)" }
        val start = EditText(this).apply { hint = "Start time (e.g. 10:00)" }
        val end = EditText(this).apply { hint = "End time (e.g. 11:00)" }
        val teacher = EditText(this).apply { hint = "Teacher code (e.g. JMK)" }
        val room = EditText(this).apply { hint = "Room / lab (e.g. FF58)" }
        val batch = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Lecture / all batches", "IT1", "IT2", "IT3")) }
        listOf<View>(day, subject, start, end, teacher, room, batch).forEach { form.addView(it, matchWrap()) }
        val dialog = AlertDialog.Builder(this).setTitle("Add timetable slot").setView(form).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (subject.text.isBlank() || start.text.isBlank() || end.text.isBlank()) { subject.error = if (subject.text.isBlank()) "Required" else null; start.error = if (start.text.isBlank()) "Required" else null; end.error = if (end.text.isBlank()) "Required" else null; return@setOnClickListener }
                val dayNum = when (day.selectedItemPosition) { 0 -> 2; 1 -> 3; 2 -> 4; 3 -> 5; 4 -> 6; 5 -> 7; else -> 1 }
                val batchValue = when (batch.selectedItemPosition) { 1 -> "IT1"; 2 -> "IT2"; 3 -> "IT3"; else -> "ALL" }
                timetable.add(CollegeClass(System.currentTimeMillis(), dayNum, subject.text.toString().trim(), start.text.toString().trim(), end.text.toString().trim(), teacher.text.toString().trim(), room.text.toString().trim(), batchValue))
                selectedDay = dayNum; saveData(); dialog.dismiss(); render()
            }
        }
        dialog.show()
    }

    private fun renderTasks() {
        sectionHeading("Today's focus", "Add a task and tick it off when done.")
        val add = actionButton("+  Add task") { showAddTaskDialog() }
        content.addView(add, matchWrap())
        if (tasks.isEmpty()) {
            emptyCard("Nothing on your list yet", "Add a small task to get started.")
        } else {
            tasks.sortedWith(compareBy<Task> { it.done }.thenBy { it.due }).forEach { task ->
                val item = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = rounded(card, 16)
                    setPadding(dp(12), dp(12), dp(10), dp(12))
                }
                val check = CheckBox(this).apply {
                    isChecked = task.done
                    buttonTintList = android.content.res.ColorStateList.valueOf(if (task.done) green else accent)
                    setOnCheckedChangeListener { _, checked ->
                        task.done = checked
                        saveData()
                        render()
                    }
                }
                item.addView(check)
                val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                val title = TextView(this).apply {
                    text = task.title
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(if (task.done) muted else ink)
                    if (task.done) paintFlags = paintFlags or 16
                }
                val meta = TextView(this).apply {
                    text = listOf(task.subject.ifBlank { "General" }, task.due.ifBlank { "No due date" }).joinToString(" • ")
                    textSize = 12f
                    setTextColor(muted)
                    setPadding(0, dp(3), 0, 0)
                }
                texts.addView(title)
                texts.addView(meta)
                item.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
                val delete = TextView(this).apply {
                    text = "×"
                    textSize = 24f
                    setTextColor(muted)
                    gravity = Gravity.CENTER
                    setPadding(dp(10), 0, dp(4), 0)
                    setOnClickListener {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("Delete task?")
                            .setMessage(task.title)
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Delete") { _, _ -> tasks.remove(task); saveData(); render() }
                            .show()
                    }
                }
                item.addView(delete)
                val lp = matchWrap()
                lp.bottomMargin = dp(9)
                content.addView(item, lp)
            }
        }
        if (tasks.any { it.done }) {
            val clear = TextView(this).apply {
                text = "Clear completed tasks"
                textSize = 13f
                setTextColor(accent)
                gravity = Gravity.CENTER
                setPadding(0, dp(12), 0, dp(12))
                setOnClickListener { tasks.removeAll { it.done }; saveData(); render() }
            }
            content.addView(clear, matchWrap())
        }
    }

    private fun renderAttendance() {
        sectionHeading("Attendance tracker", "Record classes attended. No account needed.")
        content.addView(actionButton("+  Add subject") { showAddSubjectDialog() }, matchWrap())
        if (subjects.isEmpty()) {
            emptyCard("No subjects added", "Add a subject, then record each class as present or absent.")
        } else {
            subjects.forEach { subject ->
                val item = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    background = rounded(card, 16)
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                }
                val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
                top.addView(TextView(this).apply {
                    text = subject.name
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(ink)
                }, LinearLayout.LayoutParams(0, -2, 1f))
                val percent = if (subject.total == 0) 0 else subject.attended * 100 / subject.total
                top.addView(TextView(this).apply {
                    text = "$percent%"
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(if (percent >= 75) green else Color.rgb(196, 116, 30))
                })
                item.addView(top)
                val detail = TextView(this).apply {
                    text = "${subject.attended} attended out of ${subject.total} classes"
                    textSize = 12f
                    setTextColor(muted)
                    setPadding(0, dp(5), 0, dp(10))
                }
                item.addView(detail)
                val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                    max = if (subject.total == 0) 1 else subject.total
                    progress = subject.attended
                    progressTintList = android.content.res.ColorStateList.valueOf(if (percent >= 75) green else accent)
                    progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(232, 235, 242))
                }
                item.addView(bar, LinearLayout.LayoutParams(-1, dp(6)))
                val buttons = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dp(12), 0, 0)
                }
                buttons.addView(smallButton("Present", green) {
                    subject.total += 1; subject.attended += 1; subject.history.add(true); saveData(); render()
                }, LinearLayout.LayoutParams(0, dp(40), 1f))
                buttons.addView(View(this), LinearLayout.LayoutParams(dp(8), 1))
                buttons.addView(smallButton("Absent", Color.rgb(190, 76, 76)) {
                    subject.total += 1; subject.history.add(false); saveData(); render()
                }, LinearLayout.LayoutParams(0, dp(40), 1f))
                buttons.addView(View(this), LinearLayout.LayoutParams(dp(8), 1))
                buttons.addView(smallButton("Undo", muted) {
                    if (subject.total > 0) {
                        if (subject.history.isNotEmpty()) {
                            val lastWasPresent = subject.history.removeAt(subject.history.lastIndex)
                            if (lastWasPresent) subject.attended = (subject.attended - 1).coerceAtLeast(0)
                            subject.total = (subject.total - 1).coerceAtLeast(0)
                        } else {
                            // Legacy saved data has no action history, so retain the safest available fallback.
                            if (subject.attended == subject.total) subject.attended = (subject.attended - 1).coerceAtLeast(0)
                            subject.total = (subject.total - 1).coerceAtLeast(0)
                            if (subject.attended > subject.total) subject.attended = subject.total
                        }
                        saveData(); render()
                    }
                }, LinearLayout.LayoutParams(0, dp(40), 0.75f))
                item.addView(buttons)
                val lp = matchWrap(); lp.bottomMargin = dp(10)
                content.addView(item, lp)
            }
            content.addView(TextView(this).apply {
                text = "Attendance percentages are estimates based on the classes you record."
                textSize = 12f
                setTextColor(muted)
                setPadding(dp(2), dp(6), dp(2), dp(10))
            })
        }
    }

    private fun sectionHeading(title: String, subtitle: String) {
        content.addView(TextView(this).apply {
            text = title
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
        })
        content.addView(TextView(this).apply {
            text = subtitle
            textSize = 13f
            setTextColor(muted)
            setPadding(0, dp(4), 0, dp(14))
        })
    }

    private fun actionButton(label: String, action: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = rounded(accent, 14)
            setPadding(dp(12), dp(13), dp(12), dp(13))
            setOnClickListener { action() }
        }

    private fun smallButton(label: String, color: Int, action: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color)
            background = rounded(Color.rgb(246, 248, 252), 10)
            setOnClickListener { action() }
        }

    private fun emptyCard(title: String, subtitle: String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(card, 16)
            setPadding(dp(20), dp(26), dp(20), dp(26))
        }
        box.addView(TextView(this).apply {
            text = title
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
            gravity = Gravity.CENTER
        })
        box.addView(TextView(this).apply {
            text = subtitle
            textSize = 13f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        })
        val lp = matchWrap(); lp.topMargin = dp(10)
        content.addView(box, lp)
    }

    private fun showAddTaskDialog() {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val title = EditText(this).apply { hint = "Task (e.g. Revise maths)"; isSingleLine = true }
        val subject = EditText(this).apply { hint = "Subject or category (optional)"; isSingleLine = true }
        val due = EditText(this).apply { hint = "Due date (optional, e.g. 25 Oct)"; isSingleLine = true }
        form.addView(title, matchWrap())
        form.addView(subject, matchWrap())
        form.addView(due, matchWrap())
        val dialog = AlertDialog.Builder(this).setTitle("Add a task").setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val t = title.text.toString().trim()
                if (t.isEmpty()) { title.error = "Enter a task"; return@setOnClickListener }
                tasks.add(Task(System.currentTimeMillis(), t, subject.text.toString().trim(), due.text.toString().trim(), false))
                saveData(); dialog.dismiss(); render()
            }
        }
        dialog.show()
    }

    private fun showAddSubjectDialog() {
        val input = EditText(this).apply { hint = "Subject name"; isSingleLine = true }
        AlertDialog.Builder(this).setTitle("Add a subject").setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty() && subjects.none { it.name.equals(name, true) }) {
                    subjects.add(Subject(name, 0, 0)); saveData(); render()
                } else if (name.isNotEmpty()) {
                    Toast.makeText(this, "That subject already exists.", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    private fun loadData() {
        try {
            val taskArray = JSONArray(prefs.getString("tasks", "[]"))
            for (i in 0 until taskArray.length()) {
                val o = taskArray.getJSONObject(i)
                tasks.add(Task(o.optLong("id"), o.optString("title"), o.optString("subject"), o.optString("due"), o.optBoolean("done")))
            }
            val classArray = JSONArray(prefs.getString("timetable", "[]"))
            for (i in 0 until classArray.length()) { val o = classArray.getJSONObject(i); timetable.add(CollegeClass(o.optLong("id"), o.optInt("day"), o.optString("subject"), o.optString("start"), o.optString("end"), o.optString("teacher"), o.optString("room"), o.optString("batch"), o.optString("attendance"))) }
            val subjectArray = JSONArray(prefs.getString("subjects", "[]"))
            for (i in 0 until subjectArray.length()) {
                val o = subjectArray.getJSONObject(i)
                val historyArray = o.optJSONArray("history")
                val history = mutableListOf<Boolean>()
                if (historyArray != null) {
                    for (j in 0 until historyArray.length()) history.add(historyArray.optBoolean(j))
                }
                subjects.add(Subject(o.optString("name"), o.optInt("attended"), o.optInt("total"), history))
            }
        } catch (_: Exception) {
            tasks.clear(); subjects.clear(); timetable.clear()
        }
    }

    private fun saveData() {
        val taskArray = JSONArray()
        tasks.forEach { task ->
            taskArray.put(JSONObject().put("id", task.id).put("title", task.title)
                .put("subject", task.subject).put("due", task.due).put("done", task.done))
        }
        val subjectArray = JSONArray()
        subjects.forEach { subject ->
            val historyArray = JSONArray()
            subject.history.forEach { historyArray.put(it) }
            subjectArray.put(JSONObject().put("name", subject.name)
                .put("attended", subject.attended).put("total", subject.total)
                .put("history", historyArray))
        }
        val classArray = JSONArray()
        timetable.forEach { cl -> classArray.put(JSONObject().put("id", cl.id).put("day", cl.day).put("subject", cl.subject).put("start", cl.start).put("end", cl.end).put("teacher", cl.teacher).put("room", cl.room).put("batch", cl.batch).put("attendance", cl.attendance)) }
        prefs.edit().putString("tasks", taskArray.toString())
            .putString("subjects", subjectArray.toString()).putString("timetable", classArray.toString()).apply()
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun matchWrap(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(-1, -2)
}
