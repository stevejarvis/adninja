/*
 * GNU GENERAL PUBLIC LICENSE
 *
 * Ad Ninja is an Android application intended to block popup ads.
 * Copyright (C) 2014 Steve Jarvis
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
