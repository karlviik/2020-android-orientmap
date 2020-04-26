package ee.taltech.orientmap

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import ee.taltech.orientmap.db.LocationRepository
import ee.taltech.orientmap.db.SessionRepository
import ee.taltech.orientmap.poko.SessionModel
import kotlinx.android.synthetic.main.session_tile.view.*


class DataRecyclerViewAdapterSessions(val context: Context, private val sessionRepo: SessionRepository, private var locationRepo: LocationRepository) : RecyclerView.Adapter<DataRecyclerViewAdapterSessions.ViewHolder>() {
	inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
	
	var dataSet : MutableList<SessionModel>
	private val inflater: LayoutInflater = LayoutInflater.from(context)
	
	init {
		dataSet = sessionRepo.getAll() as MutableList<SessionModel>
	}
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val rowView = inflater.inflate(R.layout.session_tile, parent, false)
		return ViewHolder(rowView)
	}
	
	override fun getItemCount(): Int {
		return dataSet.count()
	}
	
	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val session = dataSet[position]
		holder.itemView.textViewSessionName.text = session.name
		holder.itemView.textViewSessionTime.text = session.start.toString()
		holder.itemView.textViewSessionDistance.text = String.format("Distance: %d meters", session.distance)
		holder.itemView.textViewSessionDuration.text = String.format("Duration: %d:%d:%d", session.duration / 3600, session.duration % 3600 / 60, session.duration % 60)
		holder.itemView.textViewSessionTimePerKm.text = String.format("Time per km: %d:%d", session.timePerKm / 60, session.timePerKm % 60)
		holder.itemView.buttonDeleteSession.setOnClickListener {
			sessionRepo.delete(session)
			locationRepo.deleteSessionLocations(session.id)
			dataSet.removeAt(position)
			notifyDataSetChanged()
		}
		holder.itemView.buttonRenameSession.setOnClickListener {
			val dialogBuilder: AlertDialog = AlertDialog.Builder(context).create()
			val inflater: LayoutInflater = inflater
			val dialogView: View = inflater.inflate(R.layout.edit_session_dialog, null)
			
			val editText = dialogView.findViewById<View>(R.id.edt_comment)!! as EditText
			val button1: Button = dialogView.findViewById<View>(R.id.buttonSubmit)!! as Button
			val button2: Button = dialogView.findViewById<View>(R.id.buttonCancel)!! as Button
			
			button2.setOnClickListener(View.OnClickListener { dialogBuilder.dismiss() })
			button1.setOnClickListener(View.OnClickListener { // DO SOMETHINGS
				val newName: String = editText.text.toString()
				session.name = newName
				sessionRepo.update(session)
				notifyDataSetChanged()
				dialogBuilder.dismiss()
			})
			
			dialogBuilder.setView(dialogView)
			dialogBuilder.show()
		}
		holder.itemView.buttonViewSession.setOnClickListener {
		
		}
	}
}