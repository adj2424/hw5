package com.example.hw5

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.location_item.view.*

class Adapter (var locationItemList: List<LocationItem>) : RecyclerView.Adapter<Adapter.LocationViewHolder>(){
    inner class LocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.location_item, parent, false)
        return LocationViewHolder(view)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.itemView.apply {
            address.text = locationItemList[position].address
            coordinates.text = locationItemList[position].coordinates
            time.text = locationItemList[position].time
        }
    }

    override fun getItemCount(): Int {
        return locationItemList.size
    }


}