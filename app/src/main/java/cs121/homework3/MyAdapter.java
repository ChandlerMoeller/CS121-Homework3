package cs121.homework3;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import cs121.homework3.GSON.ResultList;

/**
 * Modified code by shobhit on 1/24/16.
 * Which is Copied from Prof. Luca class code
 */
public class MyAdapter extends ArrayAdapter<ResultList> {

    int resource;
    int resource2;
    Context context;
    String client_userId;

    public MyAdapter(Context _context, int _resource, int _resource2, List<ResultList> items, String _client_userId) {
        super(_context, _resource, _resource2, items);
        resource2 = _resource2;
        resource = _resource;
        context = _context;
        client_userId = _client_userId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout newView;

        ResultList w = getItem(position);
        String compuserId = w.userId;

        // Inflate a new view always.
        newView = new LinearLayout(getContext());
        String inflater = Context.LAYOUT_INFLATER_SERVICE;
        LayoutInflater vi = (LayoutInflater) getContext().getSystemService(inflater);
        if (!client_userId.equals(compuserId)) {
            vi.inflate(resource, newView, true);
        } else {
            vi.inflate(resource2, newView, true);
        }

        // Fills in the view.
        TextView tv = (TextView) newView.findViewById(R.id.message_message);
        TextView b = (TextView) newView.findViewById(R.id.message_nickname);
        tv.setText(w.message);
        b.setText(w.nickname);

        // Sets a listener for the button, and a tag for the button as well.
        b.setTag(new Integer(position));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Reacts to a button press.
                // Gets the integer tag of the button.
/*                String s = v.getTag().toString();
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, s, duration);
                toast.show();*/
            }
        });

        // Set a listener for the whole list item.
        newView.setTag(R.string.one, w.userId);
        newView.setTag(R.string.two, w.nickname);
        newView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                if (settings.getBoolean("spoofmode_switch", true)) {
                    SharedPreferences.Editor e = settings.edit();
                    e.putString("spoofed_user_id", v.getTag(R.string.one).toString());
                    e.putString("spoofed_username", v.getTag(R.string.two).toString());
                    e.commit();

                    String s = v.getTag(R.string.one).toString();
                    String s2 = v.getTag(R.string.two).toString();
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(context, s2+" | "+s, duration);
                    toast.show();
                }
            }
        });

        return newView;
    }


}