/*
 * Just handles the preferences. Mostly stores run data.
 */

package com.sajarvis.adninja;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class Prefs {
	private static SharedPreferences mPrefs;
	private Editor editor;
	String TAG = "LiftingCalc";
	
	//Set up
	public Prefs(Context context){
        Prefs.mPrefs = context.getSharedPreferences("myAppPrefs", 0);	//0 = private
        this.editor = mPrefs.edit();
    }
	
	//Check if first run stuff
	//Returns true if first run ever.
    public boolean isFirstRun(){
    	return mPrefs.getBoolean("first", true);
    }
    //Store that it's been run once.
    public void setFirstRun() {
        editor.putBoolean("first",false);
        editor.commit();
    }
    //Returns true if first run after update or ever.
    public boolean isUpdate(int version){
    	return version > mPrefs.getInt("version", 0);
    }
    //Store that it's been run.
    public void setRunned(int version) {
        editor.putInt("version",version);
        editor.commit();
    }
    //Get block status
    public boolean isBlock(){
    	return mPrefs.getBoolean("block", false);
    }
    //Store block status
    public void setBlock(boolean block){
    	editor.putBoolean("block", block);
    	editor.commit();
    }
    //See if the file system info has been set. Returns true if the type has been set.
    public boolean isTypeSet(){
    	return mPrefs.getBoolean("typeIsSet", false);
    }
    //Store that the type has been set
    public void typeIsSet(boolean set){
    	editor.putBoolean("typeIsSet", set);
    	editor.commit();
    }
    //Store file system type
    public void setType(String systemType){
    	editor.putString("systemType", systemType);
    	editor.commit();
    }
    //Get file system type
    public String getType(){
    	return mPrefs.getString("systemType", "yaffs2");
    }
    //Store system mount point
    public void setMount(String mountPoint){
    	editor.putString("mountPoint", mountPoint);
    	editor.commit();
    }
    //Get system mount point
    public String getMount(){
    	return mPrefs.getString("mountPoint", "/dev/block/mtdblock3");
    }
    //Store whether the license has been confirmed. Don't check every time.
    public void licenseConfirmed(boolean confirmed){
    	editor.putBoolean("licenseConfirmed",confirmed);
    	editor.commit();
    }
    //See if the license has been confirmed.
    public boolean licenseIsConfirmed(){
    	return mPrefs.getBoolean("licenseConfirmed", false);
    }
}
