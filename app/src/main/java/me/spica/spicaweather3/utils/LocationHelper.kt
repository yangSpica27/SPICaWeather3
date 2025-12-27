package me.spica.spicaweather3.utils

import android.content.Context
import android.util.Log
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import me.spica.spicaweather3.R

class LocationHelper(private val appContext: Context) {

  private val locationClient = LocationClient(appContext)

  fun fetchLocation(
    onSuccess: (BDLocation) -> Unit,
    onFailure: (Exception) -> Unit
  ) {
    val option = LocationClientOption();
    option.locationMode = LocationClientOption.LocationMode.Hight_Accuracy;
    option.setFirstLocType(LocationClientOption.FirstLocType.ACCURACY_IN_FIRST_LOC)
    option.setScanSpan(1000);
    option.isOpenGnss = true;
    option.isLocationNotify = true;
    option.setIgnoreKillProcess(false);
    option.SetIgnoreCacheException(false);
    option.setWifiCacheTimeOut(5 * 60 * 1000);
    option.setEnableSimulateGnss(false);
    option.setNeedNewVersionRgc(true);
    option.setIsNeedAddress(true)
    locationClient.locOption = option;
    locationClient.registerLocationListener(object : BDAbstractLocationListener() {
      override fun onReceiveLocation(location: BDLocation) {
        val addr = location.getAddrStr() //获取详细地址信息
        val country = location.getCountry() //获取国家
        val province = location.getProvince() //获取省份
        val city = location.getCity() //获取城市
        val district = location.getDistrict() //获取区县
        val street = location.getStreet() //获取街道信息
        val adcode = location.getAdCode() //获取adcode
        val town = location.getTown() //获取乡镇信息
        val latitude = location.getLatitude() //获取纬度信息
        val longitude = location.getLongitude() //获取经度信息
        val locType = location.getLocType() //获取位置类型
        val locTypeDescription = location.getLocTypeDescription() //获取位置类型描述
        Log.e("LocationHelper", "onReceiveLocation: $location")
        locationClient.stop()
        if (
          (latitude > 0 && longitude > 0) &&
          (longitude < 180 && latitude < 90) &&
          location.hasAddr()
        ) {
          onSuccess(location)
        } else {
          onFailure(
            Exception(
              appContext.getString(
                R.string.location_failed_with_code,
                location.locType.toString()
              )
            )
          )
        }
      }

    })
    locationClient.start()
  }

}