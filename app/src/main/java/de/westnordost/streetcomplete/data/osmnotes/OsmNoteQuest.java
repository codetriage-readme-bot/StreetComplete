package de.westnordost.streetcomplete.data.osmnotes;

import java.util.Date;

import de.westnordost.streetcomplete.data.Quest;
import de.westnordost.streetcomplete.data.QuestImportance;
import de.westnordost.streetcomplete.data.QuestStatus;
import de.westnordost.streetcomplete.data.QuestType;
import de.westnordost.streetcomplete.data.osm.ElementGeometry;
import de.westnordost.streetcomplete.quests.AbstractQuestAnswerFragment;
import de.westnordost.streetcomplete.quests.note_discussion.NoteDiscussionForm;
import de.westnordost.osmapi.map.data.LatLon;
import de.westnordost.osmapi.notes.Note;

public class OsmNoteQuest implements Quest
{
	public OsmNoteQuest(Note note)
	{
		this(null, note, QuestStatus.NEW, null, new Date());
	}

	public OsmNoteQuest(Long id, Note note, QuestStatus status, String comment, Date lastUpdate)
	{
		this.id = id;
		this.note = note;
		this.status = status;
		this.comment = comment;
		this.lastUpdate = lastUpdate;
	}

	private Long id;
	private Date lastUpdate;
	private QuestStatus status;
	private Note note;

	private String comment;

	public static final QuestType type = new NoteQuestType();

	@Override public QuestType getType()
	{
		return type;
	}

	@Override public QuestStatus getStatus()
	{
		return status;
	}

	@Override public void setStatus(QuestStatus status)
	{
		this.status = status;
		/* if it is hidden, clear notes comments because we do not need them anymore and they take
		 up (a lot of) space in the DB */
		if(status == QuestStatus.HIDDEN)
		{
			if (note != null) note.comments.clear();
		}
	}

	@Override public Long getId()
	{
		return id;
	}

	@Override public LatLon getMarkerLocation()
	{
		return note.position;
	}

	@Override public ElementGeometry getGeometry()
	{
		// NOTE: using the same method as in CreateNote, we could actually get the ElementGeometry
		// here. However, to make users answer notes that other users created, barely makes sense
		// (otherwise they could probably answer it themselves), so any notes created by this app
		// will/should likely not show up for other users of this app

		// no geometry other than the marker location
		return new ElementGeometry(getMarkerLocation());
	}

	public Note getNote()
	{
		return note;
	}

	public void setNote(Note note)
	{
		this.note = note;
	}

	public String getComment()
	{
		return comment;
	}

	public void setComment(String comment)
	{
		this.comment = comment;
	}

	public Date getLastUpdate()
	{
		return lastUpdate;
	}

	@Override public void setId(long id)
	{
		this.id = id;
	}

	private static class NoteQuestType implements QuestType
	{
		@Override public int importance()
		{
			return QuestImportance.NOTE;
		}

		@Override public AbstractQuestAnswerFragment getForm()
		{
			return new NoteDiscussionForm();
		}

		@Override public String getIconName() {	return "note"; }
	}

	public boolean probablyContainsQuestion()
	{
		/* from left to right (if smartass IntelliJ wouldn't mess up left-to-right):
		   - latin question mark
		   - greek question mark (a different character than semikolon, though same appearance)
		   - semikolon (often used instead of proper greek question mark)
		   - mirrored question mark (used in script written from right to left, like Arabic)
		   - armenian question mark
		   - ethopian question mark
		   - full width question mark (often used in modern Chinese / Japanese)
		   (Source: https://en.wikipedia.org/wiki/Question_mark)

			NOTE: some languages, like Thai, do not use any question mark, so this would be more
			difficult to determine.
	   */
		String questionMarksAroundTheWorld = "[?;;؟՞፧？]";

		String text = note.comments.get(0).text;
		return text.matches(".*" + questionMarksAroundTheWorld + ".*");
	}
}
