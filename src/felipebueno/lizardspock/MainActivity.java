package felipebueno.lizardspock;

import static felipebueno.lizardspock.MainActivity.Move.*;
import sneer.android.ui.*;
import android.app.*;
import android.content.*;
import android.content.DialogInterface.OnCancelListener;

public class MainActivity extends PartnerSessionActivity {

	enum Move { ROCK, PAPER, SCISSORS, LIZARD, SPOCK };

	private Move yourMove;
	private boolean waitingForYourMove;

	private String adversary;
	private Move adversarysMove;
	private ProgressDialog waitingForAdversarysMove;

	
	@Override
	protected void onPartnerName(String name) {
		adversary = name;
	}
	
	
	@Override
	protected void onMessageToPartner(Object message) {
		yourMove = Move.valueOf((String)message);
	}

	
	@Override
	protected void onMessageFromPartner(Object message) {
		adversarysMove = Move.valueOf((String)message);
	}

	
	@Override
	protected void update() {
		if (yourMove == null) {
			waitForYourMove();
			return;
		}
		
		if (adversarysMove == null) {
			waitingForAdversarysMove = progressDialog("Waiting for " + adversary + "...");
			return;
		}
		if (waitingForAdversarysMove != null) waitingForAdversarysMove.dismiss();
		
		gameOver();
	}


	private void waitForYourMove() {
		if (waitingForYourMove) return;
		waitingForYourMove = true;
		
		alert("Choose Your Move", options("Rock", "Paper", "Scissors", "Lizard", "Spock"), new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int option) {
			String move = Move.values()[option].name();
			send("Lizard Spock Challenge!", move);
		}});
	}
	

	private void gameOver() {
		String outcome = outcome();				
		String message = "You used " + yourMove + ". " + adversary + " used " + adversarysMove + ".";

		alert(outcome + " " + message, options("OK"), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface dialog, int which) {
			finish();
		}});
	}


	private String outcome() {
		if (yourMove == adversarysMove) return "Draw!";

		if (yourMove == ROCK     && adversarysMove == SCISSORS) return "You win!";
		if (yourMove == ROCK     && adversarysMove == LIZARD  ) return "You win!";
		
		if (yourMove == SCISSORS && adversarysMove == PAPER   ) return "You win!";
		if (yourMove == SCISSORS && adversarysMove == LIZARD  ) return "You win!";
		
		if (yourMove == PAPER    && adversarysMove == ROCK    ) return "You win!";
		if (yourMove == PAPER    && adversarysMove == SPOCK   ) return "You win!";
		
		if (yourMove == LIZARD   && adversarysMove == SPOCK   ) return "You win!";
		if (yourMove == LIZARD   && adversarysMove == PAPER   ) return "You win!";
		
		if (yourMove == SPOCK    && adversarysMove == ROCK    ) return "You win!";
		if (yourMove == SPOCK    && adversarysMove == SCISSORS) return "You win!";

		return "You lose!";
	}


	private ProgressDialog progressDialog(String message) {
		ProgressDialog ret = ProgressDialog.show(this, null, message);
		ret.setIndeterminate(true);
		ret.setCancelable(true);
		ret.setOnCancelListener(new OnCancelListener() {  @Override public void onCancel(DialogInterface dialog) {
			finish();
		} });
		return ret;
	}


	protected void alert(String title, CharSequence[] items, DialogInterface.OnClickListener onClickListener) {
		new AlertDialog.Builder(this)
			.setTitle(title)
			.setItems(items, onClickListener)
			.show()
			.setOnCancelListener(new OnCancelListener() {  @Override public void onCancel(DialogInterface dialog) {
				finish();
			} });;
	}


	private CharSequence[] options(CharSequence... options) {
		return options;
	}

}