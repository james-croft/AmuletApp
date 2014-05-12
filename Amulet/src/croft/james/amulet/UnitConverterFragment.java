package croft.james.amulet;

import croft.james.amulet.helpers.UnitConverter;
import android.app.Fragment;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class UnitConverterFragment extends Fragment {
	View view;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_unit_converter, container, false);

		getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		
		getActivity().setTitle("Unit Converter");

		view.findViewById(R.id.convert).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
				float drinkPercentage = 0;
				
				try{
					drinkPercentage = Float.parseFloat(((EditText)view.findViewById(R.id.drink_percentage)).getText().toString());
				}catch (Exception ex){
					Log.e("log_error", ex.getMessage());
				}
				
				if(drinkPercentage == 0) {
					return;
				}
				
				float drinkUnits = 0;
				float drinkQuantity = 0;
				
				try{
					drinkUnits = Float.parseFloat(((EditText)view.findViewById(R.id.drink_unit)).getText().toString());
				} catch (Exception ex) {
					Log.e("error", ex.getMessage());
				}
				
				if(drinkUnits == 0) {
					
					try{
						drinkQuantity = Float.parseFloat(((EditText)view.findViewById(R.id.drink_quantity)).getText().toString());
					} catch (Exception ex) {
						Log.e("error", ex.getMessage());
					}
					
					if(drinkQuantity == 0) {
						return;
					}
					
					drinkUnits = UnitConverter.ToUnit(drinkQuantity, drinkPercentage);
					((EditText)view.findViewById(R.id.drink_unit)).setText(String.valueOf(drinkUnits));
				} else {
					drinkQuantity = UnitConverter.ToMeasurement(drinkUnits, drinkPercentage);
					((EditText)view.findViewById(R.id.drink_quantity)).setText(String.valueOf(drinkQuantity));
				}
			}
		});

		return view;
	}
}
