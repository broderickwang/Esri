package marc.com.esri;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapOptions;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISLocalTiledLayer;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.TextSymbol;
import com.esri.core.tasks.geocode.Locator;
import com.esri.core.tasks.geocode.LocatorFindParameters;
import com.esri.core.tasks.geocode.LocatorGeocodeResult;
import com.getbase.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import marc.com.esri.application.activity.BaiduAcitivity;

public class MainActivity extends AppCompatActivity {

	@Bind(R.id.map)
	MapView mMapView;

	// The basemap switching menu items.
	MenuItem mStreetsMenuItem = null;
	MenuItem mTopoMenuItem = null;
	MenuItem mGrayMenuItem = null;
	MenuItem mOceansMenuItem = null;

	// Create MapOptions for each type of basemap.
	final MapOptions mTopoBasemap = new MapOptions(MapOptions.MapType.TOPO);
	final MapOptions mStreetsBasemap = new MapOptions(MapOptions.MapType.STREETS);
	final MapOptions mGrayBasemap = new MapOptions(MapOptions.MapType.GRAY);
	final MapOptions mOceansBasemap = new MapOptions(MapOptions.MapType.OCEANS);

	//search pleace
	GraphicsLayer mLocationLayer;
	Point mLocationLayerPoint;
	String mLocationLayerPointString;
	boolean mIsMapLoaded;

	EditText mSearchEditText;
	@Bind(R.id.location)
	FloatingActionButton location;

	GraphicsLayer layer1;
	Graphic graphic;
	SimpleMarkerSymbol symbol;
	final SpatialReference wm = SpatialReference.create(102100);
	final SpatialReference egs = SpatialReference.create(4326);
	private LocationManager locationManager;
	private String locationProvider;
	private ArcGISLocalTiledLayer mLocalTiledLayerGoogle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ButterKnife.bind(this);
		mLocationLayer = new GraphicsLayer();
		mMapView.addLayer(mLocationLayer);

		mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {
			public void onStatusChanged(Object source, STATUS status) {
				if ((source == mMapView) && (status == STATUS.INITIALIZED)) {
					mIsMapLoaded = true;
				}
			}
		});
		layer1 = new GraphicsLayer();
		mMapView.addLayer(layer1);

		mLocalTiledLayerGoogle = new ArcGISLocalTiledLayer(getFileAbsolutePath(this, "/PDAlayers/qdgoogle"));
		mMapView.addLayer(mLocalTiledLayerGoogle);
	}

	public static String getFileAbsolutePath(Context context, String relativePath) {
		String absolutePath = "";
		String path = Environment.getExternalStorageDirectory().getAbsolutePath() + relativePath;
		File f0 = new File(path);
		File f1 = new File(Environment.getRootDirectory().getAbsolutePath() + relativePath);
		File f2 = new File(getStoragePath(context, true) + relativePath);
		if (f0.exists()) {
			absolutePath = Environment.getExternalStorageDirectory().getAbsolutePath() + relativePath;
		} else if (f1.exists()) {
			absolutePath = Environment.getRootDirectory().getAbsolutePath() + relativePath;
		} else if (f2.exists()) {
			absolutePath = getStoragePath(context, true) + relativePath;
		}

		if ("".equals(absolutePath)) {
			return null;
		} else {
			return absolutePath;
		}
	}

	private static String getStoragePath(Context mContext, boolean isStorageRemovable) {

		StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
		String storagePath = null;
		Class<?> storageVolumeClazz = null;
		try {
			storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
			Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
			Method getPath = storageVolumeClazz.getMethod("getPath");
			Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
			Object result = getVolumeList.invoke(mStorageManager);
			final int length = Array.getLength(result);
			for (int i = 0; i < length; i++) {
				Object storageVolumeElement = Array.get(result, i);
				String path = (String) getPath.invoke(storageVolumeElement);
				boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
				if (isStorageRemovable == removable) {
					storagePath = path;
					break;
				}
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return storagePath;
	}


	private void getLocation() {
		symbol = new SimpleMarkerSymbol(Color.RED, 25, SimpleMarkerSymbol.STYLE.CIRCLE);
//获取地理位置管理器
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		//获取所有可用的位置提供器
		List<String> providers = locationManager.getProviders(true);
		if (providers.contains(LocationManager.GPS_PROVIDER)) {
			//如果是GPS
			locationProvider = LocationManager.GPS_PROVIDER;
		} else if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
			//如果是Network
			locationProvider = LocationManager.NETWORK_PROVIDER;
		} else {
			Toast.makeText(this, "没有可用的位置提供器", Toast.LENGTH_SHORT).show();
			return;
		}
		//获取Location
		checkPermission("android.permission.INTERNET", 0, 0);
		Location location = locationManager.getLastKnownLocation(locationProvider);
		if (location != null) {
			//不为空,显示地理位置经纬度
			showLocation(location);
		}
		//监视地理位置变化
		locationManager.requestLocationUpdates(locationProvider, 3000, 1, locationListener);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.mainmenu, menu);
		/*View searchRef = menu.findItem(R.id.action_search).getActionView();
		mSearchEditText = (EditText) searchRef.findViewById(R.id.searchText);

		mSearchEditText.setOnKeyListener(new View.OnKeyListener() {

			@Override
			public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
				if(keyCode == KeyEvent.KEYCODE_ENTER){
					onSearchButtonClicked(mSearchEditText);
					return true;
				}
				return false;
			}
		});*/

		// Get the basemap switching menu items.
		mStreetsMenuItem = menu.getItem(0);
		mTopoMenuItem = menu.getItem(1);
		mGrayMenuItem = menu.getItem(2);
		mOceansMenuItem = menu.getItem(3);

// Also set the topo basemap menu item to be checked, as this is the default.
		mTopoMenuItem.setChecked(true);

		return true;
	}

	public void onSearchButtonClicked(View view) {
		InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
		String address = mSearchEditText.getText().toString();
		executeLocatorTask(address);
	}

	private void executeLocatorTask(String address) {
		// Create Locator parameters from single line address string
		LocatorFindParameters findParams = new LocatorFindParameters(address);

		// Use the centre of the current map extent as the find location point
		findParams.setLocation(mMapView.getCenter(), mMapView.getSpatialReference());

		// Calculate distance for find operation
		Envelope mapExtent = new Envelope();
		mMapView.getExtent().queryEnvelope(mapExtent);
		// assume map is in metres, other units wont work, double current envelope
		double distance = (mapExtent != null && mapExtent.getWidth() > 0) ? mapExtent.getWidth() * 2 : 10000;
		findParams.setDistance(distance);
		findParams.setMaxLocations(2);

		// Set address spatial reference to match map
		findParams.setOutSR(mMapView.getSpatialReference());

		// Execute async task to find the address
		new LocatorAsyncTask().execute(findParams);
		mLocationLayerPointString = address;
	}

	LocationListener locationListener = new LocationListener() {

		@Override
		public void onStatusChanged(String provider, int status, Bundle arg2) {

		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onLocationChanged(Location location) {
			//如果位置发生变化,重新显示
			showLocation(location);

		}
	};

	private void showLocation(Location location) {
		String locationStr = "维度：" + location.getLatitude() + "\n"
				+ "经度：" + location.getLongitude();
		Toast.makeText(MainActivity.this, locationStr, Toast.LENGTH_SHORT).show();
		Log.i("TAG", "showLocation: "+locationStr);

//		Point p = new Point(location.getLongitude(),location.getLatitude());
//		Point curP = mMapView.toScreenPoint(p);
		/*Point p = checkPoint(location.getLongitude(),location.getLatitude());
		graphic = new Graphic(p,symbol);
		layer1.addGraphic(graphic);*/
		Point mLocation = new Point(location.getLongitude(), location.getLatitude());
		Point p = (Point) GeometryEngine.project(mLocation, egs, wm);
		mMapView.zoomToResolution(p, 20.0);
		mMapView.centerAt(p, true);
		mMapView.setScale(2000);
		graphic = new Graphic(p, symbol);
		layer1.addGraphic(graphic);
	}

	@OnClick({R.id.detailed_action_update_notice, R.id.location})
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.detailed_action_update_notice:
				Intent i = new Intent(MainActivity.this, BaiduAcitivity.class);
				startActivity(i);
				break;
			case R.id.location:
				getLocation();
				break;
		}
	}

	private class LocatorAsyncTask extends AsyncTask<LocatorFindParameters, Void, List<LocatorGeocodeResult>> {
		private Exception mException;

		@Override
		protected List<LocatorGeocodeResult> doInBackground(LocatorFindParameters... locatorFindParameterses) {
			mException = null;
			List<LocatorGeocodeResult> results = null;
			Locator locator = Locator.createOnlineLocator();
			try {
				results = locator.find(locatorFindParameterses[0]);
			} catch (Exception e) {
				mException = e;
			}
			return results;
		}

		@Override
		protected void onPostExecute(List<LocatorGeocodeResult> result) {
			if (mException != null) {
				Log.w("PlaceSearch", "LocatorSyncTask failed with:");
				mException.printStackTrace();
				Toast.makeText(MainActivity.this, "addressSearchFailed", Toast.LENGTH_LONG).show();
				return;
			}

			if (result.size() == 0) {
				Toast.makeText(MainActivity.this, "noResultsFound", Toast.LENGTH_LONG).show();
			} else {
				// Use first result in the list
				LocatorGeocodeResult geocodeResult = result.get(0);

				// get return geometry from geocode result
				Point resultPoint = geocodeResult.getLocation();
				// create marker symbol to represent location
				SimpleMarkerSymbol resultSymbol = new SimpleMarkerSymbol(Color.RED, 16, SimpleMarkerSymbol.STYLE.CROSS);
				// create graphic object for resulting location
				Graphic resultLocGraphic = new Graphic(resultPoint, resultSymbol);
				// add graphic to location layer
				mLocationLayer.addGraphic(resultLocGraphic);

				// create text symbol for return address
				String address = geocodeResult.getAddress();
				TextSymbol resultAddress = new TextSymbol(20, address, Color.BLACK);
				// create offset for text
				resultAddress.setOffsetX(-4 * address.length());
				resultAddress.setOffsetY(10);
				// create a graphic object for address text
				Graphic resultText = new Graphic(resultPoint, resultAddress);
				// add address text graphic to location graphics layer
				mLocationLayer.addGraphic(resultText);

				mLocationLayerPoint = resultPoint;

				// Zoom map to geocode result location
				mMapView.zoomToResolution(geocodeResult.getLocation(), 2);
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_search) {
			return true;
		}
		switch (item.getItemId()) {
			case R.id.World_Street_Map:
				mMapView.setMapOptions(mStreetsBasemap);
				mStreetsMenuItem.setChecked(true);
				return true;
			case R.id.World_Topo:
				mMapView.setMapOptions(mTopoBasemap);
				mTopoMenuItem.setChecked(true);
				return true;
			case R.id.Gray:
				mMapView.setMapOptions(mGrayBasemap);
				mGrayMenuItem.setChecked(true);
				return true;
			case R.id.Ocean_Basemap:
				mMapView.setMapOptions(mOceansBasemap);
				mOceansMenuItem.setChecked(true);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
