package com.feer.windcast;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A fragment representing a list of Items.
 * <p />
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p />
 * Activities containing this fragment MUST implement the callbacks
 * interface.
 */
public class WeatherStationFragment extends Fragment implements AbsListView.OnItemClickListener {

    private static final String TAG = "WeatherStationFragment" ;
    private OnWeatherStationFragmentInteractionListener mListener;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ArrayAdapter<WeatherStation> mAdapter;


    WeatherDataCache mCache;

    private EditText mSearchInput;

    private static final String STATION_STATE = "weatherStationState";

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public WeatherStationFragment() {
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void ShowOnlyStationsInState(String state)
    {
        //TODO add the state to the args, also implement reading from the args
        if(mAdapter != null)
        {
            SetListedStations(mCache.GetWeatherStationsFrom(state));
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void ShowAllStations()
    {
        if(mAdapter != null)
        {
            SetListedStations(mCache.GetWeatherStationsFromAllStates());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new ArrayAdapter<WeatherStation>(
                getActivity(),
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                new ArrayList<WeatherStation>());

        new AsyncTask<Void, Void, ArrayList<WeatherStation>>()
        {

            @Override
            protected ArrayList<WeatherStation> doInBackground(Void... params)
            {
                return mCache.GetWeatherStationsFromAllStates();
            }

            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
            @Override
            protected void onPostExecute(ArrayList<WeatherStation> cacheStations)
            {
                mAdapter.clear();
                mAdapter.addAll(cacheStations);
                mAdapter.notifyDataSetChanged();

                Log.i(TAG, "Finished adding new stations.");
            }
        }.execute();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weatherstation, container, false);

        AbsListView listView = (AbsListView) view.findViewById(android.R.id.list);
        listView.setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        listView.setOnItemClickListener(this);
        InitializeSearchBox(view);

        return view;
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();

        mSearchInput = null;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnWeatherStationFragmentInteractionListener) activity;


            mCache = new WeatherDataCache(activity.getResources());
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                + " must implement OnWeatherStationFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        WeatherStation station = mAdapter.getItem(position);
        WeatherStationSelected(station);
    }

    private void WeatherStationSelected(WeatherStation station)
    {
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            mListener.onWeatherStationSelected(station);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void SetListedStations(Collection<WeatherStation> stations)
    {
        mAdapter.clear();
        mAdapter.addAll(stations);

        // this has to be done to refresh the filter on mAdapter.
        // Otherwise the previously set filter will persist, and the list of objects returned by
        // the adapter will not change!
        if(mSearchInput != null) mSearchInput.setText("");
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Initializes the search box for filtering the list of weather stations
     */
    private void InitializeSearchBox(View view)
    {
        mSearchInput = (EditText)view.findViewById(R.id.weather_station_search_box);

        mSearchInput.addTextChangedListener(
                new TextWatcher()
                {
                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3)
                    {
                        mAdapter.getFilter().filter(charSequence);
                    }

                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3)
                    {
                    }

                    @Override
                    public void afterTextChanged(Editable editable)
                    {
                    }
                }
                                            );
    }

    /**
    * Interface to allow actions to occur outside of this
    * fragment, when the user interacts with this fragment
    */
    public interface OnWeatherStationFragmentInteractionListener
    {

        public void onWeatherStationSelected(WeatherStation station);
    }

}
