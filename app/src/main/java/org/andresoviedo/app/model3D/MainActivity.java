package org.andresoviedo.app.model3D;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

import org.andresoviedo.app.model3D.view.MenuActivity;
import org.andresoviedo.app.model3D.view.ModelActivity;
import org.andresoviedo.dddmodel2.R;
import org.andresoviedo.util.android.AndroidURLStreamHandlerFactory;

import java.net.URL;
/**************************************************************************************************/
public class MainActivity extends Activity {
	/**********************************************************************************************/
    static {
        System.setProperty("java.protocol.handler.pkgs", "org.andresoviedo.util.android");

        URL.setURLStreamHandlerFactory(new AndroidURLStreamHandlerFactory());
    }
	/**********************************************************************************************/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		MainActivity.this.startActivity(new Intent(MainActivity.this.getApplicationContext(), MenuActivity.class));

		MainActivity.this.finish();
	}
	/**********************************************************************************************/
	private void init() {
		MainActivity.this.startActivity(new Intent(MainActivity.this.getApplicationContext(), ModelActivity.class));

		MainActivity.this.finish();
	}
	/**********************************************************************************************/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);

		return true;
	}
}
