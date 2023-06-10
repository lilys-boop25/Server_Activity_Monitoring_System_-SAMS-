package GUI;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Parent class combining code common to the other panels.
 */
public class OshiJPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    protected JLabel msgLabel = new JLabel();
    protected JPanel msgPanel = new JPanel();

    public OshiJPanel() {
        super();
        Dimension maxSize = getMaximumSize();
        if (maxSize != null) {
            setSize(maxSize);
        }
        /*
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        msgPanel.add(msgLabel);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(msgPanel, BorderLayout.CENTER);
        this.add(topPanel, BorderLayout.NORTH);
        */
        GridBagConstraints oshiConstraint = new GridBagConstraints();
        setLayout(new GridBagLayout());
    }

    public static void print(Object... content)
    {
        for (Object object: content)
        {
            System.out.println(object);
        }
    }
}
