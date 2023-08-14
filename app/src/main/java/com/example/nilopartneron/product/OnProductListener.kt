package com.example.nilopartneron.product

import com.example.nilopartneron.entities.Product

interface OnProductListener {
    fun onClick(product: Product)
    fun onLongClick(product: Product)
}