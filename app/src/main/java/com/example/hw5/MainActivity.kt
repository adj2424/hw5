package com.example.hw5

import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private var recyclerView: RecyclerView?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)

        var locationItemList = mutableListOf(
            LocationItem("address", "coordinates", "time", 0),
            LocationItem("address", "coordinates", "time", 0),
            LocationItem("address", "coordinates", "time", 0),
            LocationItem("address", "coordinates", "time", 0),
            LocationItem("address", "coordinates", "time", 0)
        )
        val adapter = Adapter(locationItemList)
        recyclerView?.adapter = adapter
        recyclerView?.layoutManager = LinearLayoutManager(this)
        var itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        itemDecoration.setDrawable(getDrawable(R.drawable.custom_divider)!!)
        recyclerView?.addItemDecoration(itemDecoration)
        adapter.notifyDataSetChanged()


    }
}