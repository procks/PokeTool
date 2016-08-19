package com.yuralex.poketool;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.omkarmoghe.pokemap.controllers.net.NianticManager;
import com.omkarmoghe.pokemap.views.LoginActivity;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.inventory.Inventories;
import com.pokegoapi.api.inventory.Pokedex;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.api.pokemon.PokemonMeta;
import com.pokegoapi.api.pokemon.PokemonMetaRegistry;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import java.util.Locale;
import java.util.Map;

import POGOProtos.Enums.PokemonFamilyIdOuterClass;
import POGOProtos.Enums.PokemonIdOuterClass;

public class PokedexFragment extends Fragment implements MainActivity.Updatable {
    private static final String TAG = PokedexFragment.class.getSimpleName();
    private Map<Integer, PokemonImg> mPokemonImages;
    private FragmentActivity mActivity;
    private StableArrayAdapter mGridAdapter;
    private GridView mGridView;
    private PokemonGo mGo;
    private Inventories mInventories;

    public PokedexFragment() {
        // Required empty public constructor
    }

    public static PokedexFragment newInstance() {
        return new PokedexFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        if (mActivity == null)
            return;

        DaoPokemon daoPokemon = new DaoPokemon(mActivity);
        mPokemonImages = daoPokemon.getAllPokemon();

        NianticManager nianticManager = NianticManager.getInstance();
        mGo = nianticManager.getPokemonGo();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_pokedex, container, false);

        AdView mAdView = (AdView) rootView.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        if (mAdView != null) {
            mAdView.loadAd(adRequest);
        }
        mGridView = (GridView) rootView.findViewById(R.id.gridView);
        setInventories();
        return rootView;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void updateList() {
        if (mInventories == null) {
            startActivity(new Intent(mActivity, LoginActivity.class));
            mActivity.finish();
            return;
        }

        mGridAdapter = new StableArrayAdapter(mActivity,
                android.R.layout.simple_list_item_1, mInventories.getPokedex());
        if (mGridView != null) {
            mGridView.setAdapter(mGridAdapter);
        } else {
            Log.e(TAG, "gridView == null");
        }
    }

    public void setInventories() {
        try {
            if (mGo != null) {
                mInventories = mGo.getInventories();
            }
        } catch (LoginFailedException | RemoteServerException e) {
            e.printStackTrace();
        }
        updateList();
    }

    @Override
    public void update() {
        setInventories();
    }

    private class StableArrayAdapter extends ArrayAdapter<Pokemon> {
        private final Context mmContext;
        Pokedex mmPokedex;

        public StableArrayAdapter(Context context, int textViewResourceId, Pokedex pokedex) {
            super(context, textViewResourceId);
            mmContext = context;
            mmPokedex = pokedex;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView;
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) mmContext
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = inflater.inflate(R.layout.pokemon_list_item, parent, false);

            } else {
                rowView = convertView;
            }
            position = position + 1;
            TextView firstLine = (TextView) rowView.findViewById(R.id.firstLine);
            TextView secondLine = (TextView) rowView.findViewById(R.id.secondLine);
            TextView thirdLine = (TextView) rowView.findViewById(R.id.thirdLine);
            TextView fourthLine = (TextView) rowView.findViewById(R.id.fourthLine);
            ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);

            PokemonIdOuterClass.PokemonId pokemonId = PokemonIdOuterClass.PokemonId.internalGetValueMap().findValueByNumber(position);
            if (pokemonId != null) {
                PokemonMeta meta = PokemonMetaRegistry.getMeta(pokemonId);
                PokemonFamilyIdOuterClass.PokemonFamilyId familyId = meta.getFamily();
                int pokemonsSize = mInventories.getPokebank().getPokemonByPokemonId(pokemonId).size();
                int candiesSize = mInventories.getCandyjar().getCandies(familyId);
                int candyToEvolve = meta.getCandyToEvolve();
                int evolutions = 0;
                if (candyToEvolve != 0) {
                    evolutions = candiesSize / candyToEvolve;
                }

                firstLine.setText(pokemonId.name());
                secondLine.setText(String.format(Locale.ROOT, "pokemons %d", pokemonsSize));
                if (pokemonsSize == 0) {
                    firstLine.setTextColor(ContextCompat.getColor(mmContext, android.R.color.holo_red_dark));
                    secondLine.setTextColor(ContextCompat.getColor(mmContext, android.R.color.holo_red_dark));
                } else {
                    firstLine.setTextColor(ContextCompat.getColor(mmContext, android.R.color.tertiary_text_light));
                    secondLine.setTextColor(ContextCompat.getColor(mmContext, android.R.color.tertiary_text_light));
                }
                thirdLine.setText(String.format(Locale.ROOT, "candies %d/%d", candiesSize, candyToEvolve));
                if (candiesSize >= candyToEvolve && candyToEvolve > 0) {
                    thirdLine.setTextColor(ContextCompat.getColor(mmContext, android.R.color.holo_green_dark));
                } else {
                    thirdLine.setTextColor(ContextCompat.getColor(mmContext, android.R.color.tertiary_text_light));
                }
                fourthLine.setText(String.format(Locale.ROOT, "E %d P %d", evolutions, pokemonsSize - evolutions));
                PokemonImg pokeImg = mPokemonImages.get(position);
                if (pokeImg != null) {
                    imageView.setImageResource(mPokemonImages.get(position).getImagem());
                }
            } else {
                secondLine.setText("");
                thirdLine.setText("");
                fourthLine.setText("");
                imageView.setImageResource(0);
            }

            return rowView;
        }

        @Override
        public int getCount() {
            return PokemonIdOuterClass.PokemonId.values().length - 2;
        }
    }
}
