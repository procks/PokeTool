package com.yuralex.poketool;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.omkarmoghe.pokemap.controllers.app_preferences.PokemapAppPreferences;
import com.omkarmoghe.pokemap.controllers.app_preferences.PokemapSharedPreferences;
import com.omkarmoghe.pokemap.controllers.net.NianticManager;
import com.omkarmoghe.pokemap.views.LoginActivity;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int SORT_CP = 0;
    private static final int SORT_IV = 1;
    private static final int SORT_TYPE_CP = 2;
    private static final int SORT_TYPE_IV = 3;
    private static final int SORT_RESENT = 4;
    private Map<Integer, PokemonImg> mPokemonImages;
    private List<Pokemon> mPokemons;
    private int mSort;
    private PokemapAppPreferences mPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPref = new PokemapSharedPreferences(this);
        NianticManager nianticManager = NianticManager.getInstance();
        try {
            PokemonGo go = nianticManager.getPokemonGo();
            if (go != null) {
                mPokemons = go.getInventories().getPokebank().getPokemons();
            }
            updateList();

        } catch (LoginFailedException | RemoteServerException e) {
            e.printStackTrace();
        }
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        if (spinner != null) {
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    mSort = position;
                    updateList();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }
        DaoPokemon daoPokemon = new DaoPokemon(this);
        mPokemonImages = daoPokemon.getAllPokemon();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                logout();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        mPref.clearLoginCredentials();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private static float pokemonIv(Pokemon p) {
        return (p.getIndividualAttack()
                + p.getIndividualDefense()
                + p.getIndividualStamina()) * 100 / 45f;
    }

    private static class ComparatorIv implements Comparator<Pokemon>{
        public int compare(Pokemon p1, Pokemon p2) {
            return (int) (pokemonIv(p2) - pokemonIv(p1));
        }
    }

    private static class ComparatorCp implements Comparator<Pokemon>{
        public int compare(Pokemon p1, Pokemon p2) {
            return p2.getCp() - p1.getCp();
        }
    }

    private static class ComparatorTypeIv implements Comparator<Pokemon>{
        public int compare(Pokemon p1, Pokemon p2) {
            int compare = p1.getPokemonId().getNumber() - p2.getPokemonId().getNumber();
            return compare != 0 ? compare : (int) (pokemonIv(p2) - pokemonIv(p1));
        }
    }

    private static class ComparatorTypeCp implements Comparator<Pokemon>{
        public int compare(Pokemon p1, Pokemon p2) {
            int compare = p1.getPokemonId().getNumber() - p2.getPokemonId().getNumber();
            return compare != 0 ? compare : p2.getCp() - p1.getCp();
        }
    }

    private class ComparatorResent implements Comparator<Pokemon> {
        public int compare(Pokemon p1, Pokemon p2) {
            return (int) (p2.getCreationTimeMs() - p1.getCreationTimeMs());
        }
    }

    private void updateList() {
        List<Pokemon> pokemons = mPokemons;
        if (pokemons == null) return;
        Comparator<Pokemon> comparator;
        switch (mSort) {
            case SORT_CP:
                comparator = new ComparatorCp();
                break;
            case SORT_IV:
                comparator = new ComparatorIv();
                break;
            case SORT_TYPE_CP:
                comparator = new ComparatorTypeCp();
                break;
            case SORT_TYPE_IV:
                comparator = new ComparatorTypeIv();
                break;
            case SORT_RESENT:
                comparator = new ComparatorResent();
                break;
            default:
                comparator = new ComparatorCp();
        }
        Collections.sort(pokemons, comparator);

        final ListView listview = (ListView) findViewById(R.id.listview);
        final StableArrayAdapter adapter = new StableArrayAdapter(this,
                android.R.layout.simple_list_item_1, pokemons);
        if (listview != null) {
            listview.setAdapter(adapter);
        }
    }

    private class StableArrayAdapter extends ArrayAdapter<Pokemon> {
        private final Context mContext;
        List<Pokemon> mPokemons;

        public StableArrayAdapter(Context context, int textViewResourceId, List<Pokemon> pokemons) {
            super(context, textViewResourceId, pokemons);
            mContext = context;
            mPokemons = pokemons;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            @SuppressLint("ViewHolder")
            View rowView = inflater.inflate(R.layout.pokemon_list_item, parent, false);
            TextView firstLine = (TextView) rowView.findViewById(R.id.firstLine);
            TextView secondLine = (TextView) rowView.findViewById(R.id.secondLine);
            ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
            firstLine.setText(mPokemons.get(position).getPokemonId().name());
            Pokemon p = mPokemons.get(position);
            secondLine.setText(String.format(Locale.ROOT, "IV%.2f%% CP%d %d/%d/%d", pokemonIv(p), p.getCp(),
                    p.getIndividualAttack(), p.getIndividualDefense(), p.getIndividualStamina()));
            imageView.setImageResource(mPokemonImages.get(p.getPokemonId().getNumber()).getImagem());
            return rowView;
        }
    }
}
