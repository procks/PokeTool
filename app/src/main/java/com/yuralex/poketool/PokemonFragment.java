package com.yuralex.poketool;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.omkarmoghe.pokemap.controllers.net.NianticManager;
import com.omkarmoghe.pokemap.views.LoginActivity;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import POGOProtos.Networking.Responses.NicknamePokemonResponseOuterClass;
import POGOProtos.Networking.Responses.ReleasePokemonResponseOuterClass;

public class PokemonFragment extends Fragment implements MainActivity.Updatable {
    private static final String TAG = PokemonFragment.class.getSimpleName();

    private static final int SORT_CP = 0;
    private static final int SORT_IV = 1;
    private static final int SORT_TYPE_CP = 2;
    private static final int SORT_TYPE_IV = 3;
    private static final int SORT_RECENT = 4;
    private Map<Integer, PokemonImg> mPokemonImages;
    private PokemonGo mGo;
    private List<Pokemon> mPokemons;
    private int mSort;

    private FragmentActivity mActivity;
    private StableArrayAdapter mGridAdapter;
    private GridView mGridView;
    private ProgressDialog mProgress;

    public PokemonFragment() {
    }

    public static PokemonFragment newInstance() {
        return new PokemonFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        if (mActivity == null)
            return;
        setHasOptionsMenu(true);

        DaoPokemon daoPokemon = new DaoPokemon(mActivity);
        mPokemonImages = daoPokemon.getAllPokemon();
        NianticManager nianticManager = NianticManager.getInstance();
        mGo = nianticManager.getPokemonGo();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_pokemon, container, false);

        AdView mAdView = (AdView) rootView.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        if (mAdView != null) {
            mAdView.loadAd(adRequest);
        }
        mGridView = (GridView) rootView.findViewById(R.id.gridView);
        mGridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
        mGridView.setMultiChoiceModeListener(new MultiChoiceModeListener());

        Spinner spinner = (Spinner) rootView.findViewById(R.id.spinner);
        if (spinner != null) {
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    mSort = position;
                    Collections.sort(mPokemons, getPokemonComparator());
                    mGridAdapter.notifyDataSetChanged();
                    mGridView.invalidateViews();
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
        setPokemons();
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_pokemons, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_rename_by_iv:
                renameByIv();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private static float pokemonIv(Pokemon p) {
        return (p.getIndividualAttack()
                + p.getIndividualDefense()
                + p.getIndividualStamina()) * 100 / 45f;
    }

    private static class ComparatorIv implements Comparator<Pokemon> {
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

    private class ComparatorRecent implements Comparator<Pokemon> {
        public int compare(Pokemon p1, Pokemon p2) {
            return (int) (p2.getCreationTimeMs() - p1.getCreationTimeMs());
        }
    }

    private Comparator<Pokemon> getPokemonComparator() {
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
            case SORT_RECENT:
                comparator = new ComparatorRecent();
                break;
            default:
                comparator = new ComparatorCp();
        }
        return comparator;
    }

    public void update() {
        setPokemons();
    }

    private void setPokemons() {
        try {
            if (mGo != null) {
                mPokemons = mGo.getInventories().getPokebank().getPokemons();
            }
        } catch (LoginFailedException | RemoteServerException e) {
            e.printStackTrace();
        }
        updateList();
    }

    private void updateList() {
        if (mPokemons == null) {
            startActivity(new Intent(mActivity, LoginActivity.class));
            mActivity.finish();
            return;
        }

        Collections.sort(mPokemons, getPokemonComparator());

        mGridAdapter = new StableArrayAdapter(mActivity,
                android.R.layout.simple_list_item_1, mPokemons);
        if (mGridView != null) {
            mGridView.setAdapter(mGridAdapter);
        } else {
            Log.e(TAG, "gridView == null");
        }
    }

    private class StableArrayAdapter extends ArrayAdapter<Pokemon> {
        private final Context mmContext;
        List<Pokemon> mmPokemons;
        private HashMap<Integer, Boolean> mSelection = new HashMap<>();

        public StableArrayAdapter(Context context, int textViewResourceId, List<Pokemon> pokemons) {
            super(context, textViewResourceId, pokemons);
            mmContext = context;
            mmPokemons = pokemons;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) mmContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            @SuppressLint("ViewHolder")
            View rowView = inflater.inflate(R.layout.pokemon_list_item, parent, false);
            TextView firstLine = (TextView) rowView.findViewById(R.id.firstLine);
            TextView secondLine = (TextView) rowView.findViewById(R.id.secondLine);
            TextView thirdLine = (TextView) rowView.findViewById(R.id.thirdLine);
            TextView fourthLine = (TextView) rowView.findViewById(R.id.fourthLine);
            ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
            Pokemon p = mmPokemons.get(position);
            if (p != null) {
                String name = p.getNickname();
                if ("".equals(name) || name == null) {
                    name = properCase(p.getPokemonId().name());
                }
                firstLine.setText(name);
                secondLine.setText(String.format(Locale.ROOT, "IV%.2f%%", p.getIvRatio() * 100f));
                thirdLine.setText(String.format(Locale.ROOT, "CP%d lv%.1f", p.getCp(), p.getLevel()));
                fourthLine.setText(String.format(Locale.ROOT, "%d/%d/%d",
                        p.getIndividualAttack(), p.getIndividualDefense(), p.getIndividualStamina()));
                imageView.setImageResource(mPokemonImages.get(p.getPokemonId().getNumber()).getImagem());
            }

            rowView.setBackgroundColor(Color.TRANSPARENT); //default color
            if (mSelection.get(position) != null) {
                rowView.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.holo_blue_light));// this is a selected position so make it red
            }
            return rowView;
        }

        public void setNewSelection(int position, boolean value) {
            mSelection.put(position, value);
            notifyDataSetChanged();
        }

        public Set<Integer> getCurrentCheckedPosition() {
            return mSelection.keySet();
        }

        public void removeSelection(int position) {
            mSelection.remove(position);
            notifyDataSetChanged();
        }

        public void clearSelection() {
            mSelection = new HashMap<>();
            notifyDataSetChanged();
        }
    }

    private void transferSelectedItems() {
        mProgress = ProgressDialog.show(mActivity, mActivity.getString(R.string.transfer_title),
                mActivity.getString(R.string.please_waite), true);
        new TransferAsyncTask().execute();
    }

    private void renameByIv() {
        mProgress = ProgressDialog.show(mActivity, mActivity.getString(R.string.rename_title),
                mActivity.getString(R.string.please_waite), true);
        new RenameAsyncTask().execute();
    }

    private class TransferAsyncTask extends AsyncTask<Void, Void, Integer> {
        @Override
        protected Integer doInBackground(Void... params) {
            int count = 0;
            for (int position : mGridAdapter.getCurrentCheckedPosition()) {
                Log.e(TAG, "delete " + position);
                Pokemon pokemon = mPokemons.get(position);
                if (pokemon != null) {
                    try {
                        ReleasePokemonResponseOuterClass.ReleasePokemonResponse.Result result = pokemon.transferPokemon();
                        if (result == ReleasePokemonResponseOuterClass.ReleasePokemonResponse.Result.SUCCESS) count++;
                        Log.i(TAG, "Transfered result:" + result);
                    } catch (LoginFailedException | RemoteServerException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
            try {
                mPokemons = mGo.getInventories().getPokebank().getPokemons();
            } catch (LoginFailedException | RemoteServerException e) {
                e.printStackTrace();
            }
            return count;
        }

        @Override
        protected void onPostExecute(Integer result) {
            mProgress.dismiss();
//            if (result > 0)
            updateList();
        }
    }

    String properCase (String inputVal) {
        // Empty strings should be returned as-is.

        if (inputVal.length() == 0) return "";

        // Strings with only one character uppercased.

        if (inputVal.length() == 1) return inputVal.toUpperCase();

        // Otherwise uppercase first letter, lowercase the rest.

        return inputVal.substring(0,1).toUpperCase()
                + inputVal.substring(1).toLowerCase();
    }

    private class RenameAsyncTask extends AsyncTask<Void, Void, Integer> {
        @Override
        protected Integer doInBackground(Void... params) {
            int count = 0;
            for (Pokemon pokemon : mPokemons) {
//                Log.e(TAG, "rename " + position);
                if (pokemon != null) {
                    try {
                        String name = String.format(Locale.ROOT, "%s IV%d", properCase(pokemon.getPokemonId().name()),
                                (int) (pokemon.getIvRatio() * 100));

                        NicknamePokemonResponseOuterClass.NicknamePokemonResponse.Result result = pokemon.renamePokemon(name);
                        if (result == NicknamePokemonResponseOuterClass.NicknamePokemonResponse.Result.SUCCESS) count++;
                        Log.i(TAG, "Rename result:" + result);
                    } catch (LoginFailedException | RemoteServerException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
            try {
                mPokemons = mGo.getInventories().getPokebank().getPokemons();
            } catch (LoginFailedException | RemoteServerException e) {
                e.printStackTrace();
            }
            return count;
        }

        @Override
        protected void onPostExecute(Integer result) {
            mProgress.dismiss();
//            if (result > 0)
            updateList();
        }
    }

    private class MultiChoiceModeListener implements AbsListView.MultiChoiceModeListener {
        private int nr = 0;

        @Override
        public void onItemCheckedStateChanged(android.view.ActionMode mode, int position,
                                              long id, boolean checked) {
            // Here you can do something when items are selected/de-selected,
            // such as update the title in the CAB
            if (checked) {
                nr++;
                mGridAdapter.setNewSelection(position, true); //checked
            } else {
                nr--;
                mGridAdapter.removeSelection(position);
            }
            mode.setTitle(nr + " selected");
        }

        @Override
        public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {
            // Respond to clicks on the actions in the CAB
            switch (item.getItemId()) {
                case R.id.menu_transfer:
                    transferSelectedItems();
                    nr = 0;
                    mGridAdapter.clearSelection();
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean onCreateActionMode(android.view.ActionMode mode, Menu menu) {
            // Inflate the menu for the CAB
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_pokemons_context, menu);
            return true;
        }

        @Override
        public void onDestroyActionMode(android.view.ActionMode mode) {
            // Here you can make any necessary updates to the activity when
            // the CAB is removed. By default, selected items are deselected/unchecked.
            mGridAdapter.clearSelection();
        }

        @Override
        public boolean onPrepareActionMode(android.view.ActionMode mode, Menu menu) {
            // Here you can perform updates to the CAB due to
            // an invalidate() request
            nr = 0;
            return false;
        }
    }
}
