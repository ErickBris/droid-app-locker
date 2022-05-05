package com.iapps.applocker.locker.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.preference.PreferenceFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.iapps.applocker.R;
import com.iapps.applocker.locker.lock.AppLockService;
import com.iapps.applocker.locker.lock.LockPreferences;
import com.iapps.applocker.locker.lock.LockService;
import com.iapps.applocker.locker.util.PrefUtils;

@SuppressLint("NewApi")
public class SettingsFragment extends PreferenceFragment implements
		OnPreferenceChangeListener, OnSharedPreferenceChangeListener,
		OnPreferenceClickListener {

	private SharedPreferences mPrefs;
	private SharedPreferences.Editor mEditor;
	private CheckBoxPreference mShortExit;
	private CheckBoxPreference mTransparentPref;
	private PreferenceScreen mPrefScreen;
	private Preference mLockTypePref;
	private PreferenceCategory mCatPassword;
	private PreferenceCategory mCatPattern;

	private EditTextPreference mPatternSize;

	private PrefUtils mPrefUtils;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mPrefUtils = new PrefUtils(getActivity());

		PreferenceManager pm = getPreferenceManager();
		pm.setSharedPreferencesName(PrefUtils.PREF_FILE_DEFAULT);
		pm.setSharedPreferencesMode(Context.MODE_PRIVATE);
		addPreferencesFromResource(R.xml.prefs);

		mPrefs = pm.getSharedPreferences();
		mEditor = pm.getSharedPreferences().edit();
		mShortExit = (CheckBoxPreference) findPreference(getString(R.string.pref_key_delay_status));
		mTransparentPref = (CheckBoxPreference) findPreference(getString(R.string.pref_key_hide_notification_icon));
		mLockTypePref = (Preference) findPreference(getString(R.string.pref_key_lock_type));
		mPrefScreen = (PreferenceScreen) findPreference(getString(R.string.pref_key_screen));
		mCatPassword = (PreferenceCategory) findPreference(getString(R.string.pref_key_cat_password));
		mCatPattern = (PreferenceCategory) findPreference(getString(R.string.pref_key_cat_pattern));
		mPatternSize = (EditTextPreference) findPreference(getString(R.string.pref_key_pattern_size));
		initialize();
	}

	@Override
	public View onCreateView(LayoutInflater paramLayoutInflater,
			ViewGroup paramViewGroup, Bundle paramBundle) {
		getActivity().setTitle(R.string.fragment_title_settings);
		return super.onCreateView(paramLayoutInflater, paramViewGroup,
				paramBundle);
	}

	void initialize() {

		showCategory();
		mPrefs.registerOnSharedPreferenceChangeListener(this);
		mPatternSize.setOnPreferenceChangeListener(this);
		mLockTypePref.setOnPreferenceClickListener(this);

	}

	// When user clicks preference and it has still to be saved
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		Log.d("prefs", "Preference change! " + preference.getKey());
		String key = preference.getKey();
		String keyDelayTime = getString(R.string.pref_key_delay_time);
		String background = getString(R.string.pref_key_background);
		String patternSize = getString(R.string.pref_key_pattern_size);

		if (key.equals(keyDelayTime)) {
			String newTime = (String) newValue;
			boolean isZero = (newTime.length() == 0);
			try {
				isZero = (Long.parseLong(newTime) == 0);
			} catch (NumberFormatException e) {
				isZero = true;
			}
			if (isZero) {
				mShortExit.setChecked(false);
				mEditor.putBoolean(getString(R.string.pref_key_delay_status),
						false);
				mEditor.commit();

			} else {
				String res = String.valueOf(Long.parseLong(newTime));
				if (!newTime.equals(res)) {
					mEditor.putString(
							getString(R.string.pref_key_delay_status), res);
					mEditor.commit();
				}
			}
		} else if (key.equals(patternSize)) {
			int newPatternWidth;
			try {
				newPatternWidth = Integer.parseInt((String) newValue);
			} catch (Exception e) {
				newPatternWidth = Integer.parseInt(getActivity().getString(
						R.string.pref_def_pattern_size));
			}
			LockService.showCreate(getActivity(), LockPreferences.TYPE_PATTERN,
					newPatternWidth);
			return false;
		}
		return true;
	}

	// When the preference changed on disk
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
		String keyHideIcon = getString(R.string.pref_key_hide_launcher_icon);
		mPrefs = sp;

		Log.d("", "SharedPreference changed on disk (key=" + key + ")");
		if (key.equals(mPatternSize.getKey())) {
			mPatternSize.setText(sp.getString(key, null));
		}

		setupMessagesAndViews();

		Log.d("", "restating service");
		AppLockService.restart(getActivity());
	}

	private void setupMessagesAndViews() {
		showCategory();
	}

	private void showCategory() {
		if (mPrefUtils.getCurrentLockTypeInt() == LockPreferences.TYPE_PASSWORD) {
			mLockTypePref.setSummary(R.string.pref_list_lock_type_password);
			mPrefScreen.removePreference(mCatPattern);
			mPrefScreen.addPreference(mCatPassword);
		} else {
			mLockTypePref.setSummary(R.string.pref_list_lock_type_pattern);
			mPrefScreen.removePreference(mCatPassword);
			mPrefScreen.addPreference(mCatPattern);
		}
	}

	// });
	// }

	@Override
	public void onDestroy() {
		super.onDestroy();
		mPrefs.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		final String key = preference.getKey();
		final String lockType = getString(R.string.pref_key_lock_type);
		final String patternSize = getString(R.string.pref_key_pattern_size);

		if (key.equals(lockType)) {
			getChangePasswordDialog(getActivity()).show();
		} else if (key.equals(patternSize)) {
			mPatternSize.setText(mPrefs.getString(patternSize, null));
		}
		return false;
	}

	private static final int IMG_REQ_CODE = 0;

	@Override
	public void onActivityResult(int req, int res, Intent data) {
		Log.d("", "onActivityResult");
		if (req == IMG_REQ_CODE && res == Activity.RESULT_OK) {
			String image = data.getData().toString();
			mPrefUtils.put(R.string.pref_key_background, image).apply();
		}
		Toast.makeText(getActivity(), R.string.background_changed,
				Toast.LENGTH_SHORT).show();
		super.onActivityResult(req, res, data);
	}

	/**
	 * The dialog that allows the user to select between password and pattern
	 * options
	 * @param c
	 * @return
	 */
	private static AlertDialog getChangePasswordDialog(final Context c) {
		final AlertDialog.Builder choose = new AlertDialog.Builder(c);
		choose.setTitle(R.string.old_main_choose_lock_type);
		choose.setItems(R.array.lock_type_names, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int lockType = which == 0 ? LockPreferences.TYPE_PASSWORD
						: LockPreferences.TYPE_PATTERN;
				LockService.showCreate(c, lockType);
			}
		});
		return choose.create();   
	}

	public boolean onPreferenceTreeClick() {
		return false;
	}
}
