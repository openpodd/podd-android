package org.cm.podd.report;

import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import org.cm.podd.report.model.MultipleChoiceQuestion;
import org.cm.podd.report.model.MultipleChoiceSelection;
import org.cm.podd.report.model.view.MultipleChoiceQuestionView;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
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

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            MultipleChoiceQuestion<String> question = new MultipleChoiceQuestion<String>(MultipleChoiceSelection.MULTIPLE);
            question.setTitle("choose one");
            question.addItem("first", "first");
            question.addItem("second", "second");
            question.addItem("third", "third");
            question.setFreeTextChoiceEnable(true);
            question.setFreeTextId("other");
            question.setFreeTextName("other");
            question.setFreeTextText("other");

//            Question<Integer> q1 = new Question<Integer>();
//            q1.setDataType(DataType.INTEGER);
//            q1.setTitle("How old are you?");
//            q1.setName("age");
//
            RelativeLayout v = (RelativeLayout) rootView;
            v.addView(new MultipleChoiceQuestionView(getActivity(), question));

            return rootView;
        }
    }
}
