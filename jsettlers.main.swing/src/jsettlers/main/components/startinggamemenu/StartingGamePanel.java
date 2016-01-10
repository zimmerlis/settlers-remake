/*******************************************************************************
 * Copyright (c) 2015
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.main.components.startinggamemenu;

import jsettlers.graphics.localization.Labels;
import jsettlers.graphics.map.IMapInterfaceConnector;
import jsettlers.graphics.map.MapContent;
import jsettlers.graphics.progress.EProgressState;
import jsettlers.graphics.startscreen.interfaces.EGameError;
import jsettlers.graphics.startscreen.interfaces.IStartedGame;
import jsettlers.graphics.startscreen.interfaces.IStartingGame;
import jsettlers.graphics.startscreen.interfaces.IStartingGameListener;
import jsettlers.main.swing.SettlersFrame;

import javax.swing.*;
import java.awt.*;

/**
 * @author codingberlin
 */
public class StartingGamePanel extends JPanel implements IStartingGameListener {

	private final JLabel messageLabel = new JLabel("", SwingConstants.CENTER);
	private final SettlersFrame settlersFrame;

	public StartingGamePanel(SettlersFrame settlersFrame) {
		this.settlersFrame = settlersFrame;
		createStructure();
		localize();
	}

	private void localize() {
		messageLabel.setText(Labels.getProgress(EProgressState.LOADING));
	}


	private void createStructure() {
		setLayout(new BorderLayout());
		add(messageLabel, BorderLayout.SOUTH);
	}

	public void setStartingGame(IStartingGame startingGame) {
		startingGame.setListener(this);
	}

	@Override public void startProgressChanged(EProgressState state, float progress) {
		SwingUtilities.invokeLater(() -> messageLabel.setText(Labels.getProgress(state)));
	}

	@Override public IMapInterfaceConnector preLoadFinished(IStartedGame game) {
		MapContent content = new MapContent(game, settlersFrame.getSoundPlayer());
		settlersFrame.setContent(content);
		game.setGameExitListener(game1 -> settlersFrame.showMainMenu());
		return content.getInterfaceConnector();
	}

	@Override public void startFailed(EGameError errorType, Exception exception) {

	}

	@Override public void startFinished() {

	}

}