package de.westnordost.streetcomplete.settings;

import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import javax.inject.Inject;

import de.westnordost.streetcomplete.Injector;
import de.westnordost.streetcomplete.Prefs;
import de.westnordost.streetcomplete.data.QuestStatus;
import de.westnordost.streetcomplete.data.osmnotes.OsmNoteQuest;
import de.westnordost.streetcomplete.data.osmnotes.OsmNoteQuestDao;
import de.westnordost.streetcomplete.oauth.OAuth;
import de.westnordost.streetcomplete.R;
import de.westnordost.streetcomplete.oauth.OAuthWebViewDialogFragment;
import de.westnordost.streetcomplete.util.InlineAsyncTask;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener
{
	@Inject SharedPreferences prefs;
	@Inject OsmNoteQuestDao osmNoteQuestDao;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Injector.instance.getApplicationComponent().inject(this);

		addPreferencesFromResource(R.xml.preferences);

		Preference oauth = getPreferenceScreen().findPreference("oauth");
		oauth.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
		{
			@Override
			public boolean onPreferenceClick(Preference preference)
			{
				final FragmentManager fm = getFragmentManager();

				OAuthWebViewDialogFragment dlg = OAuthWebViewDialogFragment.create(
						OAuth.createConsumer(), OAuth.createProvider()
				);

				dlg.show(fm, OAuthWebViewDialogFragment.TAG);
				return true;
			}
		});
	}

	@Override
	public void onStart()
	{
		super.onStart();
		updateOsmAuthSummary();
	}

	private void updateOsmAuthSummary()
	{
		Preference oauth = getPreferenceScreen().findPreference("oauth");
		if (OAuth.isAuthorized(prefs))
		{
			oauth.setSummary(R.string.pref_title_authorized_summary);
		}
		else
		{
			oauth.setSummary(R.string.pref_title_not_authorized_summary);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		prefs.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		prefs.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		if(key.equals(Prefs.OAUTH_ACCESS_TOKEN_SECRET))
		{
			updateOsmAuthSummary();
		}
		else if(key.equals(Prefs.SHOW_NOTES_NOT_PHRASED_AS_QUESTIONS))
		{
			final Preference pref = getPreferenceScreen().findPreference(Prefs.SHOW_NOTES_NOT_PHRASED_AS_QUESTIONS);

			pref.setEnabled(false);
			new Thread() { @Override public void run()
			{
				for(OsmNoteQuest quest : osmNoteQuestDao.getAll(null,null))
				{
					if (quest.getStatus() == QuestStatus.NEW || quest.getStatus() == QuestStatus.INVISIBLE)
					{
						boolean showNonQuestionNotes = prefs.getBoolean(Prefs.SHOW_NOTES_NOT_PHRASED_AS_QUESTIONS, false);
						boolean visible = quest.probablyContainsQuestion() || showNonQuestionNotes;
						QuestStatus newQuestStatus = visible ? QuestStatus.NEW : QuestStatus.INVISIBLE;

						if (quest.getStatus() != newQuestStatus)
						{
							quest.setStatus(newQuestStatus);
							osmNoteQuestDao.update(quest);
						}
					}
				}
				getActivity().runOnUiThread(new Runnable() { @Override public void run()
				{
					pref.setEnabled(true);
				}});

			}}.start();
		}
	}
}
