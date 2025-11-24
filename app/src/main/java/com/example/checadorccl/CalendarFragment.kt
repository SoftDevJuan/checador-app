package com.example.checadorccl

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.checadorccl.api.ApiClient
import com.example.checadorccl.api.models.EventoResponse
import com.example.checadorccl.utils.ToastHelper
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import com.kizitonwose.calendar.view.CalendarView
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class CalendarFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var monthYearText: TextView

    private var eventsMap: Map<LocalDate, List<EventoResponse>> = emptyMap()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        calendarView = view.findViewById(R.id.calendarView)
        monthYearText = view.findViewById(R.id.calendarMonthYearText)

        setupCalendarBinders()

        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(12)
        val endMonth = currentMonth.plusMonths(12)
        val daysOfWeek = daysOfWeek(firstDayOfWeek = DayOfWeek.SUNDAY)

        calendarView.setup(startMonth, endMonth, daysOfWeek.first())
        calendarView.scrollToMonth(currentMonth)

        calendarView.monthScrollListener = {
            val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "MX"))
            monthYearText.text = it.yearMonth.format(formatter).replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
        }
        view.findViewById<View>(R.id.btnNextMonth).setOnClickListener {
            calendarView.findFirstVisibleMonth()?.let { calendarView.smoothScrollToMonth(it.yearMonth.plusMonths(1)) }
        }
        view.findViewById<View>(R.id.btnPrevMonth).setOnClickListener {
            calendarView.findFirstVisibleMonth()?.let { calendarView.smoothScrollToMonth(it.yearMonth.minusMonths(1)) }
        }

        fetchCalendarEvents()
    }

    private fun setupCalendarBinders() {
        class DayViewContainer(view: View) : ViewContainer(view) {
            val textView = view.findViewById<TextView>(R.id.calendarDayText)
            val eventLabel = view.findViewById<TextView>(R.id.calendarEventLabel)
            lateinit var day: CalendarDay

            init {
                view.setOnClickListener {
                    val eventosDelDia = eventsMap[day.date]
                    if (!eventosDelDia.isNullOrEmpty()) {
                        // Abrir Detalle del PRIMER evento
                        val evento = eventosDelDia[0]
                        abrirDetalleEvento(evento, day.date)
                    }
                }
            }
        }

        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day = data
                container.textView.text = data.date.dayOfMonth.toString()

                if (data.position == DayPosition.MonthDate) {
                    container.textView.setTextColor(Color.BLACK)
                    val eventos = eventsMap[data.date]

                    if (!eventos.isNullOrEmpty()) {
                        val evento = eventos[0]
                        container.eventLabel.visibility = View.VISIBLE
                        container.eventLabel.text = evento.titulo
                        container.eventLabel.setBackgroundColor(Color.parseColor(obtenerColorHex(evento.tipoVisual)))
                    } else {
                        container.eventLabel.visibility = View.GONE
                    }
                } else {
                    container.textView.setTextColor(Color.LTGRAY)
                    container.eventLabel.visibility = View.GONE
                }
            }
        }

        class MonthViewContainer(view: View) : ViewContainer(view)
        calendarView.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
            override fun create(view: View) = MonthViewContainer(view)
            override fun bind(container: MonthViewContainer, data: CalendarMonth) {}
        }
    }

    private fun fetchCalendarEvents() {
        val prefs = requireContext().getSharedPreferences("ChecadorPrefs", Context.MODE_PRIVATE)
        val token = prefs.getString("ACCESS_TOKEN", null) ?: return

        lifecycleScope.launch {
            try {
                val response = ApiClient.service.getCalendarEvents("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    val listaEventos = response.body()!!
                    procesarEventosParaCalendario(listaEventos)
                }
            } catch (e: Exception) {
                ToastHelper.show(requireContext(), "Error cargando calendario", true)
            }
        }
    }

    private fun procesarEventosParaCalendario(lista: List<EventoResponse>) {
        val nuevoMapa = mutableMapOf<LocalDate, MutableList<EventoResponse>>()

        for (evento in lista) {
            for (fechaInfo in evento.fechas) {
                try {
                    val date = LocalDate.parse(fechaInfo.fecha)
                    if (!nuevoMapa.containsKey(date)) {
                        nuevoMapa[date] = mutableListOf()
                    }
                    nuevoMapa[date]?.add(evento)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        eventsMap = nuevoMapa
        calendarView.notifyCalendarChanged()
    }

    private fun abrirDetalleEvento(evento: EventoResponse, fecha: LocalDate) {
        val intent = Intent(context, EventDetailsActivity::class.java)

        // CAMBIO IMPORTANTE: Pasamos el objeto completo serializado
        intent.putExtra("EVENTO_OBJ", evento)
        intent.putExtra("EVENT_COLOR", obtenerColorHex(evento.tipoVisual))

        startActivity(intent)
    }

    private fun obtenerColorHex(tipo: String): String {
        return when (tipo) {
            "VACATION" -> "#4CAF50"
            "HOLIDAY" -> "#F44336"
            "PERMISSION" -> "#2196F3"
            "INCAPACITY" -> "#9C27B0"
            else -> "#9E9E9E"
        }
    }
}