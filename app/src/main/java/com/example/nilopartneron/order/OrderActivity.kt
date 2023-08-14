package com.example.nilopartneron.order

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nilopartneron.chat.ChatFragment
import com.example.nilopartneron.Constants
import com.example.nilopartneron.R
import com.example.nilopartneron.databinding.ActivityOrderBinding
import com.example.nilopartneron.entities.Order
import com.example.nilopartneron.fcm.NotificationRS
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase

class OrderActivity : AppCompatActivity(), OnOrderListener, OrderAux {

    private lateinit var binding: ActivityOrderBinding

    private lateinit var adapter: OrderAdapter

    private lateinit var orderSelected: Order

    private lateinit var firebaseAnalytics: FirebaseAnalytics


    private val aValues: Array<String> by lazy {
        resources.getStringArray(R.array.status_value)
    }
    private val aKeys: Array<Int> by lazy {
        resources.getIntArray(R.array.status_key).toTypedArray()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupFirestore()
        configAnalytics()
    }

    private fun setupRecyclerView() {
        adapter = OrderAdapter(mutableListOf(), this)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@OrderActivity)
            adapter = this@OrderActivity.adapter
        }
    }

    private fun setupFirestore() {
        val db = FirebaseFirestore.getInstance()

        db.collection(Constants.COLL_REQUESTS)
            .orderBy(Constants.PROP_DATE, Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener {
                for (document in it) {
                    val order = document.toObject(Order::class.java)
                    order.id = document.id
                    adapter.add(order)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al consultar los datos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun configAnalytics(){
        firebaseAnalytics = Firebase.analytics
    }

    private fun notifyClient(order: Order) {
        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_USERS)
            .document(order.clientId)
            .collection(Constants.COLL_TOKENS)
            .get()
            .addOnSuccessListener {
                var tokesStr = ""
                for (document in it) {
                    val tokenMap = document.data
                    tokesStr += "${tokenMap.getValue(Constants.PROP_TOKEN)},"
                }
                if (tokesStr.length > 0) {
                    tokesStr = tokesStr.dropLast(1)


                    var names = ""
                    order.products.forEach {
                        names += "${it.value.name}, "
                    }
                    names = names.dropLast(2)

                    val index = aKeys.indexOf(order.status)

                    val notificationRS = NotificationRS()
                    notificationRS.sendNotification(
                        "Tu pedido ha sido ${aValues[index]}",
                        names,
                        tokesStr
                    )
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al consultar los datos", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onStartChat(order: Order) {
        orderSelected = order

        val fragment = ChatFragment()

        supportFragmentManager
            .beginTransaction()
            .add(R.id.containerMain, fragment)
            .addToBackStack(null)
            .commit()

    }

    override fun onStatusChange(order: Order) {
        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_REQUESTS)
            .document(order.id)
            .update(Constants.PROP_STATUS, order.status)
            .addOnSuccessListener {
                Toast.makeText(this, "Orden Actualizada", Toast.LENGTH_SHORT).show()
                notifyClient(order)
                //Analytics
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.ADD_SHIPPING_INFO){
                    val products = mutableListOf<Bundle>()
                    order.products.forEach {
                        val bundle = Bundle()
                        bundle.putString("id_product", it.key)
                        products.add(bundle)
                    }
                    param(FirebaseAnalytics.Param.SHIPPING, products.toTypedArray())
                    param(FirebaseAnalytics.Param.PRICE, order.totalPrice)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al Actualizar Orden", Toast.LENGTH_SHORT).show()
            }
    }

    override fun getOrdderSelected(): Order = orderSelected
}