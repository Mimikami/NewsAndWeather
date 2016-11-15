package newsandweather.mimikami.com.newsandweather;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.show.api.ShowApiRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.TimeZone;

import static android.view.View.GONE;


public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_REQUEST_CODE = 0x100;
    private LocationManager locationManager;
    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private LinearLayout layout_main;
    private LinearLayout layout_sub;
    private TextView tv_permission_error;
    private LinearLayout layout_loading;
    private LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layout_main = (LinearLayout) findViewById(R.id.layout_main);
        layout_sub = (LinearLayout) findViewById(R.id.layout_sub);
        tv_permission_error = (TextView) findViewById(R.id.tv_permission_error);
        layout_loading = (LinearLayout) findViewById(R.id.layout_loading);

        layout_main.setVisibility(GONE);
        layout_sub.setVisibility(GONE);
        layout_loading.setVisibility(View.VISIBLE);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            layout_loading.setVisibility(GONE);
            tv_permission_error.setText("请打开定位服务");
            tv_permission_error.setVisibility(View.VISIBLE);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST_CODE);
            return;
        }
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location != null) {
                    getWeatherTodayByGPS(location);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
                tv_permission_error.setVisibility(GONE);
                layout_loading.setVisibility(View.VISIBLE);
            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            getWeatherTodayByGPS(location);
        }
    }

    private void getWeatherTodayByGPS(Location location) {

        try {
            //运行异步任务
            String[] params = new String[]{
                    getString(R.string.showapi_base_url),
                    "5",
                    getString(R.string.showapi_appid),
                    getString(R.string.showapi_sign),
                    Double.toString(location.getLongitude()),
                    Double.toString(location.getLatitude())
            };

            new FetchWeatherTask().execute(params);
            locationManager.removeUpdates(locationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED &&
                    grantResults[1] == PackageManager.PERMISSION_DENIED) {
                layout_main.setVisibility(GONE);
                layout_sub.setVisibility(GONE);
                tv_permission_error.setVisibility(View.VISIBLE);
            } else {
                try {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 从指定的API获取天气数据并填充页面的异步类
     */
    class FetchWeatherTask extends AsyncTask<String, Void, String> {
        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            /**
             * 将获取到的JSON解析出来并填充到主页面上
             */
            final String SHOWAPI_RES_BODY = "showapi_res_body";
            final String CITY_INFO = "cityInfo";
            final String NOW = "now";

            try {
                JSONObject weatherDataJSON = new JSONObject(s);
                JSONObject cityInfo = weatherDataJSON.getJSONObject(SHOWAPI_RES_BODY).getJSONObject(CITY_INFO);
                JSONObject now = weatherDataJSON.getJSONObject(SHOWAPI_RES_BODY).getJSONObject(NOW);
                JSONObject body = weatherDataJSON.getJSONObject(SHOWAPI_RES_BODY);

                /**
                 * 填充现在天气内容
                 */
                Log.v(LOG_TAG, cityInfo.getString("c3"));
                ((TextView) findViewById(R.id.tv_city_name)).setText(cityInfo.getString("c3"));
                ((TextView) findViewById(R.id.tv_temperature_now)).setText(now.getString("temperature") + "℃");
                ((TextView) findViewById(R.id.tv_weather_now)).setText(now.getString("weather"));
                ((TextView) findViewById(R.id.tv_sd_now)).setText("湿度：" + now.getString("sd"));
                ((TextView) findViewById(R.id.tv_wind_direction_now)).setText(now.getString("wind_direction"));
                ((TextView) findViewById(R.id.tv_wind_power_now)).setText(now.getString("wind_power"));

                String weatherPicNow = now.getString("weather_pic");
                new ImageGetTask().execute(new String[]{weatherPicNow, Integer.toString(R.id.weather_pic_now)});

                /**
                 * 填充未来5天天气内容
                 */
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
                int currentWeek = calendar.get(Calendar.DAY_OF_WEEK);
                String[] weekString = new String[]{
                        "星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"
                };

                ((TextView) findViewById(R.id.tv_week1)).setText(weekString[(currentWeek + 1) % 7]);
                ((TextView) findViewById(R.id.tv_week2)).setText(weekString[(currentWeek + 2) % 7]);
                ((TextView) findViewById(R.id.tv_week3)).setText(weekString[(currentWeek + 3) % 7]);
                ((TextView) findViewById(R.id.tv_week4)).setText(weekString[(currentWeek + 4) % 7]);
                ((TextView) findViewById(R.id.tv_week5)).setText(weekString[(currentWeek + 5) % 7]);

                ((TextView) findViewById(R.id.tv_temperature_week1))
                        .setText(body.getJSONObject("f1").getString("day_air_temperature") + "℃");
                ((TextView) findViewById(R.id.tv_temperature_week2))
                        .setText(body.getJSONObject("f2").getString("day_air_temperature") + "℃");
                ((TextView) findViewById(R.id.tv_temperature_week3))
                        .setText(body.getJSONObject("f3").getString("day_air_temperature") + "℃");
                ((TextView) findViewById(R.id.tv_temperature_week4))
                        .setText(body.getJSONObject("f4").getString("day_air_temperature") + "℃");
                ((TextView) findViewById(R.id.tv_temperature_week5))
                        .setText(body.getJSONObject("f5").getString("day_air_temperature") + "℃");

                String imagePic;
                imagePic = body.getJSONObject("f1").getString("day_weather_pic");
                new ImageGetTask().execute(new String[]{imagePic, Integer.toString(R.id.weather_pic_week1)});
                imagePic = body.getJSONObject("f2").getString("day_weather_pic");
                new ImageGetTask().execute(new String[]{imagePic, Integer.toString(R.id.weather_pic_week2)});
                imagePic = body.getJSONObject("f3").getString("day_weather_pic");
                new ImageGetTask().execute(new String[]{imagePic, Integer.toString(R.id.weather_pic_week3)});
                imagePic = body.getJSONObject("f4").getString("day_weather_pic");
                new ImageGetTask().execute(new String[]{imagePic, Integer.toString(R.id.weather_pic_week4)});
                imagePic = body.getJSONObject("f5").getString("day_weather_pic");
                new ImageGetTask().execute(new String[]{imagePic, Integer.toString(R.id.weather_pic_week5)});


            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                layout_loading.setVisibility(GONE);
                layout_main.setVisibility(View.VISIBLE);
                layout_sub.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected String doInBackground(String... params) {
            if (params.length == 0) {
                return null;
            }

            String jsonStr = null;

            try {
                final String API_BASE_URL = params[0];
                final String API_FEATURE = params[1];
                final String API_APPID = params[2];
                final String API_SIGN = params[3];

                final String API_5_LNG = params[4];
                final String API_5_LAT = params[5];

                /**
                 * 使用API网站给我们的SDK
                 */
                jsonStr = new ShowApiRequest(API_BASE_URL + API_FEATURE,
                        API_APPID, API_SIGN)
                        .addTextPara("from", "1")
                        .addTextPara("lng", API_5_LNG)
                        .addTextPara("lat", API_5_LAT)
                        .addTextPara("needMoreDay", "1").post();

                Log.v(LOG_TAG, jsonStr);

            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return jsonStr;
        }
    }

    class ImageGetTask extends AsyncTask<String, Void, Bitmap> {

        private int RES_ID;

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            ((ImageView) findViewById(RES_ID)).setImageBitmap(bitmap);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            String url = params[0];
            RES_ID = Integer.parseInt(params[1]);
            try {
                URL imageUrl = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
                conn.connect();
                InputStream is = conn.getInputStream();
                return BitmapFactory.decodeStream(is);

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
