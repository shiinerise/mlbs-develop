package com.chenxi.hust_project.sprint01;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

public class ProbeHealthStatusService {

    private static String URL = "http://data.whmlbs.com:5002/api/v1/timeStamp/probe";
    private static int TIMECON = 60000;
    private static long INTERVALTHRESHOLD = TIMECON * 2;

    /**
     * 探针健康状况
     * @param timeStampArray 时间戳数据，从接口中获取
     * @param sendInterval sendInterval = 5 （当前系统心跳的发送间隔为 5）
     * @return 健康状态或者健康程度
     */
    public String getProbeHealthStatus(String[] timeStampArray, int sendInterval) {
        int n = timeStampArray.length;
        if(n < 5) return StatusUtils.TERRIBLE; // 接收的数据少于5次，返回TERRIBLE

        Long[] timeStamps = new Long[timeStampArray.length];
        for(int i=0;i<timeStampArray.length;i++) {
            timeStamps[i] = getTimeStamp(timeStampArray[i]);
        }

        int badCount = 0; // 差值大于5分钟的次数

        List<Long> intervalBad = new ArrayList<>(); // 差值大于五分钟的，具体的差值
        int terribleCount = 0; // 差值大于7分钟的，具体的差值

        for(int i=0;i<timeStamps.length-1;i++) {
            Long interval = timeStamps[i] - timeStamps[i+1];
            if(interval > sendInterval * TIMECON) {
                badCount++;
                intervalBad.add(interval);
            }
        }

        if(badCount == 0) return StatusUtils.EXCELLENT; // 所有相邻两个时间戳的差值都小于等于 5 分钟，返回EXCELLENT
        else if(badCount <= 3) {
            for(Long k: intervalBad) {
                if(k > (INTERVALTHRESHOLD + sendInterval * TIMECON)) terribleCount++;
            }

            if(terribleCount == 0) return StatusUtils.OK; // 差值大于5分钟的最多只有3次并且都在7分钟之内，返回OK
            else if(terribleCount > 0) return StatusUtils.BAD; // 差值大于5分钟的最多只有3次，有超过7分钟的，返回BAD
        }
        else return StatusUtils.TERRIBLE; // 差值大于5分钟的超过3次，返回TERRIBLE
        return null;
    }

    /**
     * String类型转化为时间戳
     * @param time String类型的时间
     * @return Long 时间戳
     */
    public static Long getTimeStamp(String time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        try {
            date = format.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Long timestamp = date.getTime();
        return timestamp;
    }

    public static String loadJson(String url) {
        StringBuffer json = new StringBuffer();
        try {
            URL urlObject = new URL(url);
            URLConnection urlConnection = urlObject.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String inputLine = null;
            while ((inputLine = reader.readLine()) != null) {
                json.append(inputLine);
            }
            reader.close();
        }catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return json.toString();
    }

    // 传统的json解析
    public static List<Probe> getProbeList() {
        String json = loadJson(URL);
        JSONObject jsonObject = JSONObject.fromObject(json);
        JSONArray dataArray = jsonObject.getJSONArray("data");
        List<Probe> probeList = new ArrayList<>();
        for(int i=0;i<dataArray.size();i++) {
            JSONObject jsonObject1 = dataArray.getJSONObject(i);
            String key = jsonObject1.getString("key");
            JSONArray timeStampArray = jsonObject1.getJSONArray("time_stamp_array");
            String[] s = new String[timeStampArray.size()];
            for (int j = 0; j < timeStampArray.size(); j++) {
                s[j] = timeStampArray.getString(j);
            }
            Probe probe = new Probe(key, s);
            probeList.add(probe);
        }
        return probeList;
    }

    public static void main(String[] args) {
        ProbeHealthStatusService probeHealthStatusService = new ProbeHealthStatusService();
        List<Probe> probes = getProbeList();
        for(Probe probe: probes) {
            String probeID = probe.getKey();
            String state = probeHealthStatusService.getProbeHealthStatus(probe.getTime_stamp_array(), 5);
            if(state == StatusUtils.BAD) {
                System.out.println(probeID+" "+StatusUtils.BAD);
                for(String s: probe.getTime_stamp_array()) {
                    System.out.print(s+" ");
                }
                System.out.println();
            }
            if(state == StatusUtils.TERRIBLE) {
                System.out.println(probeID+" "+StatusUtils.TERRIBLE);
                for(String s: probe.getTime_stamp_array()) {
                    System.out.print(s+" ");
                }
                System.out.println();
            }
        }
    }
}
