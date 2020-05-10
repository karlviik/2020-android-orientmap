package ee.taltech.orientmap

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.location.Location
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Response
import ee.taltech.orientmap.activities.MainActivity
import ee.taltech.orientmap.activities.SessionViewActivity
import ee.taltech.orientmap.db.LocationRepository
import ee.taltech.orientmap.db.SessionRepository
import ee.taltech.orientmap.poko.SessionModel
import ee.taltech.orientmap.service.LocationService
import ee.taltech.orientmap.utils.ApiUtils
import ee.taltech.orientmap.utils.PreferenceUtils
import ee.taltech.orientmap.utils.Utils
import kotlinx.android.synthetic.main.session_tile.view.*
import org.json.JSONObject
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle


class DataRecyclerViewAdapterSessions(
	val context: Context,
	private val sessionRepo: SessionRepository,
	private var locationRepo: LocationRepository
) : RecyclerView.Adapter<DataRecyclerViewAdapterSessions.ViewHolder>() {
	
	inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
	
	var dataSet: MutableList<SessionModel>
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
		holder.itemView.textViewSessionTime.text = session.start.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
		holder.itemView.textViewSessionDistance.text = String.format("Distance: %d meters", session.distance)
		holder.itemView.textViewSessionDuration.text = String.format("Duration: %d:%d:%d", session.duration / 3600, session.duration % 3600 / 60, session.duration % 60)
		holder.itemView.textViewSessionTimePerKm.text = String.format("Time per km: %d:%d", session.timePerKm / 60, session.timePerKm % 60)
		holder.itemView.textViewLocationCount.text = String.format("Locations: %d", session.locationCount)
		holder.itemView.textViewSyncedLocationCount.text = String.format("Synced locations: %d", session.uploadedLocationCount)
		val ownerText = if (!TextUtils.isEmpty(session.userEmail)) session.userEmail else "none"
		holder.itemView.textViewOwningUser.text = String.format("Account: %s", ownerText)
		holder.itemView.buttonDeleteSession.setOnClickListener {
			val dialogClickListener = DialogInterface.OnClickListener { _, which ->
				when (which) {
					DialogInterface.BUTTON_POSITIVE -> {
						sessionRepo.delete(session)
						locationRepo.deleteSessionLocations(session.id)
						dataSet.removeAt(position)
						notifyDataSetChanged()
						
					}
					DialogInterface.BUTTON_NEGATIVE -> {
					}
				}
			}
			val ab = AlertDialog.Builder(context)
			ab.setMessage("Are you sure you want to delete the session?").setPositiveButton("Yes", dialogClickListener)
				.setNegativeButton("No", dialogClickListener).show()
		}
		holder.itemView.buttonRenameSession.setOnClickListener {
			val dialogBuilder: AlertDialog = AlertDialog.Builder(context).create()
			val inflater: LayoutInflater = inflater
			val dialogView: View = inflater.inflate(R.layout.edit_session_dialog, null)
			
			val editText = dialogView.findViewById<View>(R.id.edt_comment)!! as EditText
			val editTextFast = dialogView.findViewById<View>(R.id.editTextSessionChangeFastPace)!! as EditText
			val editTextSlow = dialogView.findViewById<View>(R.id.editTextSessionChangeSlowPace)!! as EditText
			val button1: Button = dialogView.findViewById<View>(R.id.buttonSubmit)!! as Button
			val button2: Button = dialogView.findViewById<View>(R.id.buttonCancel)!! as Button
			
			button2.setOnClickListener(View.OnClickListener { dialogBuilder.dismiss() })
			button1.setOnClickListener(View.OnClickListener {
				val newName: String = editText.text.toString()
				if (!TextUtils.isEmpty(newName)) session.name = newName
				val fastPace = editTextSlow.text.toString().toIntOrNull()
				val slowPace = editTextFast.text.toString().toIntOrNull()
				var isOk = false
				if (fastPace == null && slowPace == null) {
					session.gradientFastTime = null
					session.gradientSlowTime = null
					isOk = true
				} else if (fastPace == null && slowPace != null || fastPace != null && slowPace == null) {
					Toast.makeText(context, "Either both fast and slow pace must be set or neither!", Toast.LENGTH_SHORT).show()
				} else if (fastPace!! < 60) {
					Toast.makeText(context, "Fast pace must be at least 60 seconds!", Toast.LENGTH_SHORT).show()
				} else if (fastPace >= slowPace!!) {
					Toast.makeText(context, "Fast pace can't be longer than slow pace!", Toast.LENGTH_SHORT).show()
				} else {
					session.gradientFastTime = fastPace
					session.gradientSlowTime = slowPace
					isOk = true
				}
				if (isOk) {
					if (session.userEmail == PreferenceUtils.getUserEmail(context)) {
						val token = PreferenceUtils.getToken(context)
						val listener = Response.Listener<JSONObject> { response ->
							// eh, nothing to do really
						}
						val errorListener = Response.ErrorListener { e ->
							Toast.makeText(context, "Locally saved, but error sending the changes to backend", Toast.LENGTH_SHORT).show()
						}
						ApiUtils.updateSession(context, listener, errorListener, session, token!!)
					}
					sessionRepo.update(session)
					notifyDataSetChanged()
					dialogBuilder.dismiss()
				}
			})
			
			dialogBuilder.setView(dialogView)
			dialogBuilder.show()
		}
		
		holder.itemView.buttonViewSession.setOnClickListener {
			val clearMapIntent = Intent(C.CLEAR_MAP)
			LocalBroadcastManager.getInstance(context).sendBroadcast(clearMapIntent)
			
			val locs = locationRepo.getSessionLocations(session.id).sortedBy { x -> x.recordedAt }
			
			val cps = ArrayList<Location>()
			for (i in locs) {
				if (i.locationType == 2) {
					val newCp = Location("")
					newCp.latitude = i.latitude
					newCp.longitude = i.longitude
					cps.add(newCp)
				}
			}
			MainActivity.cps = cps
			
			val lcs = ArrayList<Location>()
			for (i in locs) {
				if (i.locationType == 0) {
					val newLc = Location("")
					newLc.latitude = i.latitude
					newLc.longitude = i.longitude
					newLc.time = i.recordedAt.toInstant(OffsetDateTime.now().offset).toEpochMilli()
					lcs.add(newLc)
				}
			}
			MainActivity.lcs = lcs
			
			
			val colors = ArrayList<Int>()
			
			val fastTime = session.gradientFastTime ?: PreferenceUtils.getFastSpeedTime(context)
			val slowTime = session.gradientSlowTime ?: PreferenceUtils.getSlowSpeedTime(context)
			
			for (i in 1 until lcs.size) {
				colors.add(
					Utils.getColorBasedOnGradient(lcs[i - 1], lcs[i], fastTime, slowTime, C.FAST_COLOR, C.SLOW_COLOR)
				)
			}
			MainActivity.colors = colors
			
			val test = context as SessionViewActivity
			test.finish()
			
			
		}
		holder.itemView.buttonSyncSession.setOnClickListener {
			val token = PreferenceUtils.getToken(context)
			val email = PreferenceUtils.getUserEmail(context)
			if (token != null && !TextUtils.isEmpty(email)) {
				
				val locationFunction = {
					val allLocations = locationRepo.getSessionLocations(session.id)
					for (location in allLocations) {
						if (location.isUploaded) continue
						val listener = Response.Listener<JSONObject> { _ ->
							session.uploadedLocationCount += 1
							sessionRepo.update(session)
							location.isUploaded = true
							locationRepo.update(location)
							notifyDataSetChanged()
						}
						val errorListener = Response.ErrorListener { _ ->
							// this could get spammy potentially
							Toast.makeText(context, "Error saving a location!", Toast.LENGTH_SHORT).show()
						}
						ApiUtils.createLocation(context, listener, errorListener, location, session.apiId, token)
					}
				}
				
				if (TextUtils.isEmpty(session.apiId)) {
					// session hasn't been created, therefore create it
					val listener = Response.Listener<JSONObject> { response ->
						// on success add data to session and save it and start backing up locations
						session.apiId = response.getString("id") as String
						session.userEmail = email!!
						sessionRepo.update(session)
						locationFunction.invoke()
					}
					val errorListener = Response.ErrorListener { _ ->
						// went south, display some error
						Toast.makeText(context, "Error creating session!", Toast.LENGTH_SHORT).show()
					}
					ApiUtils.createSession(context, listener, errorListener, session, token)
				} else {
					// session has been created, start syncing
					locationFunction.invoke()
				}
			}
		}
		
		holder.itemView.buttonViewSession.isEnabled = !LocationService.isServiceCreated()
		
		val currentEmail = PreferenceUtils.getUserEmail(context)
		// if isn't logged in or is with different user or item is fully synced, disable button
		holder.itemView.buttonSyncSession.isEnabled =
			!(!PreferenceUtils.isLoggedIn(context) || (!TextUtils.isEmpty(session.userEmail) && !currentEmail.equals(session.userEmail)) || session.locationCount == session.uploadedLocationCount)
		
	}
}