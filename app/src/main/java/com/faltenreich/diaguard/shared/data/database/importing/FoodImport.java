package com.faltenreich.diaguard.shared.data.database.importing;

import android.content.Context;
import android.util.Log;

import com.faltenreich.diaguard.R;
import com.faltenreich.diaguard.feature.preference.data.PreferenceStore;
import com.faltenreich.diaguard.shared.data.database.dao.FoodDao;
import com.faltenreich.diaguard.shared.data.database.entity.Food;
import com.faltenreich.diaguard.shared.data.primitive.FloatUtils;
import com.opencsv.CSVReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

class FoodImport implements Importing {

    private static final String TAG = FoodImport.class.getSimpleName();
    private static final String FOOD_CSV_FILE_NAME = "seed/food.csv";

    private final Context context;
    private final Locale locale;

    FoodImport(Context context, Locale locale) {
        this.context = context;
        this.locale = locale;
    }

    @Override
    public boolean requiresImport() {
        return !PreferenceStore.getInstance().didImportCommonFood(locale);
    }

    @Override
    public void importData() {
        try {
            CSVReader reader = CsvImport.getCsvReader(context, FOOD_CSV_FILE_NAME);

            String languageCode = locale.getLanguage();
            String[] nextLine = reader.readNext();
            int languageRow = CsvImport.getLanguageColumn(languageCode, nextLine);
            if (languageRow == 0) {
                languageCode = "zh";
            }

            List<Food> foodList = new ArrayList<>();
            while ((nextLine = reader.readNext()) != null) {

                if (nextLine.length >= 5) {
                    Food food = new Food();

                    food.setName(nextLine[languageRow]);
                    food.setIngredients(food.getName());
                    food.setLabels(context.getString(R.string.food_common));
                    food.setLanguageCode(languageCode);

                    int index = 2;
                    // Main nutrients are given in grams, so we take them as they are
                    food.setCarbohydrates(FloatUtils.parseNullableNumber(nextLine[index]));
                    food.setEnergy(FloatUtils.parseNullableNumber(nextLine[index+1]));
                    food.setFat(FloatUtils.parseNullableNumber(nextLine[index+2]));
                    food.setFatSaturated(FloatUtils.parseNullableNumber(nextLine[index+3]));
                    food.setFiber(FloatUtils.parseNullableNumber(nextLine[index+4]));
                    food.setProteins(FloatUtils.parseNullableNumber(nextLine[index+5]));
                    food.setSalt(FloatUtils.parseNullableNumber(nextLine[index+6]));
                    food.setSugar(FloatUtils.parseNullableNumber(nextLine[index+7]));

                    // Mineral nutrients are given in milligrams, so we divide them by 1.000
                    Float sodium = FloatUtils.parseNullableNumber(nextLine[index+8]);
                    sodium = sodium != null ? sodium / 1000 : null;
                    food.setSodium(sodium);

                    foodList.add(food);
                }
            }

            Collections.reverse(foodList);
            FoodDao.getInstance().deleteAll();
            FoodDao.getInstance().bulkCreateOrUpdate(foodList);

            Log.i(TAG, String.format("Imported %d common food items from csv", foodList.size()));
            PreferenceStore.getInstance().setDidImportCommonFood(locale, true);

        } catch (Exception exception) {
            Log.e(TAG, exception.toString());
        }
    }
}
