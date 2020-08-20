package com.example.walkingpath

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore

class SaveData : AppCompatActivity() {
    private val quizDb = FirebaseFirestore.getInstance().collection("WalkingData")
    private val dataToSave = mutableMapOf<String, String>() //각 다큐먼트의 필드
//    var items: MutableList<SearchData> = mutableListOf() //엑셀 파일의 내용을 저장하는 리스트

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.empty_layout)

        val walkingDate = intent.getStringExtra("WalkingDate")
        val startTime = intent.getStringExtra("WalkingStartTime")
        val endTime = intent.getStringExtra("WalkingEndTime")
        val walkingTime = intent.getStringExtra("WalkingTime")


        //저장할 데이터를 만들어줍니다. (dataToSave는 mutableMapOf로 정의해줬습니다)
        dataToSave["walkingDate"] = walkingDate
        dataToSave["startTime"] = startTime
        dataToSave["endTime"] = endTime
        dataToSave["wholeTime"] = walkingTime


        //저는 여러개의 다큐먼트가 필요해서 다큐먼트도 유동적으로 생성되게 했습니다.
        //아래 코드는 dataToSave를 필드로 하여 다큐먼트를 새로 생성한다.
        quizDb.document("$walkingDate $startTime") //매개변수: 다큐먼트의 이름이 된다.
            //set("저장할 데이터")
            .set(dataToSave) //dataToSave가 생성된 다큐먼트의 필드로 저장된다.
            .addOnSuccessListener { documentReference ->
                Log.d("asdf", "저장 성공")
            }
            .addOnFailureListener { e ->
                Log.w("asdf", "Error adding document", e)
            }
        startActivity(Intent(this@SaveData, WalkingRecordActivity::class.java))
        finish() //액티비티 종료
    }
}