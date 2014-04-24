package croft.james.amulet;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class HomeFragment extends Fragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_home, container, false);
		
		getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

		view.findViewById(R.id.inspection_task_button).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
				selectItem(new InspectionFragment(), 0);
			}
		});

		view.findViewById(R.id.sequence_task_button).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
				selectItem(new SequenceFragment(), 1);
			}
		});

		view.findViewById(R.id.pilot_task_button).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
				selectItem(new PilotFragment(), 2);
			}
		});

		return view;
	}

	private void selectItem(Fragment fragment, int itemNumber){

		Bundle args = new Bundle();
		args.putInt("task_fragment", itemNumber);
		fragment.setArguments(args);

		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).addToBackStack("task_fragment").commit();
	}
}
