package sneerteam.tutorial.rockpaperscissors;

import java.util.*;

import rx.android.schedulers.*;
import rx.functions.*;
import sneerteam.snapi.*;
import sneerteam.tutorial.rockpaperscissors.RockPaperScissors.*;
import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class RockPaperScissorsActivity extends Activity {
	
	private static final String GAMES = "games";
	private static final String RPS = "rock-paper-scissors";
	private static final String MATCHES = "matches";
	private static final String CHALLENGES = "challenges";

	private static final int PICK_CONTACT_REQUEST = 100;
	
	private final RockPaperScissors rps = new RockPaperScissors(this);
	private Cloud cloud;
	private String adversary;
	private Move move;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);		

		int rename_btnNewGame_to_btnChallenge;
		button(R.id.btnNewGame).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
			challenge();
		}});
		
		cloud = Cloud.cloudFor(this);	   
		
		cloud.path(":me", "contacts").children().subscribe(new Action1<PathEvent>() { @Override public void call(PathEvent child) {
			final String contactKey = (String)child.path().lastSegment();
			cloud.path(contactKey, GAMES, RPS, ":me", CHALLENGES).value().cast(String.class).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() { @Override public void call(String id) {
				int makeThisIntoANotificationInsteadOfAnAlert;
				new AlertDialog.Builder(RockPaperScissorsActivity.this)
					.setTitle("Challenge from " + contactKey)
					.setNegativeButton("Cancel", null)
					.setPositiveButton("OK", new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int id) {
						chooseMove();
						cloud.path(contactKey, GAMES, RPS, MATCHES, id).value().subscribe(new Action1<Object>() { @Override public void call(Object event) {
							toast((String) event);
							// do stuff
						}});
					}})
					.show();
			}});
		}});		
	}


	private void challenge() {
  		Intent intent = new Intent("sneerteam.intent.action.PICK_CONTACT");
  		startActivityForResult(intent, PICK_CONTACT_REQUEST);
  	}	
  	
	
  	@Override
  	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
  		super.onActivityResult(requestCode, resultCode, intent);
  		if (requestCode != PICK_CONTACT_REQUEST) return;
  		if (resultCode != RESULT_OK) return;

  		Bundle extras = intent.getExtras();
		adversary = extras.get("public_key").toString();
		toast(adversary);

		String match = UUID.randomUUID().toString();
		cloud.path(GAMES, RPS, adversary, CHALLENGES).pub(match);
		
		cloud.path(adversary, GAMES, RPS, MATCHES, match).value().subscribe(new Action1<Object>() { @Override public void call(Object event) {
			toast((String) event);
			// do stuff
		}}); 
  	}

  	
	private void chooseMove() {
		move = null;
		new AlertDialog.Builder(this)
			.setTitle("Choose your move against " + adversary)
			.setItems(
				new CharSequence[] {"Rock", "Paper", "Scissors"},
				new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int which) {
					move = Move.values()[which];					
					waitForAdversary();
				}}
			).show();
	}
	
	
	private void waitForAdversary() {
		final ProgressDialog waiting = ProgressDialog.show(this, null, "Waiting for " + adversary + "...");
		waiting.setIndeterminate(true);
		waiting.setCancelable(true);		
		rps.moveAgainst(adversary).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Move>() { @Override public void call(Move reply) {
			waiting.dismiss();
			onReply(reply);
		}});
	}
	
	
	private void onReply(Move reply) {
		String result = result(reply);				
		String message = "You used " + move + ". " + adversary + " used " + reply + ".";

		new AlertDialog.Builder(this)
			.setTitle(result).setMessage(message).setPositiveButton("OK", new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface dialog, int which) {
				playAgain();
			}}).show();
	}
	
	
	private String result(Move reply) {
		if (move == reply) return "Draw!";
		
		if (move == Move.ROCK	 && reply == Move.SCISSORS) return "You win!";
		if (move == Move.SCISSORS && reply == Move.PAPER   ) return "You win!";
		if (move == Move.PAPER	&& reply == Move.ROCK	) return "You win!";
		
		return "You lose";
	}	
	
	
	private void playAgain() {
		DialogInterface.OnClickListener chooseMove = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				chooseMove();
			}
		};
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Challenge " + adversary + " again?");
		builder.setPositiveButton("Yes", chooseMove);
		builder.setNegativeButton("No", null);
		builder.show();
	}
	
	
	private void toast(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}

	
	private Button button(int id) {
		return (Button)findViewById(id);
	}   
}

