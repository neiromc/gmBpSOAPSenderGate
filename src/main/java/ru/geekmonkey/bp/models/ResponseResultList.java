package ru.geekmonkey.bp.models;

import ru.geekmonkey.bp.yaml.LastResultMatching;

import java.util.ArrayList;

/**
 * Created by neiro on 26.07.16.
 */
public class ResponseResultList {

    private ArrayList<String> results = new ArrayList<>();


    public void add(final String s) {
        results.add(s);
    }

    public String get(final int i) {
        return results.get(i);
    }

    public int size() {
        return results.size();
    }

    public ArrayList<String> getList() {
        return results;
    }

    public int hasErrors(final ArrayList<LastResultMatching> lastResultMatchingList) {
        for (int i = 0; i < lastResultMatchingList.size(); i++) {
            int valueFromStepId = lastResultMatchingList.get(i).getValueFromStep;
            if ( "0".equals(results.get(valueFromStepId - 1)) )
                return (i + 1);
        }

        return 0;
    }

}
