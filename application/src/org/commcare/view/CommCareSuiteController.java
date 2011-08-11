/**
 * 
 */
package org.commcare.view;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import org.commcare.api.transitions.MenuTransitions;
import org.commcare.suite.model.Menu;
import org.commcare.util.CommCareSessionController;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;
import org.javarosa.j2me.view.J2MEDisplay;

/**
 * @author ctsims
 *
 */
public class CommCareSuiteController implements HandledCommandListener {

	CommCareListView view;
	MenuTransitions transitions;
	CommCareSessionController controller;
	
	Menu m;
	public CommCareSuiteController(CommCareSessionController controller, Menu m) {
		this.m = m;
		this.controller = controller;
		
		view = new CommCareListView(m.getName().evaluate());
		view.setCommandListener(this);
	}
	
	public void setTransitions (MenuTransitions transitions) {
		this.transitions = transitions;
	}

	public void start() {
		view.deleteAll();
		controller.populateMenu(view, m.getId(), view);
		J2MEDisplay.setView(view);
	}

	public void commandAction(Command c, Displayable d) {
		CrashHandler.commandAction(this, c, d);
	}  

	public void _commandAction(Command c, Displayable d) {
		if(c.equals(List.SELECT_COMMAND)) {
			controller.chooseSessionItem(view.getSelectedIndex());
			controller.next();
		}
		else if(c.equals(CommCareListView.BACK)) {
			transitions.exitMenuTransition();
		}
	}	
}
