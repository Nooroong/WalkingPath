package com.example.walkingpath

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_walking_record.*

class WalkingRecordActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val walkingList = ArrayList<WalkingList>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_walking_record)


        db.collection("WalkingData")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    walkingList.add(
                        WalkingList(
                            document.data["walkingDate"].toString(),
                            document.data["startTime"].toString(),
                            document.data["endTime"].toString(),
                            document.data["wholeTime"].toString()
                        )
                    )
                }
            }
            .addOnFailureListener { exception ->
                Log.w("mapPoint", "Error getting documents.", exception)
            }

        val adapter =
            RecyclerViewAdapter(walkingList, LayoutInflater.from(this@WalkingRecordActivity))
        recycler_view.adapter = adapter
        //수직으로 그리기
        recycler_view.layoutManager = LinearLayoutManager(this@WalkingRecordActivity)
    }
}


class WalkingList(
    val walkingDate: String,
    val startTime: String,
    val endTime: String,
    val walkingTime: String
) {}

class RecyclerViewAdapter(
    var itemList: ArrayList<WalkingList>,
    var inflater: LayoutInflater
) : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() { //얘를 상속받아야함. 제너릭은 inner class type

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val date: TextView
        val start_time: TextView
        val end_time: TextView
        val whole_time: TextView

        //클래스가 생성되자마자 바로 실행되는 부분. 초기화 블록이라고도 한다.
        init {
            date = itemView.findViewById(R.id.walking_date)
            start_time = itemView.findViewById(R.id.start_time)
            end_time = itemView.findViewById(R.id.end_time)
            whole_time = itemView.findViewById(R.id.whole_time)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        //태그를 달고 그것을 이용해 재활용해주는 역할
        //뷰를 만듦
        //부모 뷰에 넣어줌(?). 아이템 하나가 들어갈 뷰를 만든다.
        val view = inflater.inflate(R.layout.item_view, parent, false)
        return ViewHolder(view) //재활용을 위해 ViewHolder 를 리턴
    }

    override fun getItemCount(): Int {
        //아이템 리스트의 사이즈
        return itemList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //holder: 위에 적은 ViewHolder의 객체
        //뷰를 그림
        //holder.carName, holder.carEngine: 텍스트뷰 객체(?)
        holder.date.setText(itemList[position].walkingDate)
        holder.start_time.setText(itemList[position].startTime)
        holder.end_time.setText(itemList[position].endTime)
        holder.whole_time.setText(itemList[position].walkingTime)
    }
}