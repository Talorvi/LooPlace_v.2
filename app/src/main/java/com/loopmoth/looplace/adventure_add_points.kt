package com.loopmoth.looplace

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_adventure_add_points.*

class adventure_add_points : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var database: DatabaseReference
    //odwołanie do bazy danych

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val mMarkerArray = ArrayList<Marker>()
    //tablica markerów na mapie

    private val mCMarkerArray = ArrayList<CMarker>()
    //tablica własnych markerów, inaczej wyrzuca błąd przy wczytywaniu listy

    private var long: Double? = 0.0
    private var lat: Double? = 0.0
    //współrzędne użytkownika

    private var pointLong: Double = 0.0
    private var pointLat: Double = 0.0
    //współrzędne klikanego punktu

    private var pointSelected: Boolean = false
    //czy punkt jest zaznaczony?

    private var tempmarker: Marker? = null
    //tymczasowy marker tworzony podczas kliknięcia na mapę

    private lateinit var name: String
    private lateinit var desc: String
    private lateinit var key: String
    private val adventureArray: MutableList<Adventure> = mutableListOf()
    private var adventureTemp: Adventure? = null
    //dane z formularza wyżej

    private var infoWindowShown: Boolean = false
    //czy okno z informacjami o markerze jest pokazane?

    private val character = 5
    private val singlecharacter = character.toChar()
    //separator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adventure_add_points)

        database = FirebaseDatabase.getInstance().reference

        name = intent.getStringExtra("name")
        desc = intent.getStringExtra("desc")

        if(intent.getStringExtra("adventureKey")!=null){
            key = intent.getStringExtra("adventureKey")
            initAdventure()
        }
        //przesłane dane z poprzedniego formularza

        viewForm.visibility = View.INVISIBLE

        //Toast.makeText(this@adventure_add_points, name+desc, Toast.LENGTH_LONG).show()
        //do sprawdzenia czy przesyłanie danych pomiędzy aktywnościami jest poprawne

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        //do sprawdzenia ostatniej lokalizacji użytkownika, aby otworzyć mapę w jego lokalizacji

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        //wczytanie fragmentu mapy
    }

    override fun onResume() {
        super.onResume()
        bAdd.setOnClickListener {
            if(tempmarker!=null && tName.text.toString()!="" && tDesc.text.toString()!="" && number.text.toString()!="" && tQuestion.text.toString()!="" && tAnswer.text.toString()!=""){
                //jeżeli jest tymczasowy marker (czyli po kliknięciu na mapę)
                pointLat = tempmarker!!.position.latitude
                pointLong = tempmarker!!.position.longitude
                //zapisanie jego współrzędnych

                mMarkerArray.remove(tempmarker!!)
                tempmarker!!.remove()
                //usunięcie tego markera

                val mark = mMap.addMarker(MarkerOptions().title(tName.text.toString()).snippet(tDesc.text.toString()).position(LatLng(pointLat, pointLong)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)))
                mark.tag = number.text.toString().toInt().toString() + singlecharacter + tQuestion.text.toString() + singlecharacter + tAnswer.text.toString()
                //dodanie nowego, już stałego markera
                pointSelected = false
                viewForm.visibility = View.INVISIBLE
                clearFormView()

                mMarkerArray.add(mark)
                //dodanie markera do tablicy
            }
            else{
                Toast.makeText(this@adventure_add_points, "Uzupełnij pola tekstowe", Toast.LENGTH_LONG).show()
            }
        }
        bCancel.setOnClickListener {
            if(tempmarker!=null){
                mMarkerArray.remove(tempmarker!!)
                tempmarker!!.remove()
                pointSelected = false
                viewForm.visibility = View.INVISIBLE
                clearFormView()
                //usunięcie zaznaczonego znacznika
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        //stworzenie menu
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.nav, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if(mMarkerArray.size>0){
            //wysłanie do firebase'a
            if(mMarkerArray.size>0){
                if(intent.getStringExtra("adventureKey")!=null){
                    database.child("adventures").child(key!!).setValue(null)
                    //wyzerowanie gałęzi skutkuje usunięciem jej z bazy
                }

                val newAdventure = database.child("adventures").push()
                val key = newAdventure.key
                //pobranie klucza generowanego przez FireBase

                for (marker in mMarkerArray) {
                    mCMarkerArray.add(CMarker(marker.id, marker.title, marker.snippet, marker.tag, marker.position.latitude, marker.position.longitude))
                    //potrzeba było stworzyć włany obiekt markerów, inaczej pokazywały się błędy
                }

                val adventure = Adventure(name, desc, mCMarkerArray, key)
                newAdventure.setValue(adventure)
                //dodanie do bazy

                val intent = Intent(this@adventure_add_points, MainMenu::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                Toast.makeText(this@adventure_add_points, "Dodano przygodę do bazy danych", Toast.LENGTH_LONG).show()
                startActivity(intent)
                //powrót do menu głównego
            }
            else{
                Toast.makeText(this@adventure_add_points, "Zaznacz conajmniej jeden punkt na mapie", Toast.LENGTH_LONG).show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        //odniesienie do mapy, aby można było w łatwy sposób odwoływać się do mapy
        obtainLocation()
        //pobranie lokalizacji, przeniesienie tam widoku i prybliżenie

        mMap.setOnMapClickListener {
            //po kliknięciu na mapę -> akcja, it-> obiekt LatLng przetrzymujący współrzędne, ma właściowści latitude i longitude
            if(!pointSelected && !infoWindowShown){
                //jeżeli jest już fioletowy marker i nie jest pokazane okienko z informacjami dotyczącego wskaźnika -> nie rób nic, inaczej -> pokaż formularz i dodaj marker
                tempmarker = mMap.addMarker(MarkerOptions().position(it).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))) //ZAD tutaj sobie zmieniasz na jaki kolor chcesz
                viewForm.visibility = View.VISIBLE
                pointSelected = true
            }
            else{
                viewForm.visibility = View.INVISIBLE
                infoWindowShown = false
                clearFormView()
                //po odznaczeniu wskaźnika schowaj okno z edycją wskaźnika
            }
        }

        mMap.setOnMarkerClickListener {
            if(!it.isInfoWindowShown){
                it.showInfoWindow()
                infoWindowShown = true
                viewForm.visibility = View.VISIBLE
                //pokaż okno z informacjami dotyczącymi punktu oraz okno z możliwością edycji punktu

                setFormViewText(it.title.toString(), it.snippet.toString(), getNumber(it.tag.toString()).toInt().toString(), getQuestion(it.tag.toString()), getAnswer(it.tag.toString()))// it.tag.toString().toInt().toString())
                //ustawienie tekstu

                tempmarker = it
            }

            return@setOnMarkerClickListener true
        }
    }

    @SuppressLint("MissingPermission")
    private fun obtainLocation(){
        //pobieranie lokalizacji użytkownika
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                lat =  location?.latitude
                long = location?.longitude
                //pobranie koordynatów użytkownika

                val currentLocation = LatLng(lat!!, long!!)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16.0f))
                //newLatLng - też poprawna funkcja, ale bez możliwości przybliżania od strony programistycznej

                //val mark = mMap.addMarker(MarkerOptions().position(LatLng(lat!!, long!!)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)))
                //ZAD marker, będzie błąd bo nulle w stringach 
            }
    }

    private fun clearFormView(){
        //wyczyszczenie pól
        tName.text.clear()
        tDesc.text.clear()
        number.text.clear()
        tQuestion.text.clear()
        tAnswer.text.clear()
    }

    private fun setFormViewText(name: String, desc: String, nb: String, question: String, answer: String){
        //ustawienie wartości pól
        tName.setText(name)
        tDesc.setText(desc)
        number.setText(nb)
        tQuestion.setText(question)
        tAnswer.setText(answer)
    }

    private fun initAdventure() {
        //wczytanie markerów z bazy na mapę i dodanie tych markerów do tablicy
        val markerListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                adventureArray.clear()
                dataSnapshot.children.mapNotNullTo(adventureArray) { it.getValue<Adventure>(Adventure::class.java) }
                //mapowanie z bazy do tablicy

                adventureArray.forEach{
                    if(it.key==key){
                        //jeżeli klucz przygody jest ten, którego poszukujemy, to stwórz przygodę z danych zawartych w bazie
                        adventureTemp = Adventure(it.name, it.description, it.markers!!, it.key)
                    }
                }

                adventureTemp!!.markers!!.forEach{
                    val mark = mMap.addMarker(MarkerOptions().title(it.title).snippet(it.snippet).position(LatLng(it.latitude!!, it.longitude!!)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)))
                    mark.tag = it.tag
                    //dodanie markerów na mapę

                    mMarkerArray.add(mark)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                println("loadPost:onCancelled ${databaseError.toException()}")
                //jeżeli nie uda się połączyć z bazą
            }
        }
        database.child("adventures").addListenerForSingleValueEvent(markerListener)
    }

    private fun getNumber(tag: String): String{
        //wyciągnięcie liczby z tagu
        val result = tag.split(singlecharacter)
        return result[0]
    }

    private fun getQuestion(tag: String): String{
        //wyciągnięcie pytania z tagu
        val result = tag.split(singlecharacter)
        return result[1]
    }

    private fun getAnswer(tag: String): String{
        //wyciągnięcie odpowiedzi z tagu
        val result = tag.split(singlecharacter)
        return result[2]
    }
}
