/*
 * Ad Ninja. This app includes 2 hosts files in the assets folder. One is the standard
 * hosts file, the other includes redirects to localhost for all known ad providers.
 * Each click toggles on/off and copies the appropriate file to /system/etc/hosts.
 * 
 * Update for 1.1
 * -Add App Licensing & Allows App to SD
 * -New Ninja
 * 
 * Update for 1.2
 * - Added something I can't remember to the hosts file
 * - Fixed bug so that hosts file updates when it should
 * 
 * Update for 1.3
 * - Stored preference so license isn't checked for after it's confirmed
 * 
 * Update 1.4
 * - Changed story so button stays at bottom
 * - Allow landscape
 * - Threaded everything. Plus copy files EVERY time. Seems more robust and doesn't
 * 	take long
 * 
 * Update 1.5
 * - Bug fix. Crash when su denied from thread.
 */

//TODO get rid of License checking stuff for amazon

package com.sajarvis.adninja;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.vending.licensing.AESObfuscator;
import com.android.vending.licensing.LicenseChecker;
import com.android.vending.licensing.LicenseCheckerCallback;
import com.android.vending.licensing.ServerManagedPolicy;
import com.flurry.android.FlurryAgent;

public class AdNinjaActivity extends Activity{
	//Tag
	private final String TAG = "ninja";
	
	//So we can reference the task
	private HiyaTask hiyaTask = null;
	
	//Key for license checking.
	private static final String BASE64_PUBLIC_KEY = 
		"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxNkSAz" +
		"dqQVaLYOFmueXHEQzRgiX4hbqCFOdf3WpKyVmzffNz7ZkgMXqB1" +
		"VtANt/jpt7JxvgIGtXCE9pP1W0O3rTYEYOjdiTi36n+ULUVg8jw" +
		"aJoV2J9eRPj2dGvtvnkybZ9gVaRhf6e5ncj09N6H5SAB/KbDuqaIr" +
		"tK8EVYG/FpAmYeYR+JALfHM+ZAsNnxdCYXHsUqHI8gYZUqI26JLl86" +
		"ingR2jwhpgQQ1C/+Lnj0JiiPdlw/+SHD/3Dl+IYOtZVLeLqzV3/H/" +
		"3JY7SVQw3cruQZuGNNEmWGeuzR00gmQ2C9yfQ4xlvWy3/MoAgbjbcd" +
		"cH/M/9yKtu9qzxfP2irQIDAQAB";
	private static final byte[] SALT = new byte[] {
        46,-65,50,-118,-23,-53,34,-69,53,83,95,-45,-77,-113, 
        46,-93,-91,12,-50,39
    };
	private LicenseCheckerCallback mLicenseCheckerCallback;
    private LicenseChecker mChecker;
    
    // A handler on the UI thread.
    private Handler mHandler;
    
	//The hosts files and sd location.
	private static final File SD_DIR = new File(Environment.
			getExternalStorageDirectory().toString()+File.separator+".AdNinja");
	private static final File FILE_ALLOW = new File(SD_DIR,File.separator+
			"hosts_allow");
	private static final File FILE_BLOCK = new File(SD_DIR,File.separator+
			"hosts_block");
	
	//Preference class instance. Static so it can be referenced from the static
	//task class. Static is good because there's only one instance of this.
	private static Prefs prefs;
	
	//Incrementing the version copies new hosts files.
	private static final int VERSION = 2;
	
	//Current blocking status.
	private TextView status;
	
	//The big bad ninja and his animation.
	private ImageView ninjaAllow, ninjaBlock;
	private ScaleAnimation pulse;
	
    /** 
     * Called when the activity is first created. 
     * */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main);
        
		//Declare the widgets
        status = (TextView) findViewById(R.id.status);
        ninjaAllow = (ImageView) findViewById(R.id.ninjaAllow);
        ninjaBlock = (ImageView) findViewById(R.id.ninjaBlock);
        //New preferences instance
        prefs = new Prefs(this);
        //Create animation
        anim();
        
        //Set pic and status text right.
        updateStatus("null");
		
        //If it's an update and only on update, automatically set the files.
        if(prefs.isUpdate(VERSION) && !prefs.isFirstRun()){
        	if(prefs.isBlock()){
    			hiyaTask = new HiyaTask(this);
    			hiyaTask.execute(FILE_BLOCK);
    		}else{
    			hiyaTask = new HiyaTask(this);
    			hiyaTask.execute(FILE_ALLOW);
    		}
        }
        prefs.setRunned(VERSION);
	    
        //Launch welcome ON FIRST RUN.
  		if(prefs.isFirstRun()){
  			startActivity(new Intent(this, Intro.class));
  			prefs.setFirstRun();
  		}
        
        /*
         * Reattach the thread
         */
        hiyaTask = (HiyaTask) getLastNonConfigurationInstance();
        if(hiyaTask != null && hiyaTask.stillGoing){
        	hiyaTask.attach(this);
        }
        
	    //Set onclicklisteners
        ninjaAllow.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View arg0) {
				hiyaTask = new HiyaTask(AdNinjaActivity.this);
				hiyaTask.execute(FILE_BLOCK);
			}
        });
        ninjaBlock.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View arg0) {
				hiyaTask = new HiyaTask(AdNinjaActivity.this);
				hiyaTask.execute(FILE_ALLOW);
			}
        });
        
        /*Check licensing iff the license has not already been verified. This
         * is safe because if the app is uninstalled the prefs will be as well.
         */
        if(!prefs.licenseIsConfirmed()){  
	        //TODO uncomment for Market releases
        	//checkLicense();
        }
    } 
    
    //Start Flurry
    public void onStart(){
    	super.onStart();
    	FlurryAgent.onStartSession(this, "2HVAGQXAU6PYBA3AJYDL");
    	FlurryAgent.onPageView();
    }
    
    //Stop Flurry
    public void onStop(){
       super.onStop();
       FlurryAgent.onEndSession(this);
    }
    
    //Destroy activity
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mChecker != null) mChecker.onDestroy();
        
        //Attempt to prevent out of memory errors. Unbind layout so it can
        //be garbage collected.
        unbindDrawables(findViewById(R.id.root_layout));
        System.gc();
    }
    
    //Save the instance on orientation change
    @Override
	public Object onRetainNonConfigurationInstance() {
		if(hiyaTask != null) hiyaTask.detach();
		return(hiyaTask);
	}
    
    //Unbind the drawables.
    public void unbindDrawables(View view){
    	if (view.getBackground() != null) {
            view.getBackground().setCallback(null);
        }
    }
    
    //Detect menu button
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
    	super.onCreateOptionsMenu(menu);
    	MenuInflater inflater = new MenuInflater(this);
    	inflater.inflate(R.menu.menu, menu);
    	return true;
    }
    //Call the about event if it's asked for from the menu.
    @Override
    public boolean onOptionsItemSelected (MenuItem item){
    	switch (item.getItemId()){
    	case R.id.about:
    		startActivity(new Intent(this, Intro.class));
    		return true;
    	}
    	return false;
    }
    
    //Simple pulsing animation
    public void anim(){
    	float small = 0.9f;
    	float large = 1.16f;
    	pulse = new ScaleAnimation (large, small, large, small, Animation.RELATIVE_TO_SELF, 
				0.6f, Animation.RELATIVE_TO_SELF, 0.5f);
    	pulse.setRepeatMode(Animation.REVERSE);
    	pulse.setRepeatCount(Animation.INFINITE);
    	pulse.setDuration(1500);
    }
	
	//Check license
	public void checkLicense(){
		/*
         * License checker stuff. I kept it out of the if license is confirmed
         * block to avoid nullPointerExceptions, like in onDestroy()
         */
        mHandler = new Handler();

        // Try to use more data here. ANDROID_ID is a single point of attack.
        String deviceId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);

        // Library calls this when it's done.
        mLicenseCheckerCallback = new MyLicenseCheckerCallback();
        // Construct the LicenseChecker with a policy.
        mChecker = new LicenseChecker(
            this, new ServerManagedPolicy(this,
                new AESObfuscator(SALT, getPackageName(), deviceId)),
            BASE64_PUBLIC_KEY);
        
        doCheck();
	}
	
    //Make a toast noti. Just pass the message.
	public void toast(String msg){
		Context context = getApplicationContext();
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, msg, duration);
		toast.show();
	}
	
	//If pass "null", change the status text and image to whatever is in the prefs.
	//Else the licensing failed, we're turning their shat off and status = "Invalid License"
	public void updateStatus(String update){
		if(update.equals("null")){	//Then We're doing a normal update.
			if(prefs.isBlock()){
				ninjaAllow.setVisibility(View.GONE);
				ninjaBlock.setVisibility(View.VISIBLE);
				ninjaBlock.startAnimation(pulse);
				status.setText(getString(R.string.blocking_active));
			}else{
				ninjaBlock.clearAnimation();
				ninjaBlock.setVisibility(View.GONE);
				ninjaAllow.setVisibility(View.VISIBLE);
				status.setText(getString(R.string.blocking_inactive));
			}
		}else{	//We're updating after the license has failed.
			Log.w(TAG,"license failed");
			status.setText(update);
            ninjaAllow.setEnabled(false);
            ninjaBlock.setEnabled(false);
            
            //Not checking whether it succeeds, but don't think it matters really.
            //Even if it doesn't do the best to cut their shat off.
            hiyaTask = new HiyaTask(this);
            hiyaTask.execute(FILE_ALLOW);
			
            prefs.setBlock(false);
			ninjaBlock.clearAnimation();
			ninjaBlock.setVisibility(View.GONE);
			ninjaAllow.setVisibility(View.VISIBLE);
			FlurryAgent.onEvent("Invalid license: Turned blocking OFF");
		}
	}
	
	/*
	 * License checking methods. It's checked against the Market for a valid license.
	 * During the check buttons are disabled. If it passes everything is enabled and 
	 * use continues as normal. If check fails status is set to "Invalid License" and
	 * the user is prompted to exit or buy app and buttons remain disabled.
	 */
	protected Dialog onCreateDialog(int id) {
        //Declare dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
				
		switch(id){
        case 0:	//License failure
        	Log.w(TAG,"License failure dialog");
        	builder
	            .setTitle(R.string.unlicensed_dialog_title)
	            .setMessage(R.string.unlicensed_dialog_body)
	            .setPositiveButton(R.string.buy_button, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int which) {
	                    Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
	                        "http://market.android.com/details?id=" + getPackageName()));
	                    startActivity(marketIntent);
	                }
	            })
	            .setNegativeButton(R.string.quit_button, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int which) {
	                    finish();
	                }
	            }).
	            create();
        	break;
        	
        case 1:	//su denied
        	Log.w(TAG,"Su denied failure");
        	builder
	            .setTitle(R.string.su_denied_title)
	            .setMessage(R.string.su_denied_message)
	            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int which) {
	                	dialog.dismiss();
	                }
	            }).
	            create();
        	break;
        	
        case 2:	//Thread failed
        	Log.w(TAG,"Thread returned false");
        	builder
	            .setTitle(R.string.thread_fail_title)
	            .setMessage(R.string.thread_fail_msg)
	            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int which) {
	                	dialog.dismiss();
	                }
	            }).
	            create();
        	break;
        }
		
		AlertDialog dialog = builder.create();
		return dialog;
    }

    private void doCheck() {
    	//Disable buttons
        ninjaAllow.setEnabled(false);
        ninjaBlock.setEnabled(false);
        setProgressBarIndeterminateVisibility(true);
        status.setText(R.string.checking_license);
        mChecker.checkAccess(mLicenseCheckerCallback);
    }

    private void displayResult(final String result) {
        mHandler.post(new Runnable() {
            public void run() {
            	if(result.equals("allow")){
            		if(prefs.isBlock()){
            			status.setText("Ads ninja'd. Tap to tame.");
            		}else{
            			status.setText("Ads allowed. Tap to release his fury.");
            		}
            		ninjaAllow.setEnabled(true);
                    ninjaBlock.setEnabled(true);
                    prefs.licenseConfirmed(true);	//License has been confirmed, don't check again
            	}else{	//License fail
	                updateStatus(result);
            	} 
            }
        });
    }

    private class MyLicenseCheckerCallback implements LicenseCheckerCallback {
        public void allow() {
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
            // Should allow user access.
            displayResult("allow");
        }

        public void dontAllow() {
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
            displayResult(getString(R.string.dont_allow));
            showDialog(0);
        }

        public void applicationError(ApplicationErrorCode errorCode) {
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
            // This is a polite way of saying the developer made a mistake
            // while setting up or calling the license checker library.
            // Please examine the error code and fix the error.
            String result = String.format(getString(R.string.application_error), errorCode);
            displayResult(result);
        }
    }
    
    /*
     * Thread the I/O
     */
    static class HiyaTask extends AsyncTask<File, String, Boolean>{
    	//To reference the class
    	private AdNinjaActivity activity = null;
    	private String loading_status = "Loading Ninja Intel...";
    	private String executing_status = "Executing Mission...";
    	private String status = loading_status;
    	
    	//Make a progress dialog
  		private ProgressDialog dialog;
  		
  		//Cause we need to know if the task is still rockin
  		private boolean stillGoing = true;
    	
  		//Constructor
    	public HiyaTask(AdNinjaActivity act){
    		activity = act;
    		dialog = new ProgressDialog(activity);
    	}
    	
    	@Override
  		protected void onPreExecute(){
  			this.dialog.setMessage(status);
  			this.dialog.setCancelable(false);
  			this.dialog.show();
  		}
    	
    	//Update the dialog
		@Override
		protected void onProgressUpdate(String... msg) {
        	dialog.setMessage(msg[0]);
	    }
    	
		@Override
		protected Boolean doInBackground(File... file) {
			//Marks whether this method is successfully executed
	    	boolean success = false;
	    	
	    	//I made preferences to only copy files when necessary,
	    	//but changed my mind. They'll be copied everytime. It runs
	    	//in the background, so nbd. And if they delete the folder
	    	//or it becomes corrupted this is good. SD card needs to
	    	//be around anyway.
	    	createFiles();
	        try{
	        	copyAssets();
	        }catch(Exception e){
	        	Log.e("ninja","Error copying assets");
	        	return false;
	        }
	    	
	        //Change status
	        status = executing_status;
	        publishProgress(status);
	        
	    	//For root access
	    	Process process = null;
	    	BufferedReader cmdIn;
	    	DataOutputStream cmdOut;
	        //Get ROOT access.
	        try {
				process = Runtime.getRuntime().exec("su");
				
				//Stream writer
				cmdOut = new DataOutputStream(process.getOutputStream());
				cmdIn = new BufferedReader(new InputStreamReader(process.getInputStream()));
			} catch (IOException e1) {
				FlurryAgent.onError("4","su denied","Fail");
				
				return success;	//False, fail.
			}
			try{
				//See if the file system types have been gotten.
				if(!prefs.isTypeSet()){
					//They need to be gotten.
					if(getFileSystem(cmdOut, cmdIn)){
						prefs.typeIsSet(true);	//But not again.
					}else{//else there was an issue finding system info, we'll have to hope the defaults work.
						FlurryAgent.onError("5", "Error finding file system info, going w/ defaults.", "Warning");
					}
				}
		        //To hold the values for filesystem type and mount point
				String[] fileSystem = new String[2];
				fileSystem[0] = prefs.getType();
				fileSystem[1] = prefs.getMount();	//Stored is faster.
				//Mount read/write
				FlurryAgent.onEvent("mount -o rw,remount -t "+fileSystem[0]+" "+fileSystem[1]+" /system");	//See what it's mounting.
				cmdOut.writeBytes("mount -o rw,remount -t "+fileSystem[0]+" "+fileSystem[1]+" /system\n");
				//Write to the hosts file
				cmdOut.writeBytes("cat "+file[0].toString()+" > /system/etc/hosts\n");	//Cause it doesn't need busybox
				//Remount read only
				cmdOut.writeBytes("mount -o ro,remount -t "+fileSystem[0]+" "+fileSystem[1]+" /system\n");
				//Flush all buffer
				cmdOut.writeBytes("exit");
				cmdOut.flush();
		    	cmdOut.close();
			}
			catch(IOException e){
				FlurryAgent.onError("6", "IOException executing statements", "Fail");
				return false;
			}
			try {
				process.waitFor();
				if (process.exitValue() == 0) { 
		            success = true; 
				}else{
					FlurryAgent.onError("7", "Bad exit value: "+process.exitValue(), "Fail");
				}
			} catch (InterruptedException e) {
				FlurryAgent.onError("8", "waitFor() failed", "Warning");
				e.printStackTrace();
			}
			
			return success;
		}
		
		//Passed success from do in background
		@Override
		protected void onPostExecute(Boolean result){
			this.dialog.dismiss();
			//Marks whether it needs to be reattached
			this.stillGoing = false;
			
			//Update the UI
			if(result){
				prefs.setBlock(!prefs.isBlock());	//Toggle
				activity.updateStatus("null");
				if(prefs.isBlock()){
					FlurryAgent.onEvent("Turned blocking ON");
				}else{
				}
			}else{
				activity.showDialog(2);
				FlurryAgent.onError("1", "hiya returned false", "Fail");
			}
			
			//Remove the context reference. Causes out of memory errors.
			activity.hiyaTask = null;
			activity = null;
		}
		
		//Attach
		public void attach(AdNinjaActivity act){
			activity = act;
			
			//Take care of the dialog
			this.dialog.setMessage(status);
  			this.dialog.setCancelable(false);
  			this.dialog.show();
		}
		
		//Detach
		public void detach(){
			activity = null;
			
			//Dismiss dialog
			this.dialog.dismiss();
		}
		
		//Create the new files.
	    public void createFiles(){
	        SD_DIR.mkdirs();
	        try {
				FILE_ALLOW.createNewFile();
				FILE_BLOCK.createNewFile();
			} catch (IOException e) {
				FlurryAgent.onError("2", "IOException creating new files", "Fail");
			}
	    }
		
		/* Copy the hosts_allow and hosts_block to sd card. Runs first time or
		 * on update. On update files will be overwritten automatically. After this 
		 * runs hosts_block and hosts_allow will be in (external)/.AdNinja/
		 */
		public void copyAssets() throws IOException{
			//Removed the check to see if the card is mounted bc I use the error
			//to trigger a message.
		    AssetManager assetManager = activity.getAssets();
		    InputStream inBlock = assetManager.open("hosts_block");
		    InputStream inAllow = assetManager.open("hosts_allow");
		    OutputStream outBlock = new FileOutputStream(FILE_BLOCK);
		    OutputStream outAllow = new FileOutputStream(FILE_ALLOW);
		    copyFile(inBlock, outBlock);
		    copyFile(inAllow, outAllow);
		    
		    //Close toys
		    inAllow.close();
		    inBlock.close();
			outAllow.flush();
			outBlock.flush();
			outAllow.close();
			outBlock.close();
		}
		
		//Pass streams in, out
	    public void copyFile(InputStream in, OutputStream fOut) throws IOException {
	    	//transfer bytes from the in to the out
	    	byte[] buffer = new byte[1024];
	    	int length;
	    	while ((length = in.read(buffer))>0){
	    		fOut.write(buffer, 0, length);
	    	}
	    }    	
		
		//Using Android (not Busybox) commands finds the file system type for mounting and the mount directory.
		//Sets the values in preferences. Should run on install and update.
		public boolean getFileSystem(DataOutputStream cmdOut, BufferedReader cmdIn) throws IOException{
			cmdOut.writeBytes("mount\n");
			cmdOut.flush();
			//Now find out what type is listen in there
			String response = "";
			while((response = cmdIn.readLine()) != null){
				if(response.contains("/system")){
					break;
				}
			}
			//Now we have the line we want.
			FlurryAgent.onEvent("File System: "+response);
			if(response == null) return false;	//It gets null if su was denied, just quit.
			if(response.contains(" ext3 ")){
				prefs.setType("ext3");
			}
			else if(response.contains(" ext4 ")){
				prefs.setType("ext4");
			}
			else if(response.contains(" yaffs2 ")){
				prefs.setType("yaffs2");
			}
			else if(response.contains(" rfs ")){
				prefs.setType("rfs");
			}
			
			//And the mount point. It seems to be the first listed in "mount"
			String mounts[] = response.split(" ");	//Split on space.
			prefs.setMount(mounts[0]);	//We want the first column.
			
			return true;	//On success
		}
    }
}