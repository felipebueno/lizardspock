package sneerteam.tutorial.rockpaperscissors;

import static sneer.snapi.CloudPath.*;

import java.util.concurrent.*;

import rx.functions.*;
import sneer.snapi.*;
import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class RockPaperScissorsActivity extends Activity {

	enum Move { ROCK, PAPER, SCISSORS };
	
	private static final String GAMES = "games";
	private static final String RPS = "rock-paper-scissors";

	private Cloud cloud;

	private Contact adversary;

	private long matchTime;
	private Move move;
	
	private AlertDialog currentAlert;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		button(R.id.btnChallenge).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
			challengeSomeFriend();
		}});
		
		cloud = Cloud.onAndroidMainThread(this);
		
		cloud.contacts().subscribe(new Action1<Contact>() {@Override public void call(Contact contact) {
		    listenToChallengesFrom(contact);
        }});		
	}
	

	private void listenToChallengesFrom(final Contact contact) {
		cloud.path(contact.publicKey(), GAMES, RPS, ME).children().subscribe(new Action1<PathEvent>() { @Override public void call(final PathEvent child) {
			final long matchTime = (Long)child.path().lastSegment();
			cloud.path(ME, GAMES, RPS, contact.publicKey(), matchTime).exists(1000, TimeUnit.MILLISECONDS).subscribe(new Action1<Boolean>() { @Override public void call(Boolean exists) {
			    if (exists) return;
			    
			    adversary = contact;
			    RockPaperScissorsActivity.this.matchTime = matchTime;
			    startMatch();
		    }});
		}});
	}
	
	
	private void challengeSomeFriend() {
		ContactPicker.pickContact(this).subscribe(new Action1<Contact>() {@Override public void call(Contact contact) {
            adversary = contact;
            challengeAdversary();
        }});
  	}

	
	private void challengeAdversary() {
		matchTime = System.currentTimeMillis();
		startMatch();
	}


	private void startMatch() {
	    move = null;
		alert("Choose your move against " + adversary.nickname(),
			options("Rock", "Paper", "Scissors"),
			new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int option) {
				move = Move.values()[option];
				cloud.path(GAMES, RPS, adversary.publicKey(), matchTime).pub(move.name());
				waitForAdversary();
			}}
		);
	}

 
	private void waitForAdversary() {
		final ProgressDialog waiting = progressDialog("Waiting for " + adversary.nickname() + "...");		
		cloud.path(adversary.publicKey(), GAMES, RPS, ME, matchTime).value().subscribe(new Action1<Object>() { @Override public void call(Object theirMove) {
			waiting.dismiss();
			onReply(Move.valueOf((String)theirMove));
		}});
	}


	private void onReply(Move theirMove) {
		String outcome = outcome(theirMove);				
		String message = "You used " + move + ". " + adversary.nickname() + " used " + theirMove + ".";

		alert(outcome + " " + message, options("OK"), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface dialog, int which) {
		}});
	}


	private String outcome(Move theirMove) {
		if (move == theirMove) return "Draw!";

		if (move == Move.ROCK     && theirMove == Move.SCISSORS) return "You win!";
		if (move == Move.SCISSORS && theirMove == Move.PAPER   ) return "You win!";
		if (move == Move.PAPER    && theirMove == Move.ROCK    ) return "You win!";

		return "You lose!";
	}


	private Button button(int id) {
		return (Button)findViewById(id);
	}


	private ProgressDialog progressDialog(String message) {
		ProgressDialog ret = ProgressDialog.show(this, null, message);
		ret.setIndeterminate(true);
		ret.setCancelable(true);
		return ret;
	}


	private void alert(String titge, CharSequence[] items, DialogInterface.OnClickListener onClickListener) {
		if (currentAlert != null) currentAlert.dismiss();
		
		currentAlert = new AlertDialog.Builder(this)
			.setTitle(titge)
			.setItems(items, onClickListener)
			.show();
	}


	private CharSequence[] options(CharSequence... options) {
		return options;
	}
	

	@Override
	protected void onResume() {
		super.onResume();
		cloud.unregisterForNotification(GAMES, RPS, ME);
	}
	
	 
	@Override
	protected void onPause() {
		Intent intent = new Intent(this, RockPaperScissorsActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		cloud.registerForNotification(intent, GAMES, RPS, ME);
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		if (cloud != null) cloud.dispose();
		super.onDestroy();
	}
}
