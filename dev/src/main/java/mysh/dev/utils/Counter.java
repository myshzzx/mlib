package mysh.dev.utils;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;

public class Counter {

	public Button upBtn;
	public Button downBtn;
	public Button resetBtn;

	private int up, down;

	public void upClick(ActionEvent actionEvent) {
		up++;
		refreshText();
	}

	public void downClick(ActionEvent actionEvent) {
		down++;
		refreshText();
	}

	public void resetClick(ActionEvent actionEvent) {
		up = 0; down = 0;
		refreshText();
	}

	private void refreshText() {
		upBtn.setText("up +" + up);
		downBtn.setText("down -" + down);
	}
}
