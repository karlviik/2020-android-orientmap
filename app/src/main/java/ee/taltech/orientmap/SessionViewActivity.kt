package ee.taltech.orientmap

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import ee.taltech.orientmap.db.SessionRepository
import kotlinx.android.synthetic.main.activity_session_view.*

class SessionViewActivity : AppCompatActivity() {
	
	private lateinit var sessionRepository: SessionRepository
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_session_view)
		
		sessionRepository = SessionRepository(this).open()
		
		recyclerViewSessions.layoutManager = LinearLayoutManager(this)
		recyclerViewSessions.adapter = DataRecyclerViewAdapterSessions(this, sessionRepository)
	}
	
	override fun onDestroy() {
		super.onDestroy()
		sessionRepository.close()
	}
}
