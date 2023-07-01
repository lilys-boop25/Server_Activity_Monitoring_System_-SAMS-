package GUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class HomeTopPanel {

    private static int x = 200;

    public static void main(String[] args) {
	testPanel(new BorderLayout(), BorderLayout.CENTER);
	testPanel(new GridBagLayout(), createGridBagConstraints());
    }

    private static void testPanel(LayoutManager manager, Object constraints) {
	JPanel panel = new JPanel(manager);
	JButton button = new JButton("button");
	button.setBackground(Color.ORANGE);
	panel.setBackground(Color.WHITE);
	panel.add(button, constraints);

	JFrame window = new JFrame("Test Panels");
	window.add(panel);
	window.setLocation(x, 200);
	window.setSize(400, 300);
	window.setVisible(true);
	window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	x += 400;
    }

    private static GridBagConstraints createGridBagConstraints() {
	GridBagConstraints c = new GridBagConstraints();
	c.fill = GridBagConstraints.BOTH;
	return c;
    }
}
