package at.schuschu.android.rssilogger;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class LoggerReplay extends Activity {

    ArrayAdapter<String> adapter;
    SimpleAdapter simpleadapter;
    HashMap<String,ArrayList<HashMap<String, String>>> backlog;
    ArrayList<String> timelist;
    ArrayList<HashMap<String, String>> arraylist = new ArrayList<HashMap<String, String>>();
    ListView timeview,listview;
    Listy listy;


    class Listy implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {

            String item = ((TextView)view).getText().toString();
            ArrayList<HashMap<String, String>> list = backlog.get(item);
            arraylist.clear();
            arraylist.addAll(list);
            simpleadapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logger_replay);
        backlog = (HashMap<String,ArrayList<HashMap<String, String>>>) getIntent().getSerializableExtra("BACKLOG");
        timelist = new ArrayList<String>();
        listy=new Listy();

        if(backlog==null) {
            Log.e("Replay", "Nothing passed to this activity!");
            return;
        }

        timeview = (ListView)findViewById(R.id.lv_timestamp);
        listview = (ListView)findViewById(R.id.lv_replay);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,timelist);
        simpleadapter = new SimpleAdapter(this, arraylist, R.layout.listview_row, new String[] { LoggerMain.SSID_KEY,LoggerMain.LEVEL_KEY,LoggerMain.BSSID_KEY }, new int[] { R.id.tv_row_ssid , R.id.tv_row_level, R.id.tv_row_bssid});

        timeview.setAdapter(adapter);
        listview.setAdapter(simpleadapter);

        for(String timestamp : backlog.keySet())
        {
            timelist.add(timestamp);
        }

        adapter.notifyDataSetChanged();

        timeview.setOnItemClickListener(listy);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.logger_replay, menu);
        return true;
    }

    public void oblivion (View view){
        File file = new File(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/rssilogger.json");
        file.delete();
        backlog=new HashMap<String,ArrayList<HashMap<String, String>>>();
        arraylist.clear();
        timelist.clear();
        simpleadapter.notifyDataSetChanged();
        adapter.notifyDataSetChanged();
        LoggerMain.backlog=this.backlog;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
