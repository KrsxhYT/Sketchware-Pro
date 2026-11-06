package com.besome.sketch.editor.logic;

import static com.besome.sketch.editor.logic.PaletteSelector.paletteSelectorRecord;
import static com.google.android.material.color.MaterialColors.harmonizeWithPrimary;
import static com.google.android.material.color.MaterialColors.isColorLight;
import static pro.sketchware.utility.ThemeUtils.getColor;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import a.a.a.Vs;
import a.a.a.wB;
import pro.sketchware.R;
import pro.sketchware.databinding.PaletteSelectorItemBinding;

public class PaletteSelectorAdapter extends RecyclerView.Adapter<PaletteSelectorAdapter.PaletteSelectorViewHolder> {

    private static final String PREFS = "krsxh_recent_block_categories";
    private static final String KEY_RECENT = "recent_ids";
    private static final int MAX_RECENT = 5;

    private final PaletteSelector paletteSelector;
    private final List<paletteSelectorRecord> paletteList = new ArrayList<>();
    private final Context context;
    private final Vs onBlockCategorySelectListener;
    private final SharedPreferences recentPrefs;
    private int selectedPosition = -1;

    public PaletteSelectorAdapter(PaletteSelector paletteSelector, Vs onBlockCategorySelectListener) {
        this.paletteSelector = paletteSelector;
        context = paletteSelector.getContext();
        this.onBlockCategorySelectListener = onBlockCategorySelectListener;
        recentPrefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void setPalettes(List<paletteSelectorRecord> list) {
        paletteList.clear();
        List<paletteSelectorRecord> filtered = new ArrayList<>();
        for (paletteSelectorRecord palette : list) {
            if (paletteSelector.matchesSearch(palette.text())) {
                filtered.add(palette);
            }
        }

        // Part 2: put recent categories first
        List<Integer> recent = loadRecentIds();
        if (!recent.isEmpty()) {
            List<paletteSelectorRecord> ordered = new ArrayList<>();
            Set<Integer> added = new HashSet<>();
            for (int id : recent) {
                for (paletteSelectorRecord p : filtered) {
                    if (p.index() == id && added.add(id)) {
                        ordered.add(p);
                        break;
                    }
                }
            }
            for (paletteSelectorRecord p : filtered) {
                if (added.add(p.index())) {
                    ordered.add(p);
                }
            }
            paletteList.addAll(ordered);
        } else {
            paletteList.addAll(filtered);
        }

        if (!paletteList.isEmpty() && selectedPosition < 0) {
            selectedPosition = 0;
        }
        notifyDataSetChanged();
    }

    private List<Integer> loadRecentIds() {
        String raw = recentPrefs.getString(KEY_RECENT, "");
        if (raw == null || raw.isEmpty()) return Collections.emptyList();
        List<Integer> out = new ArrayList<>();
        for (String part : raw.split(",")) {
            try {
                if (!part.isEmpty()) out.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    private void saveRecentId(int id) {
        List<Integer> recent = new ArrayList<>(loadRecentIds());
        recent.remove(Integer.valueOf(id));
        recent.add(0, id);
        while (recent.size() > MAX_RECENT) {
            recent.remove(recent.size() - 1);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < recent.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(recent.get(i));
        }
        recentPrefs.edit().putString(KEY_RECENT, sb.toString()).apply();
    }

    @NonNull
    @Override
    public PaletteSelectorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        PaletteSelectorItemBinding binding = PaletteSelectorItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PaletteSelectorViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PaletteSelectorViewHolder holder, int position) {
        paletteSelectorRecord item = paletteList.get(position);
        int id = item.index();
        String title = item.text();
        int color = harmonizeWithPrimary(context, item.color());

        holder.binding.tvCategory.setText(title);
        holder.binding.bg.setBackgroundColor(color);
        holder.binding.tvCategory.setTextColor(
                position == selectedPosition ?
                        isColorLight(color) ? getColor(context, R.attr.colorOnSurface) : getColor(context, R.attr.colorOnSurfaceInverse)
                        : getColor(context, R.attr.colorOnSurface));
        holder.binding.bg.getLayoutParams().width = position == selectedPosition ? ViewGroup.LayoutParams.MATCH_PARENT : (int) wB.a(context, 4f);

        holder.itemView.setOnClickListener(v -> {
            selectedPosition = holder.getAbsoluteAdapterPosition();
            saveRecentId(id);
            notifyDataSetChanged();
            if (onBlockCategorySelectListener != null) {
                onBlockCategorySelectListener.a(id, color);
            }
        });
    }

    @Override
    public int getItemCount() {
        return paletteList.size();
    }

    public void selectPaletteById(int tag) {
        for (int i = 0; i < paletteList.size(); i++) {
            int paletteId = paletteList.get(i).index();
            if (paletteId == tag) {
                selectedPosition = i;
                saveRecentId(paletteId);
                notifyDataSetChanged();
                if (onBlockCategorySelectListener != null) {
                    onBlockCategorySelectListener.a(paletteId, paletteList.get(i).color());
                }
                break;
            }
        }
    }

    public void selectPosition(int pos) {
        if (pos >= 0 && pos < paletteList.size()) {
            selectedPosition = pos;
            saveRecentId(paletteList.get(pos).index());
            notifyDataSetChanged();
            if (onBlockCategorySelectListener != null) {
                onBlockCategorySelectListener.a(paletteList.get(pos).index(), paletteList.get(pos).color());
            }
        }
    }

    public static class PaletteSelectorViewHolder extends RecyclerView.ViewHolder {
        final PaletteSelectorItemBinding binding;

        public PaletteSelectorViewHolder(PaletteSelectorItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
