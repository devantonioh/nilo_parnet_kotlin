package com.example.nilopartneron.order

import com.example.nilopartneron.entities.Order

interface OnOrderListener {
    fun onStartChat(order: Order)
    fun onStatusChange(order: Order)
}