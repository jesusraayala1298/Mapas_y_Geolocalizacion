package net.jesusramirez.mapas_y_geolocalizacion;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
/*
TODO FUE REALIZADO EN UNA SOLA ACTIVIDAD DONDE SE MUESTRA EL MAPA Y EL COMPONENTE DE BUSQUEDA QUE VA A TRAZAR LA RUTA
 */
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    //VARIABLES USADAS PARA EL MANEJO DEL MAPA, MOSTRARLO Y AÑADIR LAS MARCAS
    SupportMapFragment mapFragment;
    GoogleMap mapa;
    //OBJETO PARA OBTENER LA UBICACION ACTUAL DEL DISPOSITIVO
    FusedLocationProviderClient fusedLocationClient;
    //COMPONENTES QUE SE MUESTRAN EN PANTALLA
    Button btnTrazar;
    TextView txtDireccion;
    //CLAVE PARA EL USO DE LA API DE DIRECCIONES Y MAPAS
    private static final String KEY = "AIzaSyBHtYD_i3eqYqdCroUTQDwzb5FtqD323oc";
    String ORIGEN="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mapa);
        //SE CARGA EL MAPA EN LA PANTALLA PRINCIPAL
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //SE INICIALIZAN LOS OBJETOS
        btnTrazar = findViewById(R.id.btnBuscar);
        txtDireccion = findViewById(R.id.txtdireccion);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        //TRAZAR LA RUTA AL DAR CLIC EN EL BOTON DE BUSCAR
        btnTrazar.setOnClickListener(view -> {
            mapa.clear();
            AgregarDestino();
        });
        //VERIFICAR QUE EL DISPOSITIVO TENGA LOS PERMISOS ACTIVADOS
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        //A TRAVES DEL OBJETO SE OBTIENE LA UBICACION ACTUAL DEL DISPOSITIVO
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                //SE ALMACENA LA LATITUD Y LONGITUD ACTUAL DEL DISPOSITIVO
                ORIGEN= location.getLatitude() + "," + location.getLongitude()+"";
                //AL MAPA SE AGREGA UNA MARCA QUE REPRESENTA LA UBICACION ACTUAL DEL DISPOSITIVO
                mapa.addMarker(new MarkerOptions()
                        .position(new LatLng(location.getLatitude(), location.getLongitude()))
                        .title("UBICACION ACTUAL"));
            } else {
                Toast.makeText(this, "Error", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapa = googleMap;
    }


    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    public void AgregarDestino(){
        //LUGAR DE DESTINO A BUSCAR
        String DESTINO = txtDireccion.getText().toString();
        //LISTA QUE REPRESENTA LOS PUNTOS DE LA POLILINEA
        List<List<LatLng>> lista = new ArrayList<>();
        //PETICION QUE REGRESA LOS PUNTOS REPRESENTANDO LA RUTA DESDE LA UBICACION ACTUAL AL DESTINO
        String PETICION = "https://maps.googleapis.com/maps/api/directions/json?origin=" + ORIGEN + "&destination=" + DESTINO + "&key=" + KEY;
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        //OBTENER LA RESPUESTA
        StringRequest stringRequest = new StringRequest(Request.Method.GET,
                PETICION,
                response -> {
                    try {
                        //SE USAN LOS OBJETOS JSON PARA ESTRUCTURAR LA RESPUESTA
                        JSONObject obj = new JSONObject(response);
                        JSONArray listaPuntos = obj.getJSONArray("routes")
                                .getJSONObject(0).getJSONArray("legs")
                                .getJSONObject(0).getJSONArray("steps");
                        //EL CICLO SE USA PARA RECORRER LA RUTA QUE REGRESO LA API Y AÑADIR PUNTO A PUNTO EN LA
                        //LISTA QUE REPRESENTA LA RUTA A SEGUIR
                        for (int i = 0; i < listaPuntos.length(); i++) {
                            String puntos = listaPuntos.getJSONObject(i).
                                    getJSONObject("polyline").
                                    getString("points");
                            lista.add(PolyUtil.decode(puntos));
                        }
                        //ESTE CICLO ARMA LA POLILINEA Y LA VA MOSTRANDO EN EL MAPA
                        for (int i = 0; i < lista.size(); i++) {
                            Log.d("RUTAS", lista.get(i).toString());
                            mapa.addPolyline(new PolylineOptions().addAll(lista.get(i)).color(Color.RED));
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    //AL MAPA SE AGREGA UNA MARCA QUE REPRESENTA LA UBICACION ACTUAL DEL DISPOSITIVO
                    mapa.addMarker(new MarkerOptions()
                            .position((lista.get(0).get(0)))
                            .title("UBICACION ACTUAL"));
                    //SE AÑADE UNA MARCA EN EL DESTINO
                    mapa.addMarker(new MarkerOptions()
                            .position(lista.get(lista.size() - 1).get(lista.get(lista.size() - 1).size() - 1))
                            .title("DESTINO"));
                    //AL MAPA SE LE AGREGA UNA ANIMACION QUE MUESTRE LA RUTA QUE VA DEL ORIGEN AL DESTINO
                    CameraPosition cameraPosition = CameraPosition.builder().
                            target(lista.get(0).get(0))
                            .zoom(10)
                            .tilt(60)
                            .bearing(0)
                            .build();
                    mapa.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                },
                error -> {
                    Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                });
        requestQueue.add(stringRequest);
    }
}