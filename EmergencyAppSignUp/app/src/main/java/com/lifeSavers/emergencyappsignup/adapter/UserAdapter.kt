package com.lifeSavers.emergencyappsignup.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lifeSavers.emergencyappsignup.R
import com.lifeSavers.emergencyappsignup.model.User
import com.lifeSavers.emergencyappsignup.databinding.ItemProfileBinding

class UserAdapter(var context: Context, var usersList: ArrayList<User>): RecyclerView.Adapter<UserAdapter.UserViewHolder>() {
    inner class UserViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val binding: ItemProfileBinding = ItemProfileBinding.bind(itemView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        var v = LayoutInflater.from(context).inflate(R.layout.item_profile, parent, false)
        return UserViewHolder(v)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = usersList[position]
        holder.binding.username.text = user.name
        Glide.with(context).load(user.profileImage)
            .placeholder(R.drawable.profile_pic)
            .into(holder.binding.profile)
    }

    override fun getItemCount(): Int = usersList.size
}