package marc.com.esri.application.activity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import marc.com.esri.MainActivity;
import marc.com.esri.R;
import marc.com.esri.application.MyApplication;

public class BaiduAcitivity extends AppCompatActivity {
	private LocationManager locationManager;
	private String locationProvider;

	@Bind(R.id.baidu_map)
	MapView baiduMap;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_baidu_acitivity);
		ButterKnife.bind(this);

		initLocation();
		getLocation();
	}

	@OnClick(R.id.baidu_map)
	public void onClick() {
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		baiduMap.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		baiduMap.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		baiduMap.onPause();
	}
	private void getLocation() {
//		symbol = new SimpleMarkerSymbol(Color.RED, 25, SimpleMarkerSymbol.STYLE.CIRCLE);
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
	private void showLocation(Location location) {
		String locationStr = "维度：" + location.getLatitude() + "\n"
				+ "经度：" + location.getLongitude();
		Toast.makeText(BaiduAcitivity.this, locationStr, Toast.LENGTH_SHORT).show();

//		Point p = new Point(location.getLongitude(),location.getLatitude());
//		Point curP = mMapView.toScreenPoint(p);
		/*Point p = checkPoint(location.getLongitude(),location.getLatitude());
		graphic = new Graphic(p,symbol);
		layer1.addGraphic(graphic);*/
//		BitmapFactory.
		Bitmap bitmap =BitmapFactory.decodeResource(getResources(), R.drawable.test);
		BitmapDescriptor bt = BitmapDescriptorFactory.fromBitmap(bitmap);
		Point mLocation = new Point(location.getLongitude(), location.getLatitude());
		LatLng latLng = new LatLng(mLocation.getX(),mLocation.getY());
		OverlayOptions option = new MarkerOptions()
				.position(latLng)
				.icon(bt);
//		baiduMap.addOverlay(option);
		baiduMap.getMap().addOverlay(option);
		/*Point p = (Point) GeometryEngine.project(mLocation, egs, wm);
		mMapView.zoomToResolution(p, 20.0);
		mMapView.centerAt(p, true);
		mMapView.setScale(2000);
		graphic = new Graphic(p, symbol);
		layer1.addGraphic(graphic);*/
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

	private void initLocation(){
		LocationClientOption option = new LocationClientOption();
		option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy
		);//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
		option.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系
		int span=1000;
		option.setScanSpan(span);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
		option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
		option.setOpenGps(true);//可选，默认false,设置是否使用gps
		option.setLocationNotify(true);//可选，默认false，设置是否当GPS有效时按照1S/1次频率输出GPS结果
		option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
		option.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
		option.setIgnoreKillProcess(false);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
		option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
		option.setEnableSimulateGps(false);//可选，默认false，设置是否需要过滤GPS仿真结果，默认需要
		MyApplication.mLocationClient.setLocOption(option);
	}
}
