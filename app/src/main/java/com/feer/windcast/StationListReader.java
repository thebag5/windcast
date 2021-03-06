package com.feer.windcast;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.feer.windcast.R.raw.*;
import static com.feer.windcast.WeatherStation.*;

/**
 * Reads weather stations embedded as links in html
 * */
public class StationListReader
{
    static final String TAG = "StationListReader";
    public static ArrayList<WeatherData> GetWeatherStationsFromURL(URL fromUrl, String state) throws Exception
    {
        BufferedReader buf;
        try
        {
            buf = new BufferedReader(
                    new InputStreamReader(
                            fromUrl.openStream()));
        } catch (IOException e)
        {
            Log.e(TAG, "Unable to get content from url: " + fromUrl.toString());
            throw new Exception("Cant get content", e);
        }

        ArrayList<WeatherData> weatherStations = new ArrayList<WeatherData>();

        /* because we have to escape the slashes and quotes in the regex this is hard to read.

           to break this down:
           we are trying to match a line like one of the following (in html)
           <th id="tKIM-station-broome-port" class="rowleftcolumn"><a href="/products/IDW60801/IDW60801.95202.shtml">Broome Port</a></th>
           <th id="obs-station-braidwood" class="rowleftcolumn"><a href="/products/IDN60903/IDN60903.94927.shtml">Braidwood</a></th>

           we want two groups:
                - one for the url (group 1) 
                - one for the full station name (group 2)
                
           Group 1: (.*shtml)
           this is the relative link to the station page.
            - It comes after <a href="
            - It ends with 'shtml'

           Group 2: >(.*)</a>
           is all characters inside the anchor tag
        */
        Pattern idUrlAndNamePattern = Pattern.compile("<th id=\".*\" class.*<a href=\"(.*shtml)\">(.*)</a>.*");
        String str;
        
        // get timezone from string eg: 
        // <th id="tKIM-datetime" rowspan="2">Date/Time<br /><acronym title="Western Standard Time">WST</acronym></th>
        Pattern obsTimeZone = Pattern.compile("<th id=\".*-datetime.*><acronym title=\"(.*)\">");
        Pattern obsDayAndTime = Pattern.compile("<td headers=\".*-datetime.*>(.*)</td>");
        Pattern obsWindDir = Pattern.compile("<td headers=\".*-wind.dir .*\">(.*)</td>");
        Pattern obsWindSpdKmh = Pattern.compile("<td headers=\".*-wind.spd.kmh.*\">(.*)</td>");
        Pattern obsWindSpdKts = Pattern.compile("<td headers=\".*-wind.spd.kts.*\">(.*)</td>"); // could be -wind-spd-kts or -wind-spd_kts
        Pattern endOfStationDetails = Pattern.compile("</tr>");
        
        
        WeatherData d = null;
        ObservationReading r = null;
        final String source = fromUrl.toString().replaceFirst("http://www.bom.gov.au/", "");
        String lastFoundTimeZone = null;
        while((str = buf.readLine()) != null)
        {
            Matcher m = obsTimeZone.matcher(str);
            if(m.find())
            {
                lastFoundTimeZone = "Australian " + m.group(1); // added since BOM time zone acronym's and title exclude the A (Australia) see  http://www.timeanddate.com/time/zones/au
                continue;
            }
            
            m = idUrlAndNamePattern.matcher(str);
            if(m.find())
            {
                d = new WeatherData();
                d.Source = source;
                r = new ObservationReading();
                r.Wind_Observation = new WindObservation();
                d.ObservationData = new ArrayList<ObservationReading>(1);
                d.ObservationData.add(r);

                d.Station = new WeatherStation.WeatherStationBuilder()
                        .WithState(States.valueOf(state))
                        .WithURL(new URL("http://www.bom.gov.au" + StationListReader.ConvertToJSONURL(m.group(1))))
                        .WithName(m.group(2))
                        .Build();

                continue;
            }
            
            if(d == null || r == null)
            {
                continue;
            }
            
            m = obsDayAndTime.matcher(str);
            if ( m.find())
            {
                // The date is in the form dd/hh:mm followed by am or pm.
                // We need to prefix it with year and month.
                SimpleDateFormat df = new SimpleDateFormat("yyyyMM", Locale.US);
                String dateTime = df.format(Calendar.getInstance().getTime()) + m.group(1) + ' ' +  lastFoundTimeZone;
            
                df = new SimpleDateFormat("yyyyMMdd/hh:mmaa zzzz",  Locale.US);
                try
                {
                    r.LocalTime = df.parse(dateTime);
                } catch (ParseException e)
                {
                    Log.e("WindCast", "unable to parse date string: " + dateTime);
                }
                continue;
            }

            m = obsWindDir.matcher(str);
            if ( m.find())
            {
                r.Wind_Observation.CardinalWindDirection = m.group(1).toLowerCase(Locale.US);
                r.Wind_Observation.WindBearing = ObservationReader.ConvertCardinalCharsToBearing(r.Wind_Observation.CardinalWindDirection);
                continue;
            }

            m = obsWindSpdKmh.matcher(str);
            if ( m.find() && !m.group(1).equals("-"))
            {
                r.Wind_Observation.WindSpeed_KMH = Integer.parseInt(m.group(1));
                continue;
            }

            m = obsWindSpdKts.matcher(str);
            if ( m.find() && !m.group(1).equals("-"))
            {
                r.Wind_Observation.WindSpeed_KN = Integer.parseInt(m.group(1));
                continue;
            }
            
            m = endOfStationDetails.matcher(str);
            if ( m.find() && d != null && d.Station != null)
            {
                weatherStations.add(d);
                d = null;
                r = null;
            }
        }

        buf.close();
        return weatherStations;
    }

    public static String ConvertToJSONURL(String urlString)
    {
        urlString = urlString.replaceAll("shtml", "json");
        urlString = urlString.replaceAll("products", "fwo");
        return urlString;
    }
}
